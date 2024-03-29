/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import * as constants from './Constants';
import {searchWalletByTypeAndId, saveNewAddress} from './AddressRegistry';
import {getSavedTransactionByHash} from './TransactionUtils';

const DEFAULT_DECIMALS = 3;

const POLYGON_METAMASK_NETWORKS = {
  137: {
    'chainName': 'Polygon Mainnet',
    'chainId': '0x89', // 137
    'nativeCurrency': {
      'decimals': 18,
      'name': 'MATIC',
      'symbol': 'MATIC'
    },
    'rpcUrls': [
      'https://polygon-rpc.com/',
      'https://rpc-mainnet.matic.network',
      'https://matic-mainnet.chainstacklabs.com',
      'https://rpc-mainnet.maticvigil.com',
      'https://rpc-mainnet.matic.quiknode.pro',
      'https://matic-mainnet-full-rpc.bwarelabs.com'
    ],
    'blockExplorerUrls': ['https://polygonscan.com']
  },
  80001: {
    'chainName': 'Mumbai',
    'chainId': '0x13881', // 80001
    'nativeCurrency': {
      'decimals': 18,
      'name': 'MATIC',
      'symbol': 'MATIC'
    },
    'rpcUrls': [
      'https://matic-mumbai.chainstacklabs.com',
      'https://rpc-mumbai.maticvigil.com',
      'https://matic-testnet-archive-rpc.bwarelabs.com'
    ],
    'blockExplorerUrls': ['https://mumbai.polygonscan.com']
  },
};

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

export function sendPrivateKeyToServer(walletAddress) {
  walletAddress = walletAddress || window.walletSettings.wallet.address;
  if (!walletAddress) {
    return Promise.reject(new Error('Can\'t find current wallet address'));
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
    return Promise.reject(new Error('Can\'t find current wallet address'));
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
    return Promise.reject(new Error('Can\'t find current wallet address'));
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

export function initSettings(isSpace, useCometd, isAdministration) {
  const spaceId = (isSpace && eXo.env.portal.spaceId) || '';
  isAdministration = isAdministration || false;

  clearCache();

  return fetch(`/portal/rest/wallet/api/settings?spaceId=${spaceId}&administration=${isAdministration}`, {credentials: 'include'})
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

        document.addEventListener('exo.wallet.transaction.minedAndUpdated', triggerTransactionMinedEvent);
        initCometd(window.walletSettings);
      }

      if (window.walletSettings?.wallet?.provider === 'METAMASK') {
        const connected = isMetamaskConnected();
        const installed = isMetamaskInstalled();
        window.walletSettings.metamask = {
          connected,
          installed,
        };
        if (installed) {
          if (connected) {
            getMetamaskSelectedAccount();
            getMetamaskSelectedNetworkId();
          }
          window.ethereum.on('connect', () => {
            window.walletSettings.metamask.connected = true;
            getMetamaskSelectedNetworkId()
              .finally(() => document.dispatchEvent(new CustomEvent('wallet-metamask-chainChanged', {detail: window.walletSettings.metamask.networkId})));
            getMetamaskSelectedAccount()
              .finally(() => document.dispatchEvent(new CustomEvent('wallet-metamask-accountsChanged', {detail: window.walletSettings.metamask.address})));
            document.dispatchEvent(new CustomEvent('wallet-metamask-connected'));
          });
          window.ethereum.on('disconnect', () => {
            window.walletSettings.metamask.connected = false;
            window.walletSettings.metamask.networkId = null;
            window.walletSettings.metamask.address = null;
            document.dispatchEvent(new CustomEvent('wallet-metamask-disconnected'));
          });
          window.ethereum.on('accountsChanged' , () => {
            getMetamaskSelectedAccount()
              .finally(() => document.dispatchEvent(new CustomEvent('wallet-metamask-accountsChanged', {detail: window.walletSettings.metamask.address})));
          });
          window.ethereum.on('chainChanged', () => {
            getMetamaskSelectedNetworkId()
              .finally(() => document.dispatchEvent(new CustomEvent('wallet-metamask-chainChanged', {detail: window.walletSettings.metamask.networkId})));
          });
        }
      }
    })
    .then(() => {
      if (window.walletSettings.userPreferences && window.walletSettings.userPreferences.hasKeyOnServerSide && window.walletSettings.wallet.address && window.walletSettings.wallet.provider === 'INTERNAL_WALLET') {
        return retrievePrivateKeyFromServer(window.walletSettings.wallet.address)
          .then((privateKey) => {
            if (!privateKey) {
              return;
            }
          })
          .catch((e) => {
            console.error('Error retrieving wallet private key from server', e);
          });
      }
    })
    .then(() => {
      if ( window.walletSettings.wallet.provider === 'INTERNAL_WALLET' ){
        window.walletSettings.browserWalletExists = browserWalletExists(window.walletSettings.wallet.address);
        const address = window.walletSettings.wallet.address;
        if (address) {
          window.walletSettings.userP = localStorage.getItem(`exo-wallet-${address}-userp`);
          window.walletSettings.storedPassword = window.walletSettings.userP && window.walletSettings.userP.length > 0;
        }
      } else {
        window.walletSettings.browserWalletExists = true;
      }
      const accountId = getRemoteId(isSpace);
      if (isSpace && accountId) {
        return initSpaceAccount(accountId);
      }
    })
    .catch((e) => {
      console.error('initSettings method - error', e);
      throw e;
    });
}

