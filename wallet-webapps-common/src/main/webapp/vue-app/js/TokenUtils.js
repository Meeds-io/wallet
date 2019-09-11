import {etherToFiat} from './WalletUtils.js';
import {getSavedTransactionByNonceOrHash} from './TransactionUtils.js';

const NONCE_TOO_LOW_ERROR = 'nonce too low';

export function reloadContractDetails(contractDetails, walletAddress) {
  if (!contractDetails || !contractDetails.address) {
    return;
  }

  return getSavedContractDetails(contractDetails.address).then((savedDetails) => {
    Object.assign(contractDetails, savedDetails);
    return getContractDetails(walletAddress);
  });
}

export function getContractDetails(walletAddress) {
  const contractDetails = window.walletSettings.contractDetail;
  if (contractDetails && !contractDetails.icon) {
    contractDetails.icon = 'fa-file-contract';
    contractDetails.title = contractDetails.name;
    contractDetails.isContract = true;
    contractDetails.isOwner = contractDetails.owner === walletAddress && walletAddress.toLowerCase();
    contractDetails.fiatBalance = contractDetails.fiatBalance || (contractDetails.etherBalance && etherToFiat(contractDetails.etherBalance));
    if (!contractDetails.contract) {
      try {
        contractDetails.contract = getContractInstance(walletAddress, contractDetails.address);
      } catch (e) {
        transformContracDetailsToFailed(contractDetails);
        console.debug('getContractDetails method - error retrieving contract instance', contractDetails.address, new Error(e));
      }
    }

    // Cache contract details by contract address
    if (!window.walletContractsDetails) {
      window.walletContractsDetails = {};
    }

    window.walletContractsDetails[contractDetails.address] = contractDetails;
    window.walletContractsDetails[contractDetails.address.toLowerCase()] = contractDetails;
  }
  return Promise.resolve(contractDetails);
}

/*
 * Creates Web3 conract deployment transaction
 */
export function newContractInstance(abi, bin, ...args) {
  if (!abi || !bin) {
    return null;
  }
  if (args && args.length) {
    return new window.localWeb3.eth.Contract(abi).deploy({
      data: bin,
      arguments: args,
    });
  } else {
    return new window.localWeb3.eth.Contract(abi).deploy({
      data: bin,
    });
  }
}

/*
 * Creates Web3 conract deployment transaction
 */
export function deployContract(contractInstance, account, gasLimit, gasPrice, transactionHashCallback) {
  let transactionHash;
  return contractInstance
    .send({
      from: account,
      gas: gasLimit,
      gasPrice: gasPrice,
    })
    .on('transactionHash', (hash) => {
      return transactionHashCallback && transactionHashCallback((transactionHash = hash));
    })
    .on('receipt', (receipt) => {
      return transactionHashCallback && transactionHashCallback(transactionHash);
    });
}

/*
 * Retrieve contract details from eXo Platform server if exists
 */
export function getSavedContractDetails(address) {
  return fetch(`/portal/rest/wallet/api/contract?address=${address}`, {
    method: 'GET',
    credentials: 'include',
  })
    .then((resp) => {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        throw new Error(`Error getting contract details from server with address ${address}`);
      }
    })
    .then((contractDetails) => {
      if (contractDetails) {
        contractDetails.fiatBalance = contractDetails.fiatBalance || (contractDetails.etherBalance && etherToFiat(contractDetails.etherBalance));
      }
      return contractDetails && contractDetails.address ? contractDetails : null;
    })
    .catch((e) => {
      console.debug('Error getting contract details from server', e);
      return null;
    });
}

/*
 * Creates a Web3 conract instance
 */
export function createNewContractInstanceByName(tokenName, ...args) {
  let contractFiles;
  return getContractFiles(tokenName)
    .then((filesContents) => (contractFiles = filesContents))
    .then(() => newContractInstance(contractFiles.abi, contractFiles.bin, ...args))
    .then((contractInstance) => {
      contractInstance.abi = contractFiles.abi;
      contractInstance.bin = contractFiles.bin;
      return contractInstance;
    });
}

/*
 * Creates a Web3 conract instance
 */
export function createNewContractInstanceByNameAndAddress(tokenName, tokenAddress) {
  let contractFiles;
  return getContractFiles(tokenName)
    .then((filesContents) => (contractFiles = filesContents))
    .then(() => getContractInstance(window.localWeb3.eth.defaultAccount, tokenAddress, false, contractFiles.abi, contractFiles.bin))
    .then((contractInstance) => {
      contractInstance.abi = contractFiles.abi;
      contractInstance.bin = contractFiles.bin;
      return contractInstance;
    });
}

export function estimateContractDeploymentGas(instance) {
  return instance.estimateGas((error, estimatedGas) => {
    if (error) {
      throw new Error(`Error while estimating contract deployment gas ${error}`);
    }
    return estimatedGas;
  });
}

export function getContractDeploymentTransactionsInProgress() {
  const STORAGE_KEY = `exo-wallet-contract-deployment-progress-${window.walletSettings.network.id}`;
  const storageValue = localStorage.getItem(STORAGE_KEY);
  if (storageValue === null) {
    return {};
  } else {
    return JSON.parse(storageValue);
  }
}

