import {searchWalletByAddress} from './AddressRegistry.js';
import {etherToFiat, watchTransactionStatus, getTransactionReceipt, getTransaction, convertTokenAmountReceived} from './WalletUtils.js';
import {getSavedContractDetails, retrieveContractDetails} from './TokenUtils.js';

export function getLastNonce(networkId, walletAddress, useMetamask) {
  if (useMetamask) {
    return Promise.resolve(null);
  }

  return getLastPendingTransactionSent(networkId, walletAddress)
    .then((lastPendingTransaction) => {
      if (!lastPendingTransaction || !lastPendingTransaction.hash) {
        return;
      }
      return getTransaction(lastPendingTransaction.hash);
    })
    .then((pendingTransaction) => {
      // If not pending on blockchain, use auto-increment nonce
      if (!pendingTransaction || !pendingTransaction.nonce || pendingTransaction.blockNumber) {
        return;
      }
      return pendingTransaction.nonce;
    })
    .catch((e) => {
      console.debug('Error getting last nonce of wallet address', walletAddress, e);
    });
}

export function loadTransactions(networkId, account, contractDetails, transactions, onlyPending, transactionsLimit, transactionHashToSearch, isAdministration, refreshCallback) {
  if (!transactionsLimit) {
    transactionsLimit = 10;
  }

  return getStoredTransactions(networkId, account, contractDetails && contractDetails.isContract && contractDetails.address, transactionsLimit, transactionHashToSearch, onlyPending, isAdministration).then((storedTransactions) => {
    const loadedTransactions = Object.assign({}, transactions);
    const loadingPromises = [];

    if (storedTransactions && storedTransactions.length) {
      storedTransactions.forEach((storedTransaction) => {
        if (loadedTransactions[storedTransaction.hash]) {
          loadingPromises.push(loadedTransactions[storedTransaction.hash]);
        } else {
          const loadingTransactionDetailsPromise = loadTransactionDetailsFromContractAndWatchPending(networkId, account, contractDetails, transactions, storedTransaction, refreshCallback);
          loadingPromises.push(loadingTransactionDetailsPromise);
        }
      });
    }

    return Promise.all(loadingPromises).catch((error) => {
      if (`${error}`.indexOf('stopLoading') < 0) {
        throw error;
      }
    });
  });
}

export function saveTransactionDetails(transaction, contractDetails) {
  try {
    const transationDetails = {
      networkId: transaction.networkId ? transaction.networkId : window.walletSettings.defaultNetworkId,
      hash: transaction.hash ? transaction.hash : '',
      contractAddress: transaction.contractAddress,
      contractMethodName: transaction.contractMethodName,
      pending: transaction.pending ? Boolean(transaction.pending) : false,
      from: transaction.from ? transaction.from : '',
      to: transaction.to ? transaction.to : '',
      by: transaction.by ? transaction.by : '',
      label: transaction.label ? transaction.label : '',
      message: transaction.message ? transaction.message : '',
      value: transaction.value ? Number(transaction.value) : 0,
      contractAmount: transaction.contractAmount ? Number(transaction.contractAmount) : 0,
      adminOperation: transaction.adminOperation ? Boolean(transaction.adminOperation) : false,
      timestamp: transaction.timestamp ? Number(transaction.timestamp) : 0,
    };
    return fetch('/portal/rest/wallet/api/transaction/saveTransactionDetails', {
      method: 'POST',
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(transationDetails),
    });
  } catch (e) {
    console.debug('Error saving pending transaction', e);
    throw e;
  }
}

function loadTransactionReceipt(transactionDetails) {
  if (transactionDetails.transaction && transactionDetails.receipt) {
    return Promise.resolve();
  }
  if (transactionDetails.transaction) {
    // Get only receipt
    return getTransactionReceipt(transactionDetails.hash).then((receipt) => (transactionDetails.receipt = receipt));
  } else {
    // Get receipt and transaction
    return getTransaction(transactionDetails.hash)
      .then((transaction) => {
        transactionDetails.transaction = transaction;
        // If receipt exists, return without retrieving it from blockchain
        return transactionDetails.receipt || (transaction && getTransactionReceipt(transactionDetails.hash));
      })
      .then((receipt) => (transactionDetails.receipt = receipt));
  }
}

