import * as constants from './Constants';
import {searchWalletByTypeAndId, saveNewAddress} from './AddressRegistry';
import {getSavedTransactionByHash} from './TransactionUtils';

const DEFAULT_DECIMALS = 3;

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

  return fetch('/portal/rest/wallet/api/account/savePrivateKey', {
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

  const etherToFiatExchangeObject = localStorage.getItem(`exo-wallet-exchange-${currency}`);
  const etherToFiatExchangeLastCheckTime = localStorage.getItem(`exo-wallet-exchange-${currency}-time`);

  let promise = null;
  if (!etherToFiatExchangeObject || !etherToFiatExchangeLastCheckTime || Number(etherToFiatExchangeLastCheckTime) - Date.now() > 86400000) {
    promise = retrieveFiatExchangeRateOnline(currency);
  } else {
    promise = Promise.resolve(etherToFiatExchangeObject);
  }

  // Retrieve Fiat <=> Ether exchange rate
  return promise.then((content) => {
    if (etherToFiatExchangeObject && (!content || !content.length || !content[0][`price_${currency}`])) {
      // Try to get old information from local storage
      content = localStorage.getItem(`exo-wallet-exchange-${currency}`);
      if (content) {
        content = JSON.parse(content);
      }
    }

    window.walletSettings.usdPrice = content ? parseFloat(content[0].price_usd) : 0;
    window.walletSettings.fiatPrice = content ? parseFloat(content[0][`price_${currency}`]) : 0;
    window.walletSettings.priceLastUpdated = content ? new Date(parseInt(content[0].last_updated) * 1000) : null;
  });
}

export function initEmptyWeb3Instance() {
  window.localWeb3 = new LocalWeb3();
}

export function initWeb3(isSpace) {
  if (!window.walletSettings || !window.walletSettings.userPreferences) {
    // User settings aren't loaded
    throw new Error(constants.ERROR_WALLET_SETTINGS_NOT_LOADED);
  }

  return createLocalWeb3Instance(isSpace);
}

export function initSettings(isSpace, useCometd) {
  const spaceId = (isSpace && window.walletSpaceGroup) || '';

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
      if (!settings || !settings.walletEnabled) {
        window.walletSettings = {walletEnabled: false};
        return;
      } else {
        window.walletSettings = window.walletSettings || {};
      }

      window.walletSettings.userPreferences = {currency: 'usd'};
      window.walletSettings.wallet = {};
      window.walletSettings.network = {};
      window.walletSettings.contractDetail = {};

      window.walletSettings = $.extend(window.walletSettings, settings);
      window.walletSettings = $.extend(window.walletSettings, settings);
      if (useCometd && !window.walletComedDInitialized) {
        window.walletComedDInitialized = true;

        document.addEventListener('exo.wallet.transaction.modified', triggerTransactionMinedEvent);
        initCometd(window.walletSettings);
      }
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
      const address = window.walletSettings.wallet.address;
      if (address) {
        window.walletSettings.userP = localStorage.getItem(`exo-wallet-${address}-userp`);
        window.walletSettings.storedPassword = window.walletSettings.userP && window.walletSettings.userP.length > 0;
      }

      const accountId = getRemoteId(isSpace);
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

export function watchTransactionStatus(hash, transactionMinedcallback) {
  if (!transactionMinedcallback) {
    console.warn('no callback added to method');
    return;
  }
  if (!hash) {
    console.warn('empty hash added to method');
    return;
  }
  hash = hash.toLowerCase();

  if (!window.watchingTransactions) {
    window.watchingTransactions = {};
  }

  if (!window.watchingTransactions[hash]) {
    window.watchingTransactions[hash] = [transactionMinedcallback];
  } else {
    window.watchingTransactions[hash].push(transactionMinedcallback);
  }
}

export function saveBrowserWalletInstance(wallet, password, isSpace, rememberPasswordInBrowser, backedUp) {
  const account = window.localWeb3.eth.accounts.wallet.add(wallet);
  const address = account['address'].toLowerCase();

  let promise = null;
  if (window.walletSettings && window.walletSettings.wallet && window.walletSettings.wallet.address) {
    promise = Promise.resolve();
  } else {
    promise = saveNewAddress(isSpace ? window.walletSpaceGroup : eXo.env.portal.userName, isSpace ? 'space' : 'user', address, true)
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
      });
  }
  return promise.then(() => {
      saveBrowserWallet(password, null, address, rememberPasswordInBrowser);
      return sendPrivateKeyToServer(address, password);
    })
    .then(() => {
      if (backedUp) {
        setWalletBackedUp();
      }
    });
}

