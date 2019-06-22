import * as constants from './Constants';
import {searchWalletByTypeAndId, saveNewAddress} from './AddressRegistry';

const DECIMALS = 3;
const DECIMALS_POW = Math.pow(10, DECIMALS);

export function etherToFiat(amount) {
  if (window.walletSettings.fiatPrice && amount) {
    return toFixed(window.walletSettings.fiatPrice * amount);
  }
  return 0;
}

export function gasToEther(amount, gasPriceInEther) {
  if (gasPriceInEther && amount) {
    return gasPriceInEther * amount;
  }
  return 0;
}

export function sendPrivateKeyToServer(walletAddress, password, newPassword) {
  walletAddress = walletAddress || window.walletSettings.wallet.address;
  if (!walletAddress) {
    return Promise.reject(new Error("Can't find current wallet address"));
  }
  walletAddress = walletAddress.toLowerCase();

  if (!localStorage.getItem(walletAddress)) {
    return Promise.reject(new Error('No private key was found'));
  }

  return fetch(`/portal/rest/wallet/api/account/savePrivateKey`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: $.param({
      address: walletAddress,
      privateKey: localStorage.getItem(walletAddress),
    }),
  }).then((resp) => {
    if (resp && resp.ok) {
      setWalletBackedUp(walletAddress, true);
      return retrievePrivateKeyFromServer(walletAddress);
    } else {
      throw new Error('Error saving keys on server');
    }
  });
}

export function removeServerSideBackup(walletAddress) {
  walletAddress = walletAddress || window.walletSettings.wallet.address;
  if (!walletAddress) {
    return Promise.reject(new Error("Can't find current wallet address"));
  }
  walletAddress = walletAddress.toLowerCase();

  return fetch(`/portal/rest/wallet/api/account/removePrivateKey?address=${walletAddress}`, {
    credentials: 'include',
  }).then((resp) => {
    return resp && resp.ok;
  });
}

export function retrievePrivateKeyFromServer(walletAddress) {
  walletAddress = walletAddress || window.walletSettings.wallet.address;
  if (!walletAddress) {
    return Promise.reject(new Error("Can't find current wallet address"));
  }

  return fetch(`/portal/rest/wallet/api/account/getPrivateKey?address=${walletAddress}`, {credentials: 'include'})
    .then((resp) => {
      if (resp && resp.ok) {
        return resp.text();
      }
    })
    .then((privateKey) => {
      if (!privateKey) {
        throw new Error('No private key was found on server');
      }
      localStorage.setItem(walletAddress, privateKey);
      return true;
    });
}

export function gasToFiat(amount, gasPriceInEther) {
  if (window.walletSettings && window.walletSettings.fiatPrice && gasPriceInEther && amount) {
    return toFixed(gasPriceInEther * window.walletSettings.fiatPrice * amount);
  }
  return 0;
}

export function retrieveFiatExchangeRate() {
  window.walletSettings.fiatSymbol = window.walletSettings.userPreferences.currency && constants.FIAT_CURRENCIES[window.walletSettings.userPreferences.currency] ? constants.FIAT_CURRENCIES[window.walletSettings.userPreferences.currency].symbol : '$';

  const currency = window.walletSettings && window.walletSettings.userPreferences.currency ? window.walletSettings.userPreferences.currency : 'usd';
  // Retrieve Fiat <=> Ether exchange rate
  return retrieveFiatExchangeRateOnline(currency)
    .then((content) => {
      if (content && content.length && content[0][`price_${currency}`]) {
        localStorage.setItem(`exo-wallet-exchange-${currency}`, JSON.stringify(content));
      } else {
        // Try to get old information from local storage
        content = localStorage.getItem(`exo-wallet-exchange-${currency}`);
        if (content) {
          content = JSON.parse(content);
        }
      }

      window.walletSettings.usdPrice = content ? parseFloat(content[0].price_usd) : 0;
      window.walletSettings.fiatPrice = content ? parseFloat(content[0][`price_${currency}`]) : 0;
      window.walletSettings.priceLastUpdated = content ? new Date(parseInt(content[0].last_updated) * 1000) : null;
    })
    .then(() => {
      if (window.retrieveFiatExchangeRateInterval) {
        clearInterval(window.retrieveFiatExchangeRateInterval);
      }
      window.retrieveFiatExchangeRateInterval = setTimeout(() => {
        retrieveFiatExchangeRate();
      }, 300000);
    });
}