function loadTransactionContractDetails(networkId, account, transactionDetails, accountDetails) {
  // Is contract creation if contractAddress property is set in receipt
  transactionDetails.isContractCreation = transactionDetails.transaction && !transactionDetails.transaction.to && transactionDetails.receipt && transactionDetails.receipt.contractAddress;

  transactionDetails.contractAddress =
    transactionDetails.contractAddress || // Passed transaction has a contract address
    (accountDetails && accountDetails.isContract && accountDetails.address) || // Passed transaction is a contract transaction
    (transactionDetails.status && transactionDetails.receipt && transactionDetails.receipt.logs && transactionDetails.to) || // Passed transaction receipt has logs
    (transactionDetails.isContractCreation && transactionDetails.contractAddress); // Passed transaction is a contract creation transaction

  // If contract address found
  if (transactionDetails.contractAddress) {
    const cachedContractDetails = window.walletContractsDetails ? Object.values(window.walletContractsDetails).find((details) => details && details.address && transactionDetails.contractAddress.toLowerCase() === details.address.toLowerCase()) : null;
    return (
      cachedContractDetails ||
      getSavedContractDetails(transactionDetails.contractAddress, networkId).then((contractDetails) => {
        if (contractDetails) {
          if (!window.walletContractsDetails) {
            window.walletContractsDetails = {};
          }
          window.walletContractsDetails[contractDetails.address] = contractDetails;
          window.walletContractsDetails[contractDetails.address.toLowerCase()] = contractDetails;
          return contractDetails;
        } else {
          return window.localWeb3.eth.getCode(transactionDetails.contractAddress).then((contractCode) => {
            if (contractCode && contractCode !== '0x') {
              contractDetails = {
                address: transactionDetails.contractAddress,
                icon: 'fa-file-contract',
                isContract: true,
                networkId: networkId,
              };
              return retrieveContractDetails(account, contractDetails, false, true);
            }
          });
        }
      })
    );
  }
}

function addContractDetailsInTransation(transactionDetails, contractDetails) {
  if (!contractDetails) {
    return;
  }
  transactionDetails.type = 'contract';
  transactionDetails.contractName = contractDetails.name;
  transactionDetails.contractAddress = contractDetails.address;
  transactionDetails.contractAmountLabel = transactionDetails.contractAmountLabel || 'Amount';
  transactionDetails.contractSymbol = transactionDetails.contractSymbol || contractDetails.symbol;
  transactionDetails.contractDecimals = transactionDetails.contractDecimals || contractDetails.decimals || 0;
}

function loadTransactionFee(transactionDetails) {
  if (transactionDetails.transaction && transactionDetails.receipt) {
    return Promise.resolve();
  }

  return getTransactionReceipt(transactionDetails.hash)
    .then((transaction) => {
      transactionDetails.transaction = transaction;
      if (transaction) {
        return getTransactionReceipt(transactionDetails.hash);
      }
    })
    .then((receipt) => (transactionDetails.receipt = receipt));
}

function loadTransactionDetailsFromContractAndWatchPending(networkId, walletAddress, accountDetails, transactions, transactionDetails, watchLoadSuccess) {
  if (!transactionDetails || !networkId || !walletAddress) {
    console.debug('Wrong method parameters', networkId, walletAddress, transactionDetails);
    return;
  }

  if (transactionDetails.pending) {
    watchTransactionStatus(transactionDetails.hash, (receipt, block) => {
      transactionDetails.pending = false;
      transactionDetails.receipt = receipt;
      transactionDetails.timestamp = (block && block.timestamp) || transactionDetails.timestamp;
      loadTransactionDetailsFromContractAndWatchPending(networkId, walletAddress, accountDetails, transactions, transactionDetails).then(() => {
        watchLoadSuccess(transactionDetails);
      });
    });
  }

  return loadTransactionReceipt(transactionDetails)
    .then(() => {
      transactionDetails.status = transactionDetails.receipt && transactionDetails.receipt.status;
      return loadTransactionContractDetails(networkId, walletAddress, transactionDetails, accountDetails);
    })
    .then((contractDetails) => {
      if (contractDetails) {
        transactionDetails.type = 'contract';
        addContractDetailsInTransation(transactionDetails, contractDetails);
        return loadContractTransactionProperties(walletAddress, transactionDetails, contractDetails);
      } else {
        transactionDetails.type = 'ether';
        return loadEtherTransactionProperties(walletAddress, transactionDetails, contractDetails);
      }
    })
    .then(() => transactions && (transactions[transactionDetails.hash] = transactionDetails));
}

