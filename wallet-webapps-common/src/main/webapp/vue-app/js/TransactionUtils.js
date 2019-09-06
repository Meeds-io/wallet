import {searchWalletByAddress} from './AddressRegistry.js';
import {etherToFiat, watchTransactionStatus} from './WalletUtils.js';

export function loadTransactions(account, contractDetails, transactions, onlyPending, transactionsLimit, filterObject, isAdministration, refreshCallback) {
  if (!transactionsLimit) {
    transactionsLimit = 10;
  }

  return getStoredTransactions(account, contractDetails && contractDetails.isContract && contractDetails.address, transactionsLimit, filterObject, onlyPending, isAdministration).then((storedTransactions) => {
    const loadedTransactions = Object.assign({}, transactions);
    const loadingPromises = [];

    if (storedTransactions && storedTransactions.length) {
      storedTransactions.forEach((storedTransaction) => {
        if (loadedTransactions[storedTransaction.hash]) {
          loadingPromises.push(loadedTransactions[storedTransaction.hash]);
        } else {
          const loadingTransactionDetailsPromise = loadTransactionDetailsFromContractAndWatchPending(account, contractDetails, transactions, storedTransaction, refreshCallback);
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
      networkId: transaction.networkId ? transaction.networkId : window.walletSettings.network.id,
      hash: transaction.hash ? transaction.hash : '',
      contractAddress: transaction.contractAddress,
      contractMethodName: transaction.contractMethodName,
      pending: transaction.pending ? Boolean(transaction.pending) : false,
      gasPrice: transaction.gasPrice || 0,
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

export function getTransactionsAmounts(walletAddress, periodicity, selectedDate) {
  const lang = (window && window.eXo && window.eXo.env && window.eXo.env.portal && window.eXo.env.portal.language) || 'en';
  return fetch(`/portal/rest/wallet/api/transaction/getTransactionsAmounts?address=${walletAddress}&periodicity=${periodicity || ''}&date=${selectedDate || ''}&lang=${lang}`, {credentials: 'include'}).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else {
      return null;
    }
  });
}

export function getStoredTransactions(account, contractAddress, limit, filterObject, onlyPending, isAdministration) {
  const transactionHashToSearch = filterObject && filterObject.hash;
  const transactionContractMethodName = filterObject && filterObject.contractMethodName;

  return fetch(`/portal/rest/wallet/api/transaction/getTransactions?address=${account}&contractAddress=${contractAddress || ''}&contractMethodName=${transactionContractMethodName || ''}&limit=${limit}&hash=${transactionHashToSearch || ''}&pending=${onlyPending || false}&administration=${isAdministration || false}`, {credentials: 'include'})
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

export function getSavedTransactionByHash(hash) {
  return fetch(`/portal/rest/wallet/api/transaction/getSavedTransactionByHash?hash=${hash}`, {credentials: 'include'}).then((resp) => {
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
  });
}

function loadTransactionDetailsFromContractAndWatchPending(walletAddress, accountDetails, transactions, transactionDetails, watchLoadSuccess) {
  if (!transactionDetails || !walletAddress) {
    console.debug('Wrong method parameters', walletAddress, transactionDetails);
    return;
  }

  transactionDetails.type = transactionDetails.contractAddress ? 'contract' : 'ether';

  if (watchLoadSuccess && transactionDetails.pending) {
    watchTransactionStatus(transactionDetails.hash, watchLoadSuccess);
  }

  let resultPromise = null;
  if (transactionDetails.contractAddress) {
    const contractDetails = window.walletContractsDetails[transactionDetails.contractAddress];
    if (contractDetails) {
      addContractDetailsInTransation(transactionDetails, contractDetails);
      resultPromise = loadContractTransactionProperties(walletAddress, transactionDetails, contractDetails);
    } else {
      resultPromise = Promise.resolve();
    }
  } else {
    resultPromise = loadEtherTransactionProperties(walletAddress, transactionDetails);
  }
  return resultPromise.then(() => (transactions[transactionDetails.hash] = transactionDetails));
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

function loadEtherTransactionProperties(walletAddress, transactionDetails) {
  walletAddress = walletAddress.toLowerCase();
  if (!transactionDetails.fee && transactionDetails.gasUsed && transactionDetails.gasPrice) {
    transactionDetails.fee = transactionDetails.gasUsed && transactionDetails.gasPrice && window.localWeb3.utils.fromWei(String(transactionDetails.gasUsed * transactionDetails.gasPrice), 'ether');
  }
  if (!transactionDetails.feeFiat && transactionDetails.fee) {
    transactionDetails.feeFiat = etherToFiat(transactionDetails.fee);
  }

  const promises = [];
  promises.push(retrieveWalletDetails(transactionDetails, 'from'));
  promises.push(retrieveWalletDetails(transactionDetails, 'to'));

  return Promise.all(promises).then(() => {
    transactionDetails.isReceiver = walletAddress === transactionDetails.to;
    transactionDetails.amountFiat = transactionDetails.amountFiat || (transactionDetails.value && etherToFiat(transactionDetails.value));
    transactionDetails.date = transactionDetails.date || (transactionDetails.timestamp && new Date(transactionDetails.timestamp));
    if (transactionDetails.date) {
      transactionDetails.dateFormatted = `${transactionDetails.date.toLocaleDateString(eXo.env.portal.language, {year: 'numeric', month: 'long', day: 'numeric'})} - ${transactionDetails.date.toLocaleTimeString()}`;
    }
  });
}

function loadContractTransactionProperties(walletAddress, transactionDetails, contractDetails) {
  walletAddress = walletAddress.toLowerCase();

  if (!transactionDetails.fee && transactionDetails.gasUsed && transactionDetails.gasPrice) {
    transactionDetails.fee = window.localWeb3.utils.fromWei(String(transactionDetails.gasUsed * transactionDetails.gasPrice), 'ether');
  }
  if (!transactionDetails.feeFiat && transactionDetails.fee) {
    transactionDetails.feeFiat = etherToFiat(transactionDetails.fee);
  }

  if (!abiDecoder.getABIs() || !abiDecoder.getABIs().length) {
    abiDecoder.addABI(JSON.parse(window.walletSettings.contractAbi));
  }

  // TODO I18N for labels
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
  if (transactionDetails.from && transactionDetails.contractAddress.toLowerCase() !== transactionDetails.from) {
    promises.push(retrieveWalletDetails(transactionDetails, 'from'));
  }
  if (transactionDetails.to && transactionDetails.contractAddress.toLowerCase() !== transactionDetails.to) {
    promises.push(retrieveWalletDetails(transactionDetails, 'to'));
  }
  promises.push(retrieveWalletDetails(transactionDetails, 'by'));

  return Promise.all(promises).then(() => {
    transactionDetails.amountFiat = transactionDetails.amountFiat || (transactionDetails.value && etherToFiat(transactionDetails.value));
    transactionDetails.isReceiver = walletAddress === transactionDetails.to;
    transactionDetails.date = transactionDetails.date || (transactionDetails.timestamp && new Date(transactionDetails.timestamp));
    if (transactionDetails.date) {
      transactionDetails.dateFormatted = `${transactionDetails.date.toLocaleDateString(eXo.env.portal.language, {year: 'numeric', month: 'long', day: 'numeric'})} - ${transactionDetails.date.toLocaleTimeString()}`;
    }
    transactionDetails.adminIcon = transactionDetails.adminIcon || (transactionDetails.contractMethodName && transactionDetails.contractMethodName !== 'transfer' && transactionDetails.contractMethodName !== 'initializeAccount' && transactionDetails.contractMethodName !== 'transferFrom' && transactionDetails.contractMethodName !== 'approve' && transactionDetails.contractMethodName !== 'reward');
  });
}

function retrieveWalletDetails(transactionDetails, prefix) {
  if (!transactionDetails[`${prefix}DisplayName`]) {
    const address = transactionDetails[prefix];
    if (!address) {
      return;
    }
    transactionDetails[`${prefix}Address`] = address.toLowerCase();
    if (window.walletSettings.contractAddress && transactionDetails[`${prefix}Address`] === window.walletSettings.contractAddress.toLowerCase()) {
      transactionDetails[`${prefix}DisplayName`] = 'Admin';
      return;
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