export function initEmptyWeb3Instance() {
  window.localWeb3 = new LocalWeb3();
}

export function initWeb3(isSpace, isAdmin) {
  if (!window.walletSettings || !window.walletSettings.userPreferences) {
    // User settings aren't loaded
    throw new Error(constants.ERROR_WALLET_SETTINGS_NOT_LOADED);
  }

  createLocalWeb3Instance(isSpace);
  return checkNetworkStatus();
}

export function initSettings(isSpace, spaceGroup) {
  const spaceId = (isSpace && (spaceGroup || window.walletSpaceGroup)) || '';

  clearCache();

  return fetch(`/portal/rest/wallet/api/settings?spaceId=${spaceId}`, {credentials: 'include'})
    .then((resp) => {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then((settings) => {
      window.walletSettings = window.walletSettings || {};
      if (!settings || !(settings.walletEnabled || settings.isAdmin)) {
        window.walletSettings = {walletEnabled: false};
      }

      window.walletSettings.userPreferences = {currency: 'usd'};
      window.walletSettings.wallet = {};
      window.walletSettings.network = {};
      window.walletSettings.contractDetail = {};

      window.walletSettings = $.extend(window.walletSettings, settings);
      window.walletSettings = $.extend(window.walletSettings, settings);
    })
    .then(() => {
      if (window.walletSettings.userPreferences && window.walletSettings.userPreferences.hasKeyOnServerSide && window.walletSettings.wallet.address) {
        return retrievePrivateKeyFromServer(window.walletSettings.wallet.address)
          .then((privateKey) => {
            if (!privateKey) {
              return;
            }
          })
          .catch((e) => {
            console.debug('Error retrieving wallet private key from server', e);
          });
      }
    })
    .then(() => {
      window.walletSettings.browserWalletExists = browserWalletExists(window.walletSettings.wallet.address);

      if (!window.walletSettings.defaultOverviewAccounts || !window.walletSettings.defaultOverviewAccounts.length) {
        if (window.walletSettings.defaultContractsToDisplay) {
          window.walletSettings.defaultOverviewAccounts = window.walletSettings.defaultContractsToDisplay.slice();
          window.walletSettings.defaultOverviewAccounts.unshift('fiat', 'ether');
        } else {
          window.walletSettings.defaultOverviewAccounts = ['fiat', 'ether'];
        }
      }
      window.walletSettings.defaultPrincipalAccount = window.walletSettings.defaultPrincipalAccount || window.walletSettings.defaultOverviewAccounts[0];
      window.walletSettings.userPreferences.overviewAccounts = window.walletSettings.userPreferences.overviewAccounts || window.walletSettings.defaultOverviewAccounts || [];

      // Remove contracts that are removed from administration
      if (window.walletSettings.defaultContractsToDisplay && window.walletSettings.defaultContractsToDisplay.length) {
        window.walletSettings.userPreferences.overviewAccounts = window.walletSettings.userPreferences.overviewAccounts.filter((contractAddress) => contractAddress && (contractAddress.trim().indexOf('0x') < 0 || window.walletSettings.defaultContractsToDisplay.indexOf(contractAddress.trim()) >= 0));
      }

      // Display configured default contracts to display in administration
      window.walletSettings.userPreferences.overviewAccountsToDisplay = window.walletSettings.userPreferences.overviewAccounts.slice(0);
      if (window.walletSettings.defaultOverviewAccounts && window.walletSettings.defaultOverviewAccounts.length) {
        window.walletSettings.defaultOverviewAccounts.forEach((defaultOverviewAccount) => {
          if (defaultOverviewAccount && defaultOverviewAccount.indexOf('0x') === 0 && window.walletSettings.userPreferences.overviewAccountsToDisplay.indexOf(defaultOverviewAccount) < 0) {
            window.walletSettings.userPreferences.overviewAccountsToDisplay.unshift(defaultOverviewAccount);
          }
        });
      }

      const accountId = getRemoteId(isSpace);
      const address = window.walletSettings.wallet.address;
      if (address) {
        window.walletSettings.userP = localStorage.getItem(`exo-wallet-${address}-userp`);
        window.walletSettings.storedPassword = window.walletSettings.userP && window.walletSettings.userP.length > 0;
        window.walletSettings.userPreferences.autoGenerated = !window.walletSettings.userPreferences.hasKeyOnServerSide && localStorage.getItem(`exo-wallet-${address}-userp-autoGenerated`);
        window.walletSettings.userPreferences.backedUp = window.walletSettings.userPreferences.hasKeyOnServerSide || localStorage.getItem(`exo-wallet-${address}-userp-backedup`);
      }

      if (isSpace && accountId) {
        return initSpaceAccount(accountId).then(retrieveFiatExchangeRate);
      } else {
        return retrieveFiatExchangeRate();
      }
    })
    .catch((e) => {
      console.debug('initSettings method - error', e);
      throw e;
    });
}

export function watchTransactionStatus(hash, transactionFinishedcallback) {
  hash = hash.toLowerCase();

  if (!window.watchingTransactions) {
    window.watchingTransactions = {};
  }

  if (!window.watchingTransactions[hash]) {
    window.watchingTransactions[hash] = [transactionFinishedcallback];
    waitAsyncForTransactionStatus(hash, null);
  } else {
    window.watchingTransactions[hash].push(transactionFinishedcallback);
  }
}

export function getTransactionReceipt(hash) {
  return window.localWeb3.eth.getTransactionReceipt(hash);
}

export function getTransaction(hash) {
  return window.localWeb3.eth.getTransaction(hash);
}

export function computeNetwork() {
  return window.localWeb3.eth.net
    .getId()
    .then((networkId, error) => {
      if (error) {
        console.debug('Error computing network id', error);
        throw error;
      }
      if (networkId) {
        console.debug(`Detected network id: ${networkId}`);
        window.walletSettings.currentNetworkId = networkId;
        return window.localWeb3.eth.net.getNetworkType();
      } else {
        console.debug('Network is disconnected');
        throw new Error('Network is disconnected');
      }
    })
    .then((netType, error) => {
      if (error) {
        console.debug('Error computing network type', error);
        throw error;
      }
      if (netType) {
        window.walletSettings.currentNetworkType = netType;
        console.debug('Detected network type:', netType);
      }
    });
}

export function computeBalance(account) {
  if (!window.localWeb3) {
    return Promise.reject(new Error("You don't have a wallet yet"));
  }
  return window.localWeb3.eth
    .getBalance(account)
    .then((retrievedBalance, error) => {
      if (error && !retrievedBalance) {
        console.debug(`error retrieving balance of ${account}`, new Error(error));
        throw error;
      }
      if (retrievedBalance) {
        return window.localWeb3.utils.fromWei(String(retrievedBalance), 'ether');
      } else {
        return 0;
      }
    })
    .then((retrievedBalance, error) => {
      if (error) {
        throw error;
      }
      return {
        balance: retrievedBalance,
        balanceFiat: etherToFiat(retrievedBalance),
      };
    })
    .catch((e) => {
      console.debug('Error retrieving balance of account', account, e);
      return null;
    });
}

export function saveBrowserWalletInstance(wallet, password, isSpace, autoGenerateWallet, backedUp) {
  const account = window.localWeb3.eth.accounts.wallet.add(wallet);
  const address = account['address'].toLowerCase();

  return saveNewAddress(isSpace ? window.walletSpaceGroup : eXo.env.portal.userName, isSpace ? 'space' : 'user', address, true)
    .then((resp, error) => {
      if (error) {
        throw error;
      }
      if (resp && resp.ok) {
        return resp.text();
      } else {
        throw new Error('Error saving new Wallet address');
      }
    })
    .then((phrase, error) => {
      window.walletSettings.userPreferences = window.walletSettings.userPreferences || {};
      window.walletSettings.userPreferences.phrase = phrase;
      saveBrowserWallet(password, phrase, address, autoGenerateWallet, autoGenerateWallet);
      setWalletBackedUp(address, backedUp);
    })
    .then(() => sendPrivateKeyToServer(address, password));
}

export function saveBrowserWallet(password, phrase, address, autoGenerated, save) {
  if (!phrase) {
    phrase = window.walletSettings.userPreferences.phrase;
  }

  if (!address) {
    address = window.walletSettings.wallet.address;
  }

  if (!password || !password.length) {
    throw new Error('Password is mandatory');
  }
  if (!phrase || !phrase.length) {
    throw new Error('Empty user settings');
  }
  if (!address || !address.length) {
    throw new Error('Address is empty');
  }

  password = hashCode(password);

  // Create wallet with user password phrase and personal eXo Phrase generated
  // To avoid having the complete passphrase that allows to decrypt wallet in a
  // single location
  const saved = window.localWeb3.eth.accounts.wallet.save(password + phrase, address);
  if (!saved || !browserWalletExists(address)) {
    throw new Error('An unknown error occrred while saving new wallet');
  }

  rememberPassword(save || autoGenerated, password, address);

  if (autoGenerated) {
    localStorage.setItem(`exo-wallet-${address}-userp-autoGenerated`, 'true');
  } else {
    localStorage.removeItem(`exo-wallet-${address}-userp-autoGenerated`);
  }

  if (!unlockBrowserWallet(password, phrase, address)) {
    throw new Error('An unknown error occrred while unlocking newly saved wallet');
  } else {
    lockBrowserWallet(address);
  }
}

export function rememberPassword(remember, password, address) {
  address = address || window.walletSettings.wallet.address;
  if (!address) {
    throw new Error("Can't find address of user");
  }
  if (remember) {
    localStorage.setItem(`exo-wallet-${address}-userp`, password);
    window.walletSettings.userP = password;
    window.walletSettings.storedPassword = true;
  } else {
    localStorage.removeItem(`exo-wallet-${address}-userp`);
    window.walletSettings.userP = null;
    window.walletSettings.storedPassword = false;
  }
}

export function getAddressEtherscanlink(networkId) {
  if (networkId) {
    switch (networkId) {
      case 1:
        return 'https://etherscan.io/address/';
      case 3:
        return 'https://ropsten.etherscan.io/address/';
      default:
        return '#';
    }
  }
  return null;
}

export function getTokenEtherscanlink(networkId) {
  if (networkId) {
    switch (networkId) {
      case 1:
        return 'https://etherscan.io/token/';
      case 3:
        return 'https://ropsten.etherscan.io/token/';
      default:
        return '#';
    }
  }
  return null;
}

export function getTransactionEtherscanlink(networkId) {
  if (networkId) {
    switch (networkId) {
      case 1:
        return 'https://etherscan.io/tx/';
      case 3:
        return 'https://ropsten.etherscan.io/tx/';
      default:
        return '#';
    }
  }
  return null;
}

export function getCurrentBrowserWallet() {
  return window && window.localWeb3 && window.localWeb3.eth.accounts.wallet && window.walletSettings.wallet.address && window.localWeb3.eth.accounts.wallet[window.walletSettings.wallet.address];
}

export function lockBrowserWallet(address) {
  address = address || window.walletSettings.wallet.address;
  if (address) {
    window.localWeb3.eth.accounts.wallet.remove(address);
  }
}

export function unlockBrowserWallet(password, phrase, address) {
  if (!phrase || !phrase.length) {
    phrase = window.walletSettings.userPreferences.phrase;
  }

  if (!address || !address.length) {
    address = window.walletSettings.wallet.address;
  }

  if (!password || !password.length) {
    password = window.walletSettings.userP;
  }

  if (!password || !password.length || !phrase || !phrase.length || !address || !address.length) {
    return false;
  }

  try {
    // lock previously unlocked wallet first
    if (isWalletUnlocked(address)) {
      lockBrowserWallet(address);
    }

    window.localWeb3.eth.accounts.wallet.load(password + phrase, address);
  } catch (e) {
    console.debug('error while unlocking wallet', e);
    return false;
  }

  return isWalletUnlocked(address);
}

function isWalletUnlocked(address) {
  return window.localWeb3.eth.accounts.wallet.length > 0 && window.localWeb3.eth.accounts.wallet[address] && window.localWeb3.eth.accounts.wallet[address].privateKey;
}

export function setWalletBackedUp(address, backedUp) {
  if (!address || !address.length) {
    address = window.walletSettings.wallet.address;
  }
  if (backedUp) {
    localStorage.setItem(`exo-wallet-${address}-userp-backedup`, 'true');
  } else {
    localStorage.removeItem(`exo-wallet-${address}-userp-backedup`);
  }
  window.walletSettings.userPreferences.backedUp = backedUp;
}

export function hashCode(s) {
  let h = 0,
    i = 0;
  if (s && s.length > 0) {
    while (i < s.length) {
      h = ((h << 5) - h + s.charCodeAt(i++)) | 0;
    }
  }
  return String(h);
}

export function truncateError(error) {
  if (!error) {
    return '';
  }
  error = String(error);
  if (error.indexOf(' at ') >= 0) {
    error = error.substring(0, error.indexOf(' at '));
  }

  if (error.indexOf('replacement transaction underpriced') >= 0 || error.indexOf('known transaction') >= 0) {
    error = 'Another transaction is in progress please wait until the first transaction is finished';
  }
  return error;
}

export function generatePassword() {
  return Math.random()
    .toString(36)
    .slice(2);
}

export function markFundRequestAsSent(notificationId) {
  return fetch(`/portal/rest/wallet/api/account/markFundRequestAsSent?notificationId=${notificationId}`, {credentials: 'include'}).then((resp) => {
    return resp && resp.ok;
  });
}

export function checkFundRequestStatus(notificationId) {
  return fetch(`/portal/rest/wallet/api/account/fundRequestSent?notificationId=${notificationId}`, {credentials: 'include'})
    .then((resp) => {
      if (resp && resp.ok) {
        return resp.text();
      } else {
        throw new Error('Error checking fund request status');
      }
    })
    .then((content) => content === 'true');
}

export function getWallets() {
  return fetch(`/portal/rest/wallet/api/account/list`, {credentials: 'include'}).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error retrieving wallets');
    }
  });
}