function loadEtherTransactionProperties(walletAddress, transactionDetails, contractDetails) {
  walletAddress = walletAddress.toLowerCase();
  if (!transactionDetails.fee) {
    transactionDetails.gasUsed = (transactionDetails.receipt && transactionDetails.receipt.gasUsed) || transactionDetails.gasUsed || transactionDetails.gas;
    transactionDetails.gasPrice = (transactionDetails.transaction && transactionDetails.transaction.gasPrice) || transactionDetails.gasPrice;
    transactionDetails.fee = transactionDetails.gasUsed && transactionDetails.gasPrice && window.localWeb3.utils.fromWei(String(transactionDetails.gasUsed * transactionDetails.gasPrice), 'ether');
  }
  if (!transactionDetails.feeFiat && transactionDetails.fee) {
    transactionDetails.feeFiat = etherToFiat(transactionDetails.fee);
  }
  transactionDetails.from = transactionDetails.fromAddress = transactionDetails.fromAddress || transactionDetails.from || (transactionDetails.transaction && transactionDetails.transaction.from);
  transactionDetails.to = transactionDetails.toAddress = transactionDetails.toAddress || transactionDetails.to;

  const promises = [];
  promises.push(retrieveWalletDetails(transactionDetails, 'from'));
  promises.push(retrieveWalletDetails(transactionDetails, 'to'));

  return Promise.all(promises).then(() => {
    if (transactionDetails.fromAddress === walletAddress || transactionDetails.toAddress === walletAddress) {
      transactionDetails.isReceiver = walletAddress === transactionDetails.toAddress;
      transactionDetails.value = transactionDetails.value || (transactionDetails.transaction && transactionDetails.transaction.value && parseFloat(window.localWeb3.utils.fromWei(String(transactionDetails.transaction.value), 'ether')));
      transactionDetails.amountFiat = transactionDetails.amountFiat || (transactionDetails.value && etherToFiat(transactionDetails.value));
      transactionDetails.date = transactionDetails.date || (transactionDetails.timestamp && new Date(transactionDetails.timestamp));
      if (transactionDetails.date) {
        transactionDetails.dateFormatted = `${transactionDetails.date.toLocaleDateString(eXo.env.portal.language, {year: 'numeric', month: 'long', day: 'numeric'})} - ${transactionDetails.date.toLocaleTimeString()}`;
      }
    } else {
      console.warn('It seems that the transaction is added into list by error, skipping.', walletAddress, transactionDetails);
    }
  });
}

