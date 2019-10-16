import {etherToFiat} from './WalletUtils.js';
import {getSavedContractDetails} from './TokenUtils.js';

export function loadTransactions(account, contractDetails, transactions, onlyPending, transactionsLimit, filterObject, isAdministration) {
  if (!transactionsLimit) {
    transactionsLimit = 10;
  }

  return getStoredTransactions(account, contractDetails && contractDetails.isContract && contractDetails.address, transactionsLimit, filterObject, onlyPending, isAdministration)
    .then((storedTransactions) => {
      if (storedTransactions && storedTransactions.length) {
        storedTransactions.forEach((storedTransaction) => {
          loadTransactionDetailsAndWatchPending(account, contractDetails, transactions, storedTransaction);
        });
      }
  
      return storedTransactions;
    });
}

export function refreshTransactionDetail(hash) {
  return fetch(`/portal/rest/wallet/api/transaction/refreshTransactionFromBlockchain?hash=${hash}`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  }).then(resp => {
    if(resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error while sending transaction');
    }
  });
}

export function saveTransactionDetails(transationDetails, contractDetails) {
  return fetch('/portal/rest/wallet/api/transaction/saveTransactionDetails', {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(transationDetails),
  }).then(resp => {
    if(resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error while sending transaction');
    }
  });
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

  return fetch(`/portal/rest/wallet/api/transaction/getTransactions?address=${account || ''}&contractAddress=${contractAddress || ''}&contractMethodName=${transactionContractMethodName || ''}&limit=${limit}&hash=${transactionHashToSearch || ''}&pending=${onlyPending || false}&administration=${isAdministration || false}`, {credentials: 'include'})
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

export function getNonce(from) {
  return fetch(`/portal/rest/wallet/api/transaction/getNonce?from=${from}`, {credentials: 'include'}).then((resp) => {
    if (resp && resp.ok) {
      return resp.text();
    } else {
      return null;
    }
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

function loadTransactionDetailsAndWatchPending(walletAddress, accountDetails, transactions, transactionDetails) {
  if (!transactionDetails) {
    return;
  }

  transactionDetails.type = transactionDetails.contractAddress ? 'contract' : 'ether';

  if (transactionDetails.contractAddress) {
    const contractDetails = window.walletContractsDetails[transactionDetails.contractAddress];
    if (contractDetails) {
      loadContractTransactionProperties(walletAddress, transactionDetails, contractDetails);
    } else {
      getSavedContractDetails(transactionDetails.contractAddress).then(contractDetails => {
        loadContractTransactionProperties(walletAddress, transactionDetails, contractDetails);
      });
    }
  } else {
    loadEtherTransactionProperties(walletAddress, transactionDetails);
  }
  return transactions[transactionDetails.hash] = transactionDetails;
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
  walletAddress = walletAddress && walletAddress.toLowerCase();
  if (!transactionDetails.fee && transactionDetails.gasUsed && transactionDetails.gasPrice) {
    transactionDetails.fee = transactionDetails.gasUsed && transactionDetails.gasPrice && window.localWeb3.utils.fromWei(String(transactionDetails.gasUsed * transactionDetails.gasPrice), 'ether');
  }
  if (!transactionDetails.feeFiat && transactionDetails.fee) {
    transactionDetails.feeFiat = etherToFiat(transactionDetails.fee);
  }

  retrieveWalletDetails(transactionDetails, 'from');
  retrieveWalletDetails(transactionDetails, 'to');

  transactionDetails.isReceiver = walletAddress === transactionDetails.to;
  transactionDetails.amountFiat = transactionDetails.amountFiat || (transactionDetails.value && etherToFiat(transactionDetails.value));
  if (transactionDetails.timestamp) {
    transactionDetails.dateFormatted = `${new Date(transactionDetails.timestamp).toLocaleString(eXo.env.portal.language, {year: 'numeric', month: 'long', day: 'numeric', hour: 'numeric', minute: 'numeric'})}`;
  }
  if (transactionDetails.sentTimestamp) {
    transactionDetails.sentDateFormatted = `${new Date(transactionDetails.sentTimestamp).toLocaleString(eXo.env.portal.language, {year: 'numeric', month: 'long', day: 'numeric', hour: 'numeric', minute: 'numeric'})}`;
  }
}

function loadContractTransactionProperties(walletAddress, transactionDetails, contractDetails) {
  if (contractDetails) {
    addContractDetailsInTransation(transactionDetails, contractDetails);
  }

  walletAddress = walletAddress && walletAddress.toLowerCase();

  if (!transactionDetails.fee && transactionDetails.gasUsed && transactionDetails.gasPrice) {
    transactionDetails.fee = window.localWeb3.utils.fromWei(String(transactionDetails.gasUsed * transactionDetails.gasPrice), 'ether');
  }
  if (!transactionDetails.feeFiat && transactionDetails.fee) {
    transactionDetails.feeFiat = etherToFiat(transactionDetails.fee);
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

  retrieveWalletDetails(transactionDetails, 'from');
  retrieveWalletDetails(transactionDetails, 'to');
  retrieveWalletDetails(transactionDetails, 'by');

  transactionDetails.amountFiat = transactionDetails.amountFiat || (transactionDetails.value && etherToFiat(transactionDetails.value));
  transactionDetails.isReceiver = walletAddress === transactionDetails.to;
  if (transactionDetails.timestamp) {
    transactionDetails.dateFormatted = `${new Date(transactionDetails.timestamp).toLocaleString(eXo.env.portal.language, {year: 'numeric', month: 'long', day: 'numeric', hour: 'numeric', minute: 'numeric'})}`;
  }
  if (transactionDetails.sentTimestamp) {
    transactionDetails.sentDateFormatted = `${new Date(transactionDetails.sentTimestamp).toLocaleString(eXo.env.portal.language, {year: 'numeric', month: 'long', day: 'numeric', hour: 'numeric', minute: 'numeric'})}`;
  }
  transactionDetails.adminIcon = transactionDetails.adminIcon || (transactionDetails.contractMethodName && transactionDetails.contractMethodName !== 'transfer' && transactionDetails.contractMethodName !== 'initializeAccount' && transactionDetails.contractMethodName !== 'transferFrom' && transactionDetails.contractMethodName !== 'approve' && transactionDetails.contractMethodName !== 'reward');
}

function retrieveWalletDetails(transactionDetails, prefix) {
  if (!transactionDetails[`${prefix}DisplayName`]) {
    const address = transactionDetails[prefix] && transactionDetails[prefix].toLowerCase();
    if (!address || (transactionDetails.contractAddress && address === transactionDetails.contractAddress.toLowerCase())) {
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
    }
  }
}