export function removeWalletAssociation(address) {
  return fetch(`/portal/rest/wallet/api/account/remove?address=${address}`, {credentials: 'include'}).then((resp) => resp && resp.ok);
}

export function enableWallet(address, enable) {
  return fetch(`/portal/rest/wallet/api/account/enable?address=${address}&enable=${enable}`, {credentials: 'include'}).then((resp) => resp && resp.ok);
}

export function setDraggable() {
  if (!$.draggable) {
    return;
  }
  if ($('#WalletApp .v-dialog:not(.not-draggable)').length) {
    $('#WalletApp .v-dialog:not(.not-draggable)').draggable();
  } else if ($('#WalletAdminApp .v-dialog:not(.not-draggable)').length) {
    $('#WalletAdminApp .v-dialog:not(.not-draggable)').draggable();
  }
}

/*
 * return amount * 10 ^ decimals
 */
export function convertTokenAmountToSend(amount, decimals) {
  if (decimals === 0) {
    return amount;
  }
  const toBN = LocalWeb3.utils.toBN;
  const base = toBN(10).pow(toBN(decimals));
  const negative = String(amount).substring(0, 1) === '-';

  if (negative) {
    amount = amount.substring(1);
  }
  const comps = String(amount).split('.');
  let integer = comps[0];
  let fraction = comps[1] ? comps[1] : '0';
  if (fraction.length > decimals) {
    throw new Error(`number Fractions ${fraction.length} exceed number of decimals ${decimals}`);
  }
  while (fraction.length < decimals) {
    fraction += '0';
  }
  integer = toBN(integer);
  fraction = toBN(fraction);
  let result = integer.mul(base).add(fraction);
  if (negative) {
    result = result.mul(-1);
  }
  return result.toString(10);
}

