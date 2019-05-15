import {convertTokenAmountReceived, computeBalance} from './WalletUtils.js';
import {getLastNonce} from './TransactionUtils.js';

/*
 * Get the list of Contracts with details:
 * {
 *   name: name of contract,
 *   symbol: symbol of Token currency,
 *   balance: balance of current account in Tokens,
 *   contract: web3.js contract object,
 *   icon: contract icon,
 *   error: true if there is an error,
 *   isContract: true,
 *   isDefault: is default contract coming from configuration
 * }
 */
export function getContractsDetails(account, netId, onlyDefault, isAdministration) {
  let contractsAddresses = [];
  if (onlyDefault) {
    contractsAddresses = window.walletSettings.defaultContractsToDisplay || [];
  } else {
    const overviewAccounts = window.walletSettings.userPreferences.overviewAccountsToDisplay || [];
    contractsAddresses = overviewAccounts.filter((contractAddress) => contractAddress && contractAddress.indexOf('0x') === 0);
  }

  const contractsDetailsPromises = [];
  for (let i = 0; i < contractsAddresses.length; i++) {
    const address = contractsAddresses[i];
    if (address && address.trim().length) {
      const contractDetails = {};
      contractDetails.address = address;
      contractDetails.icon = 'fa-file-contract';
      contractDetails.networkId = netId;
      contractDetails.isDefault = window.walletSettings.defaultContractsToDisplay && window.walletSettings.defaultContractsToDisplay.indexOf(address) > -1;
      contractsDetailsPromises.push(retrieveContractDetails(account, contractDetails, isAdministration));
    }
  }
  if (!window.walletContractsDetails) {
    window.walletContractsDetails = {};
  }
  return Promise.all(contractsDetailsPromises);
}

/*
 * Retrieve an ERC20 contract instance at specified address
 */
