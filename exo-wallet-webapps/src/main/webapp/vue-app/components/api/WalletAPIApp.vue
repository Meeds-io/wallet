<template>
  <v-app id="WalletAPIApp" class="hidden" />
</template>

<script>
import * as constants from '../../js/Constants.js';
import {saveTransactionDetails} from '../../js/TransactionUtils.js';
import {retrieveContractDetails, sendContractTransaction} from '../../js/TokenUtils.js';
import {initWeb3, initSettings, watchMetamaskAccount, convertTokenAmountToSend, truncateError, lockBrowserWallet, unlockBrowserWallet, hashCode} from '../../js/WalletUtils.js';
import {searchAddress} from '../../js/AddressRegistry.js';

export default {
  data() {
    return {
      isWalletEnabled: false,
      isWalletInitialized: false,
      loading: true,
      networkId: false,
      useMetamask: false,
      needPassword: false,
      isReadOnly: true,
      walletAddress: null,
      principalContractDetails: null,
      error: null,
    };
  },
  created() {
    if ((!eXo && eXo.env) || !eXo.env.portal || !eXo.env.portal.userName || !eXo.env.portal.userName.length) {
      this.isWalletEnabled = false;
      return;
    }
    if (eXo.env.portal.profileOwner && eXo.env.portal.profileOwner !== eXo.env.portal.userName) {
      this.isWalletEnabled = false;
      return;
    }

    document.addEventListener('exo-wallet-init', this.init);
    document.addEventListener('exo-wallet-metamask-changed', this.metamaskAccountChanged);
    document.addEventListener('exo-wallet-send-tokens', this.sendTokens);

    window.walletAddonInstalled = true;

    document.dispatchEvent(new CustomEvent('exo-wallet-installed'));
  },
  methods: {
    metamaskAccountChanged(event) {
      if(this.isWalletInitialized) {
        this.init(event);
      }
    },
    init(event) {
      this.isWalletInitialized = true;
      document.dispatchEvent(new CustomEvent('exo-wallet-init-loading'));
      try {
        this.loading = true;
        this.error = null;
        this.needPassword = false;
        this.principalContractDetails = null;
        this.walletAddress = null;

        console.debug("Wallet API application start loading");

        const settings = event && event.detail;
        let isSpace = false;
        if(settings && settings.sender) {
          if(settings.sender.type === 'space') {
            isSpace = true;
            window.walletSpaceGroup = settings.sender.id;
          }
        }

        return initSettings(isSpace)
          .then((result, error) => {
            this.handleError(error);
            if (!window.walletSettings || !window.walletSettings.isWalletEnabled) {
              this.isWalletEnabled = false;
              this.isReadOnly = true;
              throw new Error('Wallet disabled for current user');
            } else if (!window.walletSettings.userPreferences.walletAddress) {
              this.isReadOnly = true;
              throw new Error(constants.ERROR_WALLET_NOT_CONFIGURED);
            } else if (!window.walletSettings.defaultPrincipalAccount) {
              this.isReadOnly = true;
              throw new Error("Wallet principal account isn't configured");
            }
            this.isWalletEnabled = true;
            this.isReadOnly = false;
            return initWeb3();
          })
          .then((result, error) => {
            this.handleError(error);
            this.useMetamask = window.walletSettings.userPreferences.useMetamask;
            this.walletAddress = window.walletSettings.userPreferences.walletAddress;

            if(!this.walletAddress) {
              this.isReadOnly = true;
              throw new Error("No wallet is configured for current user");
            } else if ((!this.useMetamask && !window.walletSettings.browserWalletExists)
                || (this.useMetamask
                    && (!window.web3
                        || !window.web3.eth
                        || !window.web3.eth.defaultAccount
                        || window.web3.eth.defaultAccount.toLowerCase() !== this.walletAddress.toLowerCase()))) {
              this.isReadOnly = true;
              throw new Error("Wallet is in readonly state");
            }
            this.needPassword = !this.useMetamask && window.walletSettings.browserWalletExists && !window.walletSettings.storedPassword;
            this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
            this.networkId = window.walletSettings.currentNetworkId;
            this.isReadOnly = window.walletSettings.isReadOnly;
            if (window.walletSettings.maxGasPrice) {
              window.walletSettings.maxGasPriceEther = window.walletSettings.maxGasPriceEther || window.localWeb3.utils.fromWei(String(window.walletSettings.maxGasPrice), 'ether').toString();
            }
            this.principalContractDetails = {
              networkId: this.networkId,
              address: window.walletSettings.defaultPrincipalAccount,
              isContract: true,
              isDefault: true,
            };
            return retrieveContractDetails(this.walletAddress, this.principalContractDetails);
          })
          .then((result, error) => {
            this.handleError(error);

            if(!this.principalContractDetails || !this.principalContractDetails.address || this.principalContractDetails.address.indexOf('0x') !== 0) {
              console.debug('Principal token seems inconsistent', this.principalContractDetails);
              this.isReadOnly = true;
              throw new Error(`Default token isn't configured`);
            } else if(this.principalContractDetails.error) {
              console.debug('Error retrieving principal contract details', this.principalContractDetails.error, this.principalContractDetails);
              this.isReadOnly = true;
              throw new Error(this.principalContractDetails.error);
            } else if(!this.principalContractDetails.contract) {
              console.debug('Principal account in wallet isn\'t a token contract', this.principalContractDetails);
              this.isReadOnly = true;
              throw new Error('Principal account in wallet isn\'t a token contract');
            } else if(!this.principalContractDetails.contract.options || !this.principalContractDetails.contract.options.from) {
              console.debug('Error retrieving sender address', this.principalContractDetails);
              this.isReadOnly = true;
              throw new Error('Error retrieving your wallet address');
            }
          })
          .catch((e) => {
            console.debug('init method - error', e);
            const error = `${e}`;

            if (error.indexOf(constants.ERROR_WALLET_NOT_CONFIGURED) >= 0) {
              this.error = 'Wallet not configured';
            } else if (error.indexOf(constants.ERROR_WALLET_SETTINGS_NOT_LOADED) >= 0) {
              this.error = 'Failed to load user settings';
            } else if (error.indexOf(constants.ERROR_WALLET_DISCONNECTED) >= 0) {
              this.error = 'Failed to connect to network';
            } else {
              this.error = (e && e.message) || e;
            }
          })
          .finally(() => {
            console.debug("Wallet API application finished loading");
            this.loading = false;
            const result = {
              error : this.error,
              needPassword : this.needPassword,
              symbol : this.principalContractDetails && this.principalContractDetails.symbol,
            };
            document.dispatchEvent(new CustomEvent('exo-wallet-init-result', {detail : result}));

            if (this.useMetamask && window.walletSettings.enablingMetamaskAccountDone) {
              this.$nextTick(() => watchMetamaskAccount(window.walletSettings.detectedMetamaskAccount));
            }
          });
      } catch(e) {
        console.debug('init method - error', e);
        this.loading = false;
        document.dispatchEvent(new CustomEvent('exo-wallet-init-result', {detail : {
          error : (e && e.message) || e,
          symbol : this.principalContractDetails && this.principalContractDetails.symbol,
        }}));
      }
    },
    handleError(error) {
      if(error) {
        throw error;
      }
    },
    sendTokens(event) {
      try {
        if(!this.isWalletEnabled || this.isReadOnly) {
          throw new Error(`Your wallet is't accessible`);
        } else if(!this.principalContractDetails || !this.principalContractDetails.address || this.principalContractDetails.address.indexOf('0x') !== 0) {
          console.debug('Principal token seems inconsistent', this.principalContractDetails);
          throw new Error(`Default token isn't configured`);
        } else if(this.principalContractDetails.error || !this.principalContractDetails.contract || !this.principalContractDetails.contract.options || !this.principalContractDetails.contract.options.from) {
          console.debug('Principal token seems inconsistent', this.principalContractDetails);
          throw new Error(`Error retrieving default token`);
        }

        if(this.error) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : this.error
          }));
          return;
        }
  
        const sendTokensRequest = event && event.detail;
        if (!sendTokensRequest) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : 'Empty payment request'
          }));
          return;
        }
  
        const amount = sendTokensRequest.amount;
        const password = sendTokensRequest.password;
        const receiver = sendTokensRequest.receiver;
        const sender = sendTokensRequest.sender;
        const label = sendTokensRequest.label;
        const message = sendTokensRequest.message;
  
        if (!amount || Number.isNaN(parseFloat(amount)) || !Number.isFinite(amount) || amount <= 0) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : 'Invalid payment amount'
          }));
          return;
        }
  
        if (!receiver || !receiver.type || !receiver.id) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : 'Empty payment receiver'
          }));
          return;
        }
  
        if (!this.storedPassword && (!password || !password.length)) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : 'Empty password'
          }));
          return;
        }

        try {
          const unlocked = this.useMetamask || unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : hashCode(password));
          if (!unlocked) {
            document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
              detail : 'Wrong password'
            }));
            return;
          }
        } catch(e) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : 'Wrong password'
          }));
          return;
        }
  
        if (!this.principalContractDetails.balance || this.principalContractDetails.balance < amount) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : 'Unsufficient funds'
          }));
          return;
        }
  
        const gasPrice = window.walletSettings.minGasPrice;
        const defaultGas = window.walletSettings.userPreferences.defaultGas;
        const transfer = this.principalContractDetails.contract.methods.transfer;
        const isApprovedAccount = this.principalContractDetails.contract.methods.isApprovedAccount;
        const contractAddress = this.principalContractDetails.address;
        const contractType = this.principalContractDetails.contractType;
        const amountWithDecimals = convertTokenAmountToSend(amount, this.principalContractDetails.decimals);

        let approvedSender = false;
        let approvedReceiver = false;
        let receiverAddress = null;
        let senderAddress = null;

        return searchAddress(receiver.id, receiver.type)
          .then((address) => {
            receiverAddress = address;
            if (!receiverAddress || !receiverAddress.length) {
              throw new Error(`Receiver doesn't have a wallet`);
            }

            return searchAddress(sender.id, sender.type)
              .then((address) => {
                senderAddress = address;

                if (!senderAddress || !senderAddress.length) {
                  throw new Error(`The sender wallet doesn't have an address`);
                }

                if(senderAddress.toLowerCase() !== this.walletAddress.toLowerCase()) {
                  throw new Error(`The sender wallet isn't coherent with currently used wallet, please refresh the page.`);
                }

                if(senderAddress.toLowerCase() === receiverAddress.toLowerCase()) {
                  throw new Error(`You can't send tokens to the same address.`);
                }
              });
          })
          .then(() => {
            return Promise.resolve(null)
            // check approved account on ERT Token contract type only
            .then(() => (contractType && isApprovedAccount(senderAddress).call()) || true)
            .then((approved) => {
              approvedSender = approved;
              if(!approved) {
                throw new Error('Your wallet is not approved by administrator');
              }
            })
            // check approved account on ERT Token contract type only
            .then(() => (contractType && isApprovedAccount(receiverAddress).call()) || true)
            .then((approved) => {
              approvedReceiver = approved;
              if(!approved) {
                throw new Error('Receiver wallet is not approved by administrator');
              }
            })
            .then(() => transfer(receiverAddress, amountWithDecimals).estimateGas({
              from: senderAddress,
              gas: defaultGas,
              gasPrice: gasPrice,
            }))
            .catch((e) => {
              if(approvedReceiver && approvedSender) {
                console.debug('Error estimating transaction fee', {
                  from: senderAddress,
                  to: receiverAddress,
                  gas: defaultGas,
                  gasPrice: gasPrice,
                  balance: this.principalContractDetails.balance,
                  amount: amount,
                }, e);

                document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
                  detail : 'Error simulating transaction, please contact your administrator'
                }));
              } else {
                document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
                  detail : (e && e.message) || e,
                }));
              }
              return;
            })
            .then((result) => {
              if (!result) {
                // Transaction estimation seems to have an error
                return;
              }
              if (result > defaultGas) {
                document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
                  detail : 'Payment transaction needs more fee, please contact your administrator'
                }));
                return;
              }

              //finally paas this data parameter to send Transaction
              return sendContractTransaction(this.useMetamask, this.networkId, {
                   contractAddress: contractAddress,
                   senderAddress: senderAddress,
                   gas: defaultGas,
                   gasPrice: gasPrice,
                   method: transfer,
                   parameters: [receiverAddress, amountWithDecimals],
                  },
                  (hash) => {
                    const pendingTransaction = {
                      hash: hash,
                      from: senderAddress.toLowerCase(),
                      to: receiverAddress.toLowerCase(),
                      value: 0,
                      gas: defaultGas,
                      gasPrice: gasPrice,
                      pending: true,
                      contractAddress: contractAddress,
                      contractMethodName: 'transfer',
                      contractAmount: amount,
                      label: label,
                      message: message,
                      timestamp: Date.now()
                    };

                    // *async* save transaction message for contract, sender and receiver
                    saveTransactionDetails(pendingTransaction);

                    // The transaction has been hashed and will be sent
                    document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-pending', {
                      detail : pendingTransaction
                    }));
                  },
                  null,
                  null,
                  (error, receipt) => {
                    console.debug('contract transfer method - error', error, receipt);
                    document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
                      detail : `Payment transaction error: ${error}`
                    }));
                  });
            })
            .catch((e) => {
              console.debug('contract transfer method - error', e);
              this.loading = false;
              throw new Error(`Error sending tokens: ${truncateError(e)}`);
            })
            .finally(() => this.useMetamask || lockBrowserWallet());
          })
          .catch((error) => {
            console.debug('sendTokens method - error', error);
            document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
              detail : (error && error.message) || error,
            }));
          });
      } catch(e) {
        document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
          detail : (e && e.message) || e,
        }));
      }
    },
  },
};
</script>