/*
 * return amount * 10 ^ decimals
 */
export function convertTokenAmountReceived(amount, decimals) {
  if (decimals === 0) {
    return amount;
  }
  const toBN = LocalWeb3.utils.toBN;
  let amountBN = toBN(amount);
  const negative = amountBN.lt(0);
  const base = toBN(10).pow(toBN(decimals));

  if (negative) {
    amountBN = amountBN.mul(-1);
  }
  let fraction = amountBN.mod(base).toString(10);
  while (fraction.length < decimals) {
    fraction = `0${fraction}`;
  }
  fraction = fraction.match(/^([0-9]*[1-9]|0)(0*)/)[1];
  const whole = amountBN.div(base).toString(10);
  let value = `${whole}${fraction === '0' ? '' : `.${fraction}`}`;
  if (negative) {
    value = `-${value}`;
  }
  return value;
}

export function estimateTransactionFeeEther(gas, gasPrice) {
  if (!gasPrice || !gas) {
    return 0;
  }
  const gasFeeWei = parseInt(gas * gasPrice);
  return LocalWeb3.utils.fromWei(String(gasFeeWei), 'ether');
}

export function estimateTransactionFeeFiat(gas, gasPrice) {
  if (!gasPrice || !gas) {
    return 0;
  }
  return etherToFiat(estimateTransactionFeeEther(gas, gasPrice));
}