export function retrieveContractDetails(account, contractDetails, isAdministration, ignoreSavedDetails, avoidSaving) {
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

  return (ignoreSavedDetails ? Promise.resolve(null) : getSavedContractDetails(contractDetails.address, contractDetails.networkId))
    .then((savedDetails) => {
      if (savedDetails) {
        contractDetails.isContract = true;

        if (!savedDetails.hasOwnProperty('contractType')) {
          contractToSave = true;
        }

        Object.keys(savedDetails).forEach((key) => {
          contractDetails[key] = savedDetails[key];
        });
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
      }
    })
    .catch((e) => {
      console.debug('retrieveContractDetails method - error retrieving saved details', contractDetails.address, new Error(e));
    })
    .then(() => contractDetails.contractType > 1 || contractDetails.contract.methods.version().call())
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
      } else if (!avoidSaving && contractToSave && window.walletSettings.isAdmin) {
        saveContractAddressOnServer(contractDetails);
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
export function getSavedContractDetails(address, networkId) {
  return fetch(`/portal/rest/wallet/api/contract/getContract?address=${address}&networkId=${networkId}`, {
    method: 'GET',
    credentials: 'include',
  })
    .then((resp) => {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        throw new Error(`Error getting contract details from server with address ${address} on network with id ${networkId}`);
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

/*
 * Removes a Contract address defined as default contract to display for all users
 */
export function removeContractAddressFromDefault(address) {
  return fetch('/portal/rest/wallet/api/contract/remove', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: $.param({
      address: address,
      networkId: window.walletSettings.currentNetworkId,
    }),
  }).then((resp) => {
    if (resp && resp.ok) {
      window.walletSettings.defaultContractsToDisplay.splice(window.walletSettings.defaultContractsToDisplay.indexOf(address), 1);
    } else {
      throw new Error('Error deleting contract as default');
    }
  });
}

/*
 * Save a new Contract address as default contract to display for all users
 */
export function saveContractAddressAsDefault(contractDetails) {
  console.debug('save contract as default', contractDetails);
  contractDetails.defaultContract = true;
  return saveContractAddressOnServer(contractDetails);
}

/*
 * Save a new Contract address as default contract to display for all users
 */
export function saveContractAddressOnServer(contractDetails) {
  contractDetails.address = contractDetails.address.toLowerCase();
  return fetch('/portal/rest/wallet/api/contract/save', {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      name: contractDetails.name,
      symbol: contractDetails.symbol,
      address: contractDetails.address,
      networkId: contractDetails.networkId,
      owner: contractDetails.owner,
      contractType: contractDetails.contractType,
      sellPrice: contractDetails.sellPrice,
      decimals: contractDetails.decimals,
      defaultContract: contractDetails.defaultContract ? contractDetails.defaultContract : false,
    }),
  }).then((resp) => {
    if (!window.walletSettings.defaultContractsToDisplay) {
      window.walletSettings.defaultContractsToDisplay = [];
    }
    if (resp && resp.ok && window.walletSettings.defaultContractsToDisplay.indexOf(contractDetails.address) < 0) {
      window.walletSettings.defaultContractsToDisplay.push(contractDetails.address);
    }
    return resp;
  });
}

/*
 * Validate Contract existence and save its address in localStorage
 */
export function saveContractAddress(account, address, netId, isDefaultContract) {
  if (isDefaultContract && window.walletSettings && window.walletSettings.defaultContractsToDisplay && window.walletSettings.defaultContractsToDisplay.indexOf(address) >= 0) {
    return Promise.reject(new Error('Contract already exists in the list'));
  }
  return getContractInstance(account, address, true)
    .catch((e) => {
      console.debug('saveContractAddress method - error getting contract instance', e);
    })
    .then((foundContract, error) => {
      if (error) {
        console.debug('saveContractAddress method - error getting contract instance', error);
        throw error;
      }
      // Test on existence of balanceOf method in contract code
      return foundContract.methods.balanceOf(account).call();
    })
    .then((balance, error) => {
      if (error) {
        console.debug('saveContractAddress method - error getting balance of user', error);
        throw new Error('Invalid contract address');
      }

      let overviewAccounts = window.walletSettings.userPreferences.overviewAccounts || [];
      overviewAccounts = overviewAccounts.filter((contractAddress) => contractAddress && contractAddress.indexOf('0x') === 0);
      if (isDefaultContract || overviewAccounts.indexOf(address) < 0) {
        return retrieveContractDetails(account, {address: address, networkId: netId}, true, true, true).then((contractDetails, error) => {
          if (error) {
            throw error;
          }
          if (contractDetails && !contractDetails.error) {
            return contractDetails;
          } else {
            return false;
          }
        });
      } else if (!isDefaultContract) {
        throw new Error('Contract already exists');
      }
    })
    .catch((e) => {
      console.debug('saveContractAddress method - error getting balance of user', e);
      throw new Error('It seems that the addres is not a valid ERC20 contract');
    })
    .then((contractDetails) => {
      if (contractDetails && isDefaultContract) {
        return saveContractAddressAsDefault(contractDetails).then(() => contractDetails);
      }
      return contractDetails;
    });
}

export function getContractDeploymentTransactionsInProgress(networkId) {
  const STORAGE_KEY = `exo-wallet-contract-deployment-progress-${networkId}`;
  const storageValue = localStorage.getItem(STORAGE_KEY);
  if (storageValue === null) {
    return {};
  } else {
    return JSON.parse(storageValue);
  }
}

export function removeContractDeploymentTransactionsInProgress(networkId, transactionHash) {
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
    const contractInstance = new window.localWeb3.eth.Contract(abi ? abi : window.walletSettings.contractAbi, address, {
      from: account && account.toLowerCase(),
      gas: window.walletSettings.userPreferences.defaultGas,
      gasPrice: window.walletSettings.normalGasPrice,
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

export function sendContractTransaction(useMetamask, networkId, txDetails, hashCallback, receiptCallback, confirmedCallback, errorCallback) {
  // suppose you want to call a function named myFunction of myContract
  const data = txDetails.method(...txDetails.parameters).encodeABI();
  const transactionToSend = {
    to: txDetails.contractAddress,
    from: txDetails.senderAddress,
    data: data,
    gas: txDetails.gas,
    gasPrice: txDetails.gasPrice,
  };

  return getLastNonce(networkId, txDetails.senderAddress, useMetamask).then((nonce) => {
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