import {etherToFiat} from './WalletUtils.js';
import {getNewTransactionNonce, getSavedTransactionByHash} from './TransactionUtils.js';

export function reloadContractDetails(contractDetails, walletAddress) {
  if (!contractDetails || !contractDetails.address) {
    return;
  }

  return getSavedContractDetails(contractDetails.address)
    .then(savedDetails => {
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
     .then((filesContents) => contractFiles = filesContents)
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
     .then((filesContents) => contractFiles = filesContents)
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

function waitTransactionOnBlockchain(hash, hashCallback, errorCallback, attemptTimes) {
  if (!attemptTimes) {
    if (errorCallback) {
      return errorCallback('Transaction hash not found');
    }
    return;
  }
  return window.localWeb3.eth.getTransaction(hash)
    .then(transaction => {
      if (transaction) {
        console.debug('Found transaction on blockchain', transaction);
        if (hashCallback) {
          return hashCallback(hash);
        }
        return;
      } else {
        console.debug('Transaction not found on blockchain', hash);
        waitTransactionOnBlockchain(hash, hashCallback, errorCallback, attemptTimes--);
      }
    })
}

function transformContracDetailsToFailed(contractDetails, e) {
  contractDetails.icon = 'warning';
  contractDetails.title = contractDetails.address;
  contractDetails.error = `Error retrieving contract at specified address ${e ? e : ''}`;
  return contractDetails;
}

function sendTransaction(transactionToSend, hashCallback, errorCallback) {
  return window.localWeb3.eth.sendTransaction(transactionToSend)
    .on('transactionHash', (hash) => {
      if (hashCallback) {
        // Verify if the generated hash is a bug from Web3js
        // And then delete nonce to reattempt sending transaction
        return getSavedTransactionByHash(hash)
          .then(transaction => {
            // A transaction with same hash has been already sent
            if (transaction) {
              console.debug('Transaction was found on server eXo', transaction, ' verifying blockchain transactions status, else incrementing gas price ', transactionToSend.nonce);

              return window.localWeb3.eth.getTransaction(hash)
                .then(transaction => {
                  if (transaction) {
                    console.debug('Found transaction on blockchain, incrementing nonce', transaction);
                    transactionToSend.nonce++;
                  } else {
                    console.debug('Transaction not found on blockchain, incrementing gas price with 1% to replace old buggy transaction', hash);
                    delete transactionToSend.nonce;
                    transactionToSend.gasPrice = transactionToSend.gasPrice * 1.01;
                  }
                  return sendTransaction(transactionToSend, hashCallback, errorCallback);
                });
            }
            // Attempt 5 times to retrieve sent transaction
            return waitTransactionOnBlockchain(hash, hashCallback, errorCallback, 5);
          })
          .then(() => {
            // Workaround to stop polling from blockchain waiting for receipt
            const currentProvider = window.localWeb3.currentProvider;
            if (currentProvider) {
              window.localWeb3.currentProvider = null;
              window.setTimeout(() => {
                window.localWeb3.currentProvider = currentProvider;
              }, 3000);
            }
          });
      }
    })
    .on('error', (error, receipt) => {
      // Workaround to stop polling from blockchain waiting for receipt
      if (String(error).indexOf("Failed to check for transaction receipt")) {
        return;
      } else {
        return errorCallback(error, receipt);
      }
    })
    .catch((error) => {
      // Workaround to stop polling from blockchain waiting for receipt
      if (String(error).indexOf("Failed to check for transaction receipt")) {
        return;
      } else {
        console.error('Error fetching transaction with hash', transactionToSend && transactionToSend.hash, error);
        errorCallback(error);
      }
    });

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
        bin: contractBin
      }
    });
}