export function toFixed(value, decimals) {
  if (!decimals) {
    decimals = 3;
  }
  const number = value ? Number(String(value).trim()) : 0;
  if (Number.isNaN(number) || !Number.isFinite(number) || !number) {
    return 0;
  }
  value = String(number);
  const toBN = LocalWeb3.utils.toBN;
  const negative = value.substring(0, 1) === '-';
  if (negative) {
    value = value.substring(1);
  }
  const comps = value.split('.');
  let integer = comps[0];
  let fraction = comps.length > 0 ? comps[1] : '0';
  if (fraction && fraction.length > decimals) {
    fraction = String(Math.round(Number('0.' + fraction) * DECIMALS_POW) / DECIMALS_POW).substring(2); // eslint-disable-line prefer-template
  }
  if (!fraction || Number(fraction) === 0) {
    fraction = null;
  }
  integer = toBN(integer);
  if (negative) {
    integer = integer.mul(-1);
  }
  if (fraction && fraction.length) {
    return `${integer}.${fraction}`;
  } else {
    return integer.toString();
  }
}

export function saveWalletInitializationStatus(address, status) {
  return fetch(`/portal/rest/wallet/api/account/setInitializationStatus?address=${address}&status=${status}`, {
      credentials: 'include',
    }).then((resp) => {
      if(!resp || !resp.ok) {
        throw new Error('Error while changing initialization status of wallet');
      }
    });
}