function loadContractTransactionProperties(walletAddress, transactionDetails, contractDetails) {
  walletAddress = walletAddress.toLowerCase();

  if (!transactionDetails.fee) {
    transactionDetails.gasUsed = (transactionDetails.receipt && transactionDetails.receipt.gasUsed) || transactionDetails.gasUsed || transactionDetails.gas;
    transactionDetails.gasPrice = (transactionDetails.transaction && transactionDetails.transaction.gasPrice) || transactionDetails.gasPrice;
    transactionDetails.fee = transactionDetails.gasUsed && transactionDetails.gasPrice && window.localWeb3.utils.fromWei(String(transactionDetails.gasUsed * transactionDetails.gasPrice), 'ether');
  }
  if (!transactionDetails.feeFiat && transactionDetails.fee) {
    transactionDetails.feeFiat = etherToFiat(transactionDetails.fee);
  }
  transactionDetails.from = transactionDetails.fromAddress = transactionDetails.fromAddress || transactionDetails.from || (transactionDetails.transaction && transactionDetails.transaction.from);
  transactionDetails.to = transactionDetails.toAddress = transactionDetails.toAddress || transactionDetails.to;
  transactionDetails.by = transactionDetails.byAddress = transactionDetails.byAddress || transactionDetails.by;

  if (!abiDecoder.getABIs() || !abiDecoder.getABIs().length) {
    abiDecoder.addABI(window.walletSettings.contractAbi);
  }

  if (transactionDetails.transaction && !transactionDetails.contractMethodName) {
    const method = abiDecoder.decodeMethod(transactionDetails.transaction.input);
    transactionDetails.contractMethodName = method && method.name;
  }

  if (transactionDetails.contractMethodName && transactionDetails && transactionDetails.receipt && transactionDetails.receipt.logs && transactionDetails.receipt.logs.length) {
    computeTransactionDetailsTokenOperation(transactionDetails, transactionDetails.receipt.logs);
  }

  if (transactionDetails.contractMethodName === 'addAdmin') {
    transactionDetails.contractSymbol = '';
    transactionDetails.contractAmountLabel = 'Admin level';
  } else if (transactionDetails.contractMethodName === 'setSellPrice') {
    transactionDetails.contractSymbol = 'eth';
    transactionDetails.contractAmountLabel = 'New sell price';
  } else if (transactionDetails.contractMethodName === 'upgradeData' || transactionDetails.contractMethodName === 'upgradeImplementation') {
    transactionDetails.contractSymbol = '';
    transactionDetails.contractAmountLabel = 'Version';
  } else if (transactionDetails.contractMethodName === 'transformToVested') {
    transactionDetails.contractAmountLabel = 'Initialization amount';
  } else if (transactionDetails.contractMethodName === 'reward') {
    transactionDetails.contractAmountLabel = 'Rewarded amount';
  }

  const promises = [];
  if (transactionDetails.contractAddress.toLowerCase() !== transactionDetails.fromAddress) {
    promises.push(retrieveWalletDetails(transactionDetails, 'from'));
  }
  if (transactionDetails.contractAddress.toLowerCase() !== transactionDetails.toAddress) {
    promises.push(retrieveWalletDetails(transactionDetails, 'to'));
  }
  promises.push(retrieveWalletDetails(transactionDetails, 'by'));

  return Promise.all(promises).then(() => {
    if ((transactionDetails.contractAddress && transactionDetails.contractAddress.toLowerCase()) === walletAddress || transactionDetails.fromAddress === walletAddress || transactionDetails.toAddress === walletAddress || transactionDetails.byAddress === walletAddress) {
      transactionDetails.value = transactionDetails.value || (transactionDetails.transaction && transactionDetails.transaction.value && parseFloat(window.localWeb3.utils.fromWei(String(transactionDetails.transaction.value), 'ether')));
      transactionDetails.amountFiat = transactionDetails.amountFiat || (transactionDetails.value && etherToFiat(transactionDetails.value));
      transactionDetails.adminIcon = transactionDetails.adminIcon || transactionDetails.value || (transactionDetails.contractMethodName && transactionDetails.contractMethodName !== 'transfer' && transactionDetails.contractMethodName !== 'initializeAccount' && transactionDetails.contractMethodName !== 'transferFrom' && transactionDetails.contractMethodName !== 'approve');
      transactionDetails.isReceiver = !transactionDetails.adminIcon && walletAddress === transactionDetails.toAddress;
      transactionDetails.date = transactionDetails.date || (transactionDetails.timestamp && new Date(transactionDetails.timestamp));
      if (transactionDetails.date) {
        transactionDetails.dateFormatted = `${transactionDetails.date.toLocaleDateString(eXo.env.portal.language, {year: 'numeric', month: 'long', day: 'numeric'})} - ${transactionDetails.date.toLocaleTimeString()}`;
      }
    } else {
      console.warn('It seems that the transaction is added into list by error, skipping.', walletAddress, transactionDetails);
    }
  });
}