export function saveBrowserWallet(password, phrase, address, save) {
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

  rememberPassword(save, password, address);

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

export function getAddressEtherscanlink() {
  switch (window.walletSettings.network.id) {
    case 1:
      return 'https://etherscan.io/address/';
    case 3:
      return 'https://ropsten.etherscan.io/address/';
    default:
      return '#';
  }
}

export function getTokenEtherscanlink() {
  switch (window.walletSettings.network.id) {
    case 1:
      return 'https://etherscan.io/token/';
    case 3:
      return 'https://ropsten.etherscan.io/token/';
    default:
      return '#';
  }
}

export function getTransactionEtherscanlink() {
  switch (window.walletSettings.network.id) {
    case 1:
      return 'https://etherscan.io/tx/';
    case 3:
      return 'https://ropsten.etherscan.io/tx/';
    default:
      return '#';
  }
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

export function setWalletBackedUp() {
  return fetch('/portal/rest/wallet/api/account/saveBackupState', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: $.param({
      walletId: window.walletSettings.wallet.technicalId,
      backedUp: true,
    }),
  })
    .then((resp) => {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        throw new Error();
      }
    })
    .then((wallet) => {
      return (window.walletSettings.wallet = wallet);
    });
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

  if (String(error).indexOf('transaction underpriced') >= 0 || String(error).indexOf('nonce too low') >= 0|| String(error).indexOf('known transaction') >= 0) {
    error = 'Another transaction is in progress please wait until the first transaction is finished';
  }
  return error;
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
    decimals = DEFAULT_DECIMALS;
  }
  try {
    return Number.parseFloat(value).toFixed(decimals).replace(/(\..*[1-9])0+$/, '$1').replace(/\.0*$/, '');
  } catch (e) {
    console.error('Error parsing value ', value, ' same value will be retruned', e);
    return value;
  }
}

export function saveWalletInitializationStatus(address, status) {
  return fetch(`/portal/rest/wallet/api/account/setInitializationStatus?address=${address}&status=${status}`, {
    credentials: 'include',
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Error while changing initialization status of wallet');
    }
  });
}

function triggerTransactionMinedEvent(event) {
  return triggerTransactionMined(event && event.detail && event.detail.string);
}

function triggerTransactionMined(hash) {
  if (!window.watchingTransactions || !window.watchingTransactions[hash]) {
    return;
  }

  return getSavedTransactionByHash(hash).then((transactionDetails) => {
    window.watchingTransactions[hash].forEach((callback) => {
      callback(transactionDetails);
    });
    window.watchingTransactions[hash] = null;
  });
}

function isWalletUnlocked(address) {
  return window.localWeb3.eth.accounts.wallet.length > 0 && window.localWeb3.eth.accounts.wallet[address] && window.localWeb3.eth.accounts.wallet[address].privateKey;
}

function createLocalWeb3Instance(isSpace) {
  if (window.walletSettings && window.walletSettings.network && window.walletSettings.wallet && window.walletSettings.wallet.address && window.walletSettings.network.providerURL) {
    const provider = new LocalWeb3.providers.HttpProvider(window.walletSettings.network.providerURL);
    window.localWeb3 = new LocalWeb3(provider);
    window.localWeb3.eth.defaultAccount = window.walletSettings.wallet.address.toLowerCase();

    if (isSpace && !window.walletSettings.wallet.spaceAdministrator) {
      window.walletSettings.isReadOnly = true;
    } else {
      window.walletSettings.isReadOnly = !window.walletSettings.browserWalletExists;
    }
  } else {
    initEmptyWeb3Instance();
    // Wallet not configured
    throw new Error(constants.ERROR_WALLET_NOT_CONFIGURED);
  }
}

function initSpaceAccount(spaceGroup) {
  return searchWalletByTypeAndId(spaceGroup, 'space').then((spaceObject, error) => {
    if (error) {
      throw error;
    }
    if (spaceObject && spaceObject.spaceAdministrator) {
      return (window.walletSettings.wallet.spaceAdministrator = true);
    } else {
      window.walletSettings.isReadOnly = true;
      return (window.walletSettings.wallet.spaceAdministrator = false);
    }
  });
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
    .then((content) => {
      if (content && content.length && content[0][`price_${currency}`]) {
        localStorage.setItem(`exo-wallet-exchange-${currency}`, JSON.stringify(content));
        localStorage.setItem(`exo-wallet-exchange-${currency}-time`, Date.now());
      }
    })
    .catch((error) => {
      console.debug('error retrieving currency exchange, trying to get exchange from local store', error);
    });
}

function initCometd(settings) {
  const loc = window.location;
  cCometd.configure({
    url: `${loc.protocol}//${loc.hostname}${(loc.port && ':') || ''}${loc.port || ''}/${settings.cometdContext}/cometd`,
    exoId: eXo.env.portal.userName,
    exoToken: settings.cometdToken,
  });

  cCometd.subscribe(settings.cometdChannel, null, (event) => {
    const data = event.data && JSON.parse(event.data);

    document.dispatchEvent(new CustomEvent(data.eventId, {detail: data && data.message}));
  });
}