function createLocalWeb3Instance(isSpace) {
  if (window.walletSettings.wallet.address) {
    window.localWeb3 = new LocalWeb3(new LocalWeb3.providers.HttpProvider(window.walletSettings.providerURL));
    window.localWeb3.eth.defaultAccount = window.walletSettings.wallet.address.toLowerCase();

    if (isSpace && !window.walletSettings.wallet.isSpaceAdministrator) {
      window.walletSettings.isReadOnly = true;
    } else {
      window.walletSettings.isReadOnly = !window.walletSettings.browserWalletExists;
    }
  } else {
    // Wallet not configured
    throw new Error(constants.ERROR_WALLET_NOT_CONFIGURED);
  }
}

function isBrowserWallet(id, type, address) {
  address = address.toLowerCase();
  return localStorage.getItem(`exo-wallet-${type}-${id}`) === address;
}

function checkNetworkStatus(waitTime, tentativesCount) {
  if (!waitTime) {
    waitTime = 300;
  }
  if (!tentativesCount) {
    tentativesCount = 1;
  }
  // Test if network is connected: isListening operation can hang up forever
  window.localWeb3.eth.net.isListening().then((listening) => (window.walletSettings.isListening = window.walletSettings.isListening || listening));
  return new Promise((resolve) => setTimeout(resolve, waitTime))
    .then(() => {
      if (!window.walletSettings.isListening) {
        console.debug('The network seems to be disconnected');
        throw new Error(constants.ERROR_WALLET_DISCONNECTED);
      }
    })
    .then(() => computeNetwork())
    .then(() => console.debug('Network status: OK'))
    .then(() => constants.OK)
    .catch((error) => {
      if (tentativesCount > 10) {
        throw error;
      }
      console.debug('Reattempt to connect with wait time:', waitTime, ' tentative : ', tentativesCount);
      return checkNetworkStatus(waitTime, ++tentativesCount);
    });
}