function computeTransactionDetailsTokenOperation(transactionDetails, logs) {
  if (transactionDetails.decodedLogs) {
    return;
  }
  const decodedLogs = abiDecoder.decodeLogs(logs);
  if (transactionDetails.contractMethodName) {
    if (transactionDetails.contractMethodName === 'transfer' || transactionDetails.contractMethodName === 'approve') {
      const methodLog = decodedLogs && decodedLogs.find((decodedLog) => decodedLog && (decodedLog.name === 'Transfer' || decodedLog.name === 'Approval'));
      if (methodLog) {
        transactionDetails.fromAddress = methodLog.events[0].value.toLowerCase();
        transactionDetails.toAddress = methodLog.events[1].value.toLowerCase();
        transactionDetails.contractAmount = convertTokenAmountReceived(methodLog.events[2].value, transactionDetails.contractDecimals);
      }
    } else if (transactionDetails.contractMethodName === 'transferFrom') {
      const methodLog = decodedLogs && decodedLogs.find((decodedLog) => decodedLog && decodedLog.name === 'Transfer');
      if (methodLog) {
        transactionDetails.fromAddress = methodLog.events[0].value.toLowerCase();
        transactionDetails.toAddress = methodLog.events[1].value.toLowerCase();
        transactionDetails.byAddress = transactionDetails.transaction.from.toLowerCase();
        transactionDetails.contractAmount = convertTokenAmountReceived(methodLog.events[2].value, transactionDetails.contractDecimals);
      }
    } else if (transactionDetails.contractMethodName === 'approveAccount') {
      const methodLog = decodedLogs && decodedLogs.find((decodedLog) => decodedLog && decodedLog.name === 'ApprovedAccount');
      if (methodLog) {
        transactionDetails.toAddress = methodLog.events[0].value.toLowerCase();
      } else {
        transactionDetails.toDisplayName = 'a previously approved';
      }
    } else if (transactionDetails.contractMethodName === 'disapproveAccount') {
      const methodLog = decodedLogs && decodedLogs.find((decodedLog) => decodedLog && decodedLog.name === 'DisapprovedAccount');
      if (methodLog) {
        transactionDetails.toAddress = methodLog.events[0].value.toLowerCase();
      } else {
        transactionDetails.toDisplayName = 'a previously disapproved';
      }
    } else if (transactionDetails.contractMethodName === 'addAdmin') {
      const methodLog = decodedLogs && decodedLogs.find((decodedLog) => decodedLog && decodedLog.name === 'AddedAdmin');
      if (methodLog) {
        transactionDetails.toAddress = methodLog.events[0].value.toLowerCase();
        transactionDetails.contractAmount = methodLog.events[1].value;
      }
    } else if (transactionDetails.contractMethodName === 'transferOwnership') {
      const methodLog = decodedLogs && decodedLogs.find((decodedLog) => decodedLog && decodedLog.name === 'TransferOwnership');
      if (methodLog) {
        transactionDetails.toAddress = methodLog.events[0].value.toLowerCase();
      }
    } else if (transactionDetails.contractMethodName === 'removeAdmin') {
      const methodLog = decodedLogs && decodedLogs.find((decodedLog) => decodedLog && decodedLog.name === 'RemovedAdmin');
      if (methodLog) {
        transactionDetails.toAddress = methodLog.events[0].value.toLowerCase();
      } else {
        transactionDetails.toDisplayName = 'a not admin account';
      }
    } else if (transactionDetails.contractMethodName === 'setSellPrice') {
      const methodLog = decodedLogs && decodedLogs.find((decodedLog) => decodedLog && decodedLog.name === 'TokenPriceChanged');
      if (methodLog) {
        transactionDetails.contractAmount = methodLog.events[0].value.toLowerCase();
        if (transactionDetails.contractAmount) {
          transactionDetails.contractAmount = window.localWeb3.utils.fromWei(String(transactionDetails.contractAmount), 'ether');
        }
      }
    }
    const transactionFeeLog = decodedLogs && decodedLogs.find((decodedLog) => decodedLog && decodedLog.name === 'TransactionFee');
    if (transactionFeeLog) {
      transactionDetails.feeToken = convertTokenAmountReceived(transactionFeeLog.events[1].value, transactionDetails.contractDecimals);
    } else {
      const noSufficientFundsLog = decodedLogs && decodedLogs.find((decodedLog) => decodedLog && decodedLog.name === 'NoSufficientFund');
      if (noSufficientFundsLog) {
        transactionDetails.feeNoSufficientFunds = true;
      }
    }
  }
  transactionDetails.decodedLogs = true;
}