export function watchTransactionStatus(hash, transactionMinedcallback) {
  if (!transactionMinedcallback) {
    return;
  }
  if (!hash) {
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
  if (window.walletSettings && window.walletSettings.wallet && window.walletSettings.wallet.address && window.walletSettings.wallet.initializationState !== 'DELETED') {
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
      .then((phrase) => {
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
    throw new Error('Can\'t find address of user');
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

export function getTransactionExplorerName() {
  switch (window.walletSettings.network.id) {
  case 1:
    return 'Etherscan';
  case 3:
    return 'Ropsten Etherscan';
  case 5:
    return 'Goerli Etherscan';
  case 137:
    return 'Polygonscan';
  case 80001:
    return 'Mumbai Polygonscan';
  default:
    return '#';
  }
}

export function getAddressEtherscanlink() {
  switch (window.walletSettings.network.id) {
  case 1:
    return 'https://etherscan.io/address/';
  case 3:
    return 'https://ropsten.etherscan.io/address/';
  case 5:
    return 'https://goerli.etherscan.io/address/';
  case 137:
    return 'https://polygonscan.com/address/';
  case 80001:
    return 'https://mumbai.polygonscan.com/address/';
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
  case 5:
    return 'https://goerli.etherscan.io/token/';
  case 137:
    return 'https://polygonscan.com/token/';
  case 80001:
    return 'https://mumbai.polygonscan.com/token/';

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
  case 5:
    return 'https://goerli.etherscan.io/tx/';
  case 137:
    return 'https://polygonscan.com/tx/';
  case 80001:
    return 'https://mumbai.polygonscan.com/tx/';
  default:
    return '#';
  }
}

export function getNetworkLink() {
  switch (window.walletSettings.network.id) {
  case 1:
    return 'https://etherscan.io';
  case 3:
    return 'https://ropsten.etherscan.io';
  case 5:
    return 'https://goerli.etherscan.io';
  case 80001:
  case 137 :
    return 'https://polygon.technology';
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
    console.error('error while unlocking wallet', e);
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
  return fetch('/portal/rest/wallet/api/account/list', {credentials: 'include'}).then((resp) => {
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
  const gasFeeWei = LocalWeb3.utils.toBN(gas).mul(LocalWeb3.utils.toBN(gasPrice));
  return LocalWeb3.utils.fromWei(gasFeeWei.toString(), 'ether');
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
export function deleteWallet(address) {
  return fetch(`/portal/rest/wallet/api/account/deleteWallet?address=${address}`, {
    credentials: 'include',
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Error while deleting wallet');
    }
  });
}

export function getMetamaskSelectedAccount() {
  return window.ethereum?.request({
    method: 'eth_accounts'
  }).then(address => window.walletSettings.metamask.address = address && address.length && address[0] || null);
}

export function getMetamaskSelectedNetworkId() {
  return window.ethereum?.request({
    method: 'eth_chainId'
  }).then(networkId => window.walletSettings.metamask.networkId = Number(networkId));
}

export function isMetamaskInstalled() {
  return window.ethereum && window.ethereum.isMetaMask;
}

export function isMetamaskConnected() {
  return isMetamaskInstalled() && window.ethereum.isConnected();
}

export function connectToMetamask() {
  return window.ethereum.request({
    method: 'eth_requestAccounts'
  });
}

export function switchMetamaskAccount() {
  return window.ethereum?.request({
    method: 'wallet_requestPermissions',
    params: [{ eth_accounts: {} }],
  }) || Promise.reject(new Error('No Metamask installed'));
}

export function switchMetamaskNetwork() {
  const network = POLYGON_METAMASK_NETWORKS[window.walletSettings.network.id];
  const networkId = network?.chainId;
  if (!networkId) {
    return Promise.reject(new Error('Unexpected Network id'));
  }
  return window.ethereum && window.ethereum.request({
    method: 'wallet_switchEthereumChain',
    params: [{
      chainId: networkId,
    }],
  }).catch(switchError => {
    if (switchError.code === 4902) {
      return window.ethereum.request({
        method: 'wallet_addEthereumChain',
        params: [
          network,
        ],
      });
    } else {
      throw switchError;
    }
  }) || Promise.reject(new Error('No Metamask installed'));
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
  if (window.walletSettings && window.walletSettings.network && window.walletSettings.wallet && window.walletSettings.network.providerURL) {
    const provider = new LocalWeb3.providers.HttpProvider(window.walletSettings.network.providerURL);
    window.localWeb3 = new LocalWeb3(provider);
    if (window.walletSettings.wallet?.address) {
      window.localWeb3.eth.defaultAccount = window.walletSettings.wallet.address.toLowerCase();
    }

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