function initSpaceAccount(spaceGroup) {
  return searchWalletByTypeAndId(spaceGroup, 'space').then((spaceObject, error) => {
    if (error) {
      throw error;
    }
    if (spaceObject && spaceObject.spaceAdministrator) {
      return (window.walletSettings.wallet.isSpaceAdministrator = true);
    } else {
      window.walletSettings.isReadOnly = true;
      return (window.walletSettings.wallet.isSpaceAdministrator = false);
    }
  });
}

function waitAsyncForTransactionStatus(hash, transaction) {
  if (!transaction || !transaction.blockHash || !transaction.blockNumber) {
    getTransaction(hash).then((transaction) => {
      setTimeout(() => {
        waitAsyncForTransactionStatus(hash, transaction);
      }, 2000);
    });
  } else {
    window.localWeb3.eth
      .getBlock(transaction.blockHash, false)
      .then((block) => {
        // Sometimes the block is not available on time
        if (!block) {
          setTimeout(() => {
            waitAsyncForTransactionStatus(hash, transaction);
          }, 2000);
          return;
        }
        return getTransactionReceipt(hash).then((receipt) => {
          if (window.watchingTransactions[hash] && window.watchingTransactions[hash].length) {
            window.watchingTransactions[hash].forEach((callback) => {
              callback(receipt, block);
            });
            window.watchingTransactions[hash] = null;
          }
        });
      })
      .catch((error) => {
        if (window.watchingTransactions[hash] && window.watchingTransactions[hash].length) {
          window.watchingTransactions[hash].forEach((callback) => {
            callback(null, null);
          });
        }
      });
  }
}

function getRemoteId(isSpace) {
  return isSpace ? window.walletSpaceGroup : eXo.env.portal.userName;
}

function browserWalletExists(address) {
  let encryptedWalletObject = localStorage.getItem(address);
  if (encryptedWalletObject) {
    encryptedWalletObject = JSON.parse(encryptedWalletObject);
  }

  return encryptedWalletObject !== null && encryptedWalletObject.length > 0 && encryptedWalletObject[0] != null && encryptedWalletObject[0].address && (encryptedWalletObject[0].address.toLowerCase() === address.toLowerCase() || `0x${encryptedWalletObject[0].address.toLowerCase()}` === address.toLowerCase());
}

function clearCache() {
  Object.keys(sessionStorage).forEach((key) => {
    // Remove association of (address <=> user/space)
    if (key.indexOf('exo-wallet-address-') === 0) {
      sessionStorage.removeItem(key);
    }
  });
}

function retrieveFiatExchangeRateOnline(currency) {
  // Retrieve Fiat <=> Ether exchange rate
  return fetch(`https://api.coinmarketcap.com/v1/ticker/ethereum/?convert=${currency}`, {
    referrerPolicy: 'no-referrer',
    headers: {
      Origin: '',
    },
  })
    .then((resp) => {
      if (resp && resp.ok) {
        return resp.json();
      }
    })
    .catch((error) => {
      console.debug('error retrieving currency exchange, trying to get exchange from local store', error);
    });
}