function retrieveWalletDetails(transactionDetails, prefix) {
  if (!transactionDetails[`${prefix}DisplayName`]) {
    transactionDetails[`${prefix}Address`] = transactionDetails[`${prefix}Address`] || transactionDetails[prefix] || (transactionDetails.transaction && transactionDetails.transaction[prefix]);
    if (!transactionDetails[`${prefix}Address`]) {
      return;
    }

    transactionDetails[`${prefix}Address`] = transactionDetails[`${prefix}Address`].toLowerCase();
    if (window.walletSettings.principalContractAdminAddress && transactionDetails[`${prefix}Address`] === window.walletSettings.principalContractAdminAddress.toLowerCase()) {
      transactionDetails[`${prefix}DisplayName`] = window.walletSettings.principalContractAdminName;
    }

    if (transactionDetails[`${prefix}Wallet`]) {
      transactionDetails[`${prefix}TechnicalId`] = transactionDetails[`${prefix}Wallet`].technicalId;
      transactionDetails[`${prefix}SpaceId`] = transactionDetails[`${prefix}Wallet`].spaceId;
      transactionDetails[`${prefix}Username`] = transactionDetails[`${prefix}Wallet`].id;
      transactionDetails[`${prefix}Type`] = transactionDetails[`${prefix}Wallet`].type;
      transactionDetails[`${prefix}Avatar`] = transactionDetails[`${prefix}Wallet`].avatar;
      transactionDetails[`${prefix}DisplayName`] = transactionDetails[`${prefix}Wallet`].name;
    } else if (transactionDetails[`${prefix}Address`]) {
      return searchWalletByAddress(transactionDetails[`${prefix}Address`]).then((item) => {
        if (item && item.name && item.name.length) {
          transactionDetails[`${prefix}TechnicalId`] = item.technicalId;
          transactionDetails[`${prefix}SpaceId`] = item.spaceId;
          transactionDetails[`${prefix}Username`] = item.id;
          transactionDetails[`${prefix}Type`] = item.type;
          transactionDetails[`${prefix}Avatar`] = item.avatar;
          transactionDetails[`${prefix}DisplayName`] = item.name;
        }
      });
    }
  }
}

function getStoredTransactions(networkId, account, contractAddress, limit, transactionHashToSearch, onlyPending, isAdministration) {
  return fetch(`/portal/rest/wallet/api/transaction/getTransactions?networkId=${networkId}&address=${account}&contractAddress=${contractAddress || ''}&limit=${limit}&hash=${transactionHashToSearch || ''}&pending=${onlyPending || false}&administration=${isAdministration || false}`, {credentials: 'include'})
    .then((resp) => {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then((transactions) => {
      return transactions && transactions.length ? transactions : [];
    })
    .catch((error) => {
      throw new Error('Error retrieving transactions list', error);
    });
}

function getLastPendingTransactionSent(networkId, address) {
  return fetch(`/portal/rest/wallet/api/transaction/getLastPendingTransactionSent?networkId=${networkId}&address=${address}`, {credentials: 'include'})
    .then((resp) => {
      if (resp && resp.ok) {
        const contentType = resp.headers && resp.headers.get('content-type');
        if (contentType && contentType.indexOf('application/json') !== -1) {
          return resp.json();
        } else {
          return null;
        }
      } else {
        return null;
      }
    })
    .catch((error) => {
      throw new Error('Error retrieving last pending transaction', error);
    });
}
