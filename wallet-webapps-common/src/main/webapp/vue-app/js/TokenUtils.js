import {convertTokenAmountReceived, computeBalance} from './WalletUtils.js';
import {getLastNonce} from './TransactionUtils.js';

export function getContractDetails(account, isAdministration) {
  if (window.walletSettings.contractAddress) {
    const contractDetails = window.walletSettings.contractDetail || {};
    contractDetails.address = window.walletSettings.contractAddress;
    contractDetails.icon = 'fa-file-contract';
    contractDetails.networkId = window.walletSettings.network.id;
    contractDetails.isContract = true;
    return retrieveContractDetails(account, contractDetails, isAdministration);
  }
  return Promise.resolve();
}

/*
 * Retrieve an ERC20 contract instance at specified address
 */
export function retrieveContractDetails(account, contractDetails, isAdministration) {
  contractDetails.retrievedAttributes = 0;
  let contractToSave = false;
  try {
    const contractInstance = getContractInstance(account, contractDetails.address);
    contractDetails.contract = contractInstance;
  } catch (e) {
    transformContracDetailsToFailed(contractDetails);
    console.debug('retrieveContractDetails method - error retrieving instance', contractDetails.address, new Error(e));
    return Promise.resolve(contractDetails);
  }

  if (!contractDetails.hasOwnProperty('contractType')) {
    contractToSave = true;
  }

  // FIXME: Workaround for a unidentified bug
  if (contractDetails.symbol && contractDetails.symbol.trim() === '?') {
    contractDetails.symbol = null;
  }

  // Convert to numbers
  if (contractDetails.contractType) {
    contractDetails.contractType = Number(contractDetails.contractType);
  }
  if (contractDetails.sellPrice) {
    contractDetails.sellPrice = Number(contractDetails.sellPrice);
  }
  return (contractDetails.contractType > 1 ? contractDetails.contract.methods.version().call() : Promise.resolve(null))
    .then((version) => {
      if (version !== true && version) {
        version = Number(version);
        const originalContractType = contractDetails.contractType;
        contractDetails.contractType = version && !Number.isNaN(Number(version)) && Number.isInteger(version) ? Number(version) : 0;
        contractToSave = contractToSave || Number(contractDetails.contractType) !== originalContractType;
      }
      contractDetails.retrievedAttributes++;
    })
    .catch((e) => {
      contractDetails.contractType = 0;
    })
    .then(() => contractDetails.decimals || contractDetails.contract.methods.decimals().call())
    .then((decimals) => {
      if (decimals) {
        decimals = Number(decimals);
        contractToSave = contractToSave || Number(contractDetails.decimals) !== decimals;
        contractDetails.decimals = decimals;
        contractDetails.retrievedAttributes++;
      } else {
        contractDetails.decimals = 0;
      }
    })
    .catch((e) => {
      contractDetails.decimals = 0;
    })
    .then(() => account && contractDetails.contract.methods.balanceOf(account).call())
    .then((balance) => {
      if (balance) {
        contractDetails.balance = convertTokenAmountReceived(balance, contractDetails.decimals);
      }
    })
    .catch((e) => {
      contractDetails.contractType = -1;
      console.debug('retrieveContractDetails method - error computing balance', e);
    })
    .then(() => {
      return contractDetails.contractType < 0 ? null : contractDetails.symbol ? contractDetails.symbol : contractDetails.contract.methods.symbol().call();
    })
    .then((symbol) => {
      if (symbol) {
        contractToSave = contractToSave || contractDetails.symbol !== symbol;
        contractDetails.symbol = symbol;
        contractDetails.retrievedAttributes++;
      }
    })
    .catch((e) => {
      contractDetails.contractType = -1;
      console.debug('retrieveContractDetails method - error retrieving symbol', contractDetails.address, new Error(e));
    })
    .then(() => {
      return contractDetails.contractType < 0 ? null : contractDetails.hasOwnProperty('name') ? contractDetails.name : contractDetails.contract.methods.name().call();
    })
    .then((name) => {
      if (name) {
        contractToSave = contractToSave || contractDetails.name !== name;
        contractDetails.name = name;
        contractDetails.title = name;
        contractDetails.retrievedAttributes++;
      }
    })
    .catch((e) => {
      contractDetails.contractType = -1;
      console.debug('retrieveContractDetails method - error retrieving name', contractDetails.address, new Error(e));
    })
    .then(() => {
      return contractDetails.contractType < 0 ? null : contractDetails.hasOwnProperty('totalSupply') ? contractDetails.totalSupply : contractDetails.contract.methods.totalSupply().call();
    })
    .then((totalSupply) => {
      if (totalSupply) {
        contractDetails.totalSupply = totalSupply;
        contractDetails.retrievedAttributes++;
      }
    })
    .catch((e) => {
      console.debug('retrieveContractDetails method - error retrieving totalSupply', contractDetails.address, new Error(e));
    })
    .then(() => contractDetails.contractType > 0 && isAdministration && computeBalance(contractDetails.address))
    .then((contractBalance) => {
      if (contractBalance && contractBalance.balance) {
        contractDetails.contractBalance = contractBalance.balance;
        contractDetails.contractBalanceFiat = contractBalance.balanceFiat;
      }
    })
    .catch((e) => {
      console.debug('retrieveContractDetails method - error retrieving balance of contract', contractDetails.address, new Error(e));
    })
    .then(() => {
      if (!contractDetails.contractType || contractDetails.contractType <= 0) {
        return;
      }
      // Compute ERT Token attributes
      return (contractDetails.sellPrice ? Promise.resolve(Number(contractDetails.sellPrice) * 1000000000000000000) : contractDetails.contract.methods.getSellPrice().call())
        .then((sellPrice) => {
          if (sellPrice && !Number.isNaN(Number(sellPrice))) {
            contractDetails.sellPrice = Number(sellPrice) / 1000000000000000000;
          } else {
            contractDetails.sellPrice = 0;
          }
          contractDetails.retrievedAttributes++;
        })
        .catch((e) => {
          console.debug('retrieveContractDetails method - error retrieving sellPrice', contractDetails.address, new Error(e));
        })
        .then(() => {
          return contractDetails.owner ? contractDetails.owner : contractDetails.contract.methods.owner().call();
        })
        .then((owner) => {
          if (owner) {
            contractDetails.owner = owner.toLowerCase();
            contractDetails.isOwner = contractDetails.owner === account && account.toLowerCase();
          }
          contractDetails.retrievedAttributes++;
        })
        .catch((e) => {
          console.debug('retrieveContractDetails method - error retrieving sellPrice', contractDetails.address, new Error(e));
        })
        .then(() => {
          return contractDetails.hasOwnProperty('isApproved') && !isAdministration ? contractDetails.isApproved : contractDetails.contract.methods.isApprovedAccount(account).call();
        })
        .then((approvedAccount) => {
          contractDetails.isApproved = approvedAccount;
        })
        .catch((e) => {
          console.debug('retrieveContractDetails method - error retrieving isApprovedAccount', contractDetails.address, new Error(e));
        })
        .then(() => {
          return contractDetails.hasOwnProperty('adminLevel') ? contractDetails.adminLevel : contractDetails.contract.methods.getAdminLevel(account).call();
        })
        .then((habilitationLevel) => {
          if (habilitationLevel) {
            contractDetails.adminLevel = Number(habilitationLevel);
            contractDetails.isAdmin = habilitationLevel > 0;
            if (contractDetails.isAdmin) {
              contractDetails.isApproved = true;
            }
          }
        })
        .catch((e) => {
          console.debug('retrieveContractDetails method - error retrieving getAdminLevel', contractDetails.address, new Error(e));
        })
        .then(() => {
          return contractDetails.hasOwnProperty('isPaused') ? contractDetails.isPaused : contractDetails.contract.methods.isPaused().call();
        })
        .then((isPaused) => {
          contractDetails.isPaused = isPaused ? true : false;
          contractDetails.retrievedAttributes++;
        })
        .catch((e) => {
          console.debug('retrieveContractDetails method - error retrieving isPaused', contractDetails.address, new Error(e));
        });
    })
    .catch((e) => {
      console.debug('retrieveContractDetails method - error retrieving ERT Token', account, contractDetails, new Error(e), e);
      return contractDetails;
    })
    .then(() => {
      if (contractDetails.contractType < 0 || contractDetails.retrievedAttributes === 0) {
        transformContracDetailsToFailed(contractDetails);
      } else if (contractToSave && window.walletSettings.isAdmin) {
        refreshContractOnServer();
      }

      if (!window.walletContractsDetails) {
        window.walletContractsDetails = {};
      }

      if (contractDetails.contractType > 0) {
        contractDetails.contractTypeLabel = `ERT Token V${contractDetails.contractType}`;
        window.walletContractsDetails[contractDetails.address] = contractDetails;
        window.walletContractsDetails[contractDetails.address.toLowerCase()] = contractDetails;
      } else if (contractDetails.contractType === 0) {
        contractDetails.contractTypeLabel = 'Standard ERC20 Token';
        window.walletContractsDetails[contractDetails.address] = contractDetails;
        window.walletContractsDetails[contractDetails.address.toLowerCase()] = contractDetails;
      } else {
        contractDetails.contractTypeLabel = 'Non ERC20 Contract';
      }
      return contractDetails;
    });
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
  return fetch(`/portal/rest/wallet/api/contract/getContract?address=${address}`, {
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
  const networkId = window.walletSettings.network.id;
  const STORAGE_KEY = `exo-wallet-contract-deployment-progress-${networkId}`;
  const storageValue = localStorage.getItem(STORAGE_KEY);
  if (storageValue === null) {
    return {};
  } else {
    return JSON.parse(storageValue);
  }
}

export function removeContractDeploymentTransactionsInProgress(transactionHash) {
  const networkId = window.walletSettings.network.id;
  const STORAGE_KEY = `exo-wallet-contract-deployment-progress-${networkId}`;
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

export function sendContractTransaction(txDetails, hashCallback, receiptCallback, confirmedCallback, errorCallback) {
  const networkId = window.walletSettings.network.id;

  // suppose you want to call a function named myFunction of myContract
  const data = txDetails.method(...txDetails.parameters).encodeABI();
  const transactionToSend = {
    to: txDetails.contractAddress,
    from: txDetails.senderAddress,
    data: data,
    gas: txDetails.gas,
    gasPrice: txDetails.gasPrice,
  };

  return getLastNonce(txDetails.senderAddress).then((nonce) => {
    const networkId = window.walletSettings.network.id;

    // Increment manually nonce if we have the last transaction always pending
    if (nonce && Number(nonce) > 0) {
      transactionToSend.nonce = nonce + 1;
    }

    return window.localWeb3.eth
      .sendTransaction(transactionToSend)
      .on('transactionHash', (hash) => {
        if (hashCallback) {
          return hashCallback(hash);
        }
      })
      .on('receipt', (receipt) => {
        if (receiptCallback) {
          return receiptCallback(receipt);
        }
      })
      .on('confirmation', (confirmationNumber, receipt) => {
        if (confirmedCallback) {
          return confirmedCallback(confirmationNumber, receipt);
        }
      })
      .on('error', (error, receipt) => {
        if (errorCallback) {
          return errorCallback(error, receipt);
        }
      });
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
        bin: contractBin
      }
    });
}

function refreshContractOnServer() {
  return fetch('/portal/rest/wallet/api/contract/refresh', {
    method: 'GET',
  }).then((resp) => {
    return resp && resp.ok;
  });
}