export function removeContractDeploymentTransactionsInProgress(transactionHash) {
  const STORAGE_KEY = `exo-wallet-contract-deployment-progress-${window.walletSettings.network.id}`;
  let storageValue = localStorage.getItem(STORAGE_KEY);
  if (storageValue === null) {
    return;
  } else {
    storageValue = JSON.parse(storageValue);
  }

  if (storageValue[transactionHash]) {
    delete storageValue[transactionHash];
    localStorage.setItem(STORAGE_KEY, JSON.stringify(storageValue));
  }
}

export function getContractInstance(account, address, usePromise, abi, bin) {
  try {
    const contractInstance = new window.localWeb3.eth.Contract(abi ? abi : JSON.parse(window.walletSettings.contractAbi), address, {
      from: account && account.toLowerCase(),
      gas: window.walletSettings.network.gasLimit,
      gasPrice: window.walletSettings.network.normalGasPrice,
      data: bin ? bin : window.walletSettings.contractBin,
    });
    if (usePromise) {
      return Promise.resolve(contractInstance);
    } else {
      return contractInstance;
    }
  } catch (e) {
    console.debug('An error occurred while retrieving contract instance', new Error(e));
    if (usePromise) {
      return Promise.reject(e);
    } else {
      return null;
    }
  }
}

export function sendContractTransaction(txDetails, hashCallback, errorCallback) {
  // suppose you want to call a function named myFunction of myContract
  const transactionToSend = {
    to: txDetails.contractAddress,
    from: txDetails.senderAddress,
    data: txDetails.method(...txDetails.parameters).encodeABI(),
    gas: txDetails.gas,
    gasPrice: txDetails.gasPrice,
  };

  return getNewTransactionNonce(txDetails.senderAddress).then((nonce) => {
    // Increment manually nonce if we have the last transaction always pending
    transactionToSend.nonce = nonce;

    return sendTransaction(transactionToSend, hashCallback, errorCallback);
  });
}

function sendTransaction(transactionToSend, hashCallback, errorCallback) {
  return window.localWeb3.eth
    .sendTransaction(transactionToSend, (error, hash) => {
      if (!error && hash) {
        if (hashCallback) {
          // Verify if the generated hash is a duplication of an existing one
          // And then delete nonce to reattempt sending transaction
          return getSavedTransactionByNonceOrHash(transactionToSend.from, transactionToSend.nonce, hash)
            .then((transaction) => {
              // A transaction with same hash has been already sent
              if (transaction) {
                console.debug('Transaction was found on server eXo ', hash, '. Reattempting by incrementing nonce');
                manageTransactionError(new Error(NONCE_TOO_LOW_ERROR), hash, transactionToSend, hashCallback, errorCallback);
              } else {
                hashCallback(hash, (transactionToSend.nonce && Number(transactionToSend.nonce)) || 0);
              }
            });
        }
      }
    })
    .catch(error => {
      if (error) {
        manageTransactionError(error, null, transactionToSend, hashCallback, errorCallback)
      }
    });
}

function manageTransactionError(error, hash, transactionToSend, hashCallback, errorCallback) {
  // Workaround to stop polling from blockchain waiting for receipt
  if (error && String(error).indexOf('Failed to check for transaction receipt') >= 0) {
    return;
  // Transaction nonce is not correct
  } else if(String(error).indexOf(NONCE_TOO_LOW_ERROR) >= 0 || String(error).indexOf('transaction underpriced') >= 0 || String(error).indexOf('known transaction') >= 0) {
    console.debug(`Transaction wasn't sent because its nonce `, Number(transactionToSend.nonce) ,` isn't correct, increment it and retry`);
    transactionToSend.nonce = Number(transactionToSend.nonce) + 1;
    return sendTransaction(transactionToSend, hashCallback, errorCallback);
  } else {
    console.error('Error fetching transaction with hash', hash, error);
    if (errorCallback) {
      errorCallback(error);
    } else {
      throw error;
    }
  }
}

function getNewTransactionNonce(walletAddress) {
  return window.localWeb3.eth.getTransactionCount(walletAddress, 'pending').catch((e) => {
    console.debug('Error getting last nonce of wallet address', walletAddress, e);
  });
}

function transformContracDetailsToFailed(contractDetails, e) {
  contractDetails.icon = 'warning';
  contractDetails.title = contractDetails.address;
  contractDetails.error = `Error retrieving contract at specified address ${e ? e : ''}`;
  return contractDetails;
}

function getContractFiles(tokenName) {
  let contractBin;
  return fetch(`/portal/rest/wallet/api/contract/bin/${tokenName}`, {
    method: 'GET',
    credentials: 'include',
  })
    .then((resp) => {
      if (resp && resp.ok) {
        return resp.text();
      } else {
        throw new Error(`Cannot find contract BIN with name ${tokenName}`);
      }
    })
    .then((bin) => (contractBin = bin.indexOf('0x') === 0 ? bin : `0x${bin}`))
    .then(() =>
      fetch(`/portal/rest/wallet/api/contract/abi/${tokenName}`, {
        method: 'GET',
        credentials: 'include',
      })
    )
    .then((resp) => {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        throw new Error(`Cannot find contract ABI with name ${tokenName}`);
      }
    })
    .then((abi) => {
      return {
        abi: abi,
        bin: contractBin,
      };
    });
}
