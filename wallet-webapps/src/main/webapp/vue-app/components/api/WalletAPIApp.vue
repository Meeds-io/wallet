<template>
  <v-app id="WalletAPIApp" class="hidden VuetifyApp" />
</template>

<script>
export default {
  data() {
    return {
      isWalletEnabled: false,
      isWalletInitialized: false,
      loading: true,
      needPassword: false,
      isReadOnly: true,
      wallet: null,
      walletAddress: null,
      principalContractDetails: null,
      error: null,
      settings: null,
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
    document.addEventListener('exo-wallet-send-tokens', this.sendTokens);

    window.walletAPIInitialized = true;

    this.init();

    document.dispatchEvent(new CustomEvent('exo-wallet-installed'));
  },
  methods: {
    init(event) {
      this.isWalletInitialized = true;
      document.dispatchEvent(new CustomEvent('exo-wallet-init-loading'));
      try {
        this.loading = true;
        this.error = null;
        this.needPassword = false;
        this.principalContractDetails = null;
        this.walletAddress = null;
        this.wallet = null;

        console.debug("Wallet API application start loading");

        const settings = event && event.detail;
        let isSpace = false;
        if(settings && settings.sender) {
          if(settings.sender.type === 'space') {
            isSpace = true;
            window.walletSpaceGroup = settings.sender.id;
          } else if (settings.sender.id !== eXo.env.portal.userName) {
            throw new Error(this.$t('exoplatform.wallet.warning.walletInitializationFailure'));
          }
        }

        return this.walletUtils.initSettings(isSpace)
          .then((result, error) => {
            this.handleError(error);
            this.settings = window.walletSettings || {};

            document.dispatchEvent(new CustomEvent('exo-wallet-settings-loaded', {detail : this.settings}));

            if (!this.settings.walletEnabled) {
              this.isWalletEnabled = false;
              this.isReadOnly = true;
              throw new Error(this.$t('exoplatform.wallet.warning.walletDisabled'));
            } else if (!this.settings.wallet || !this.settings.wallet.address) {
              this.isReadOnly = true;
              throw new Error(this.constants.ERROR_WALLET_NOT_CONFIGURED);
            } else if (!this.settings.contractAddress) {
              this.isReadOnly = true;
              throw new Error(this.$t('exoplatform.wallet.warning.noConfiguredToken'));
            }
            this.isWalletEnabled = true;
            this.isReadOnly = false;
            return this.walletUtils.initWeb3();
          })
          .then((result, error) => {
            this.handleError(error);
            this.walletAddress = this.settings.wallet.address;
            this.wallet = this.settings.wallet;

            if(!this.walletAddress) {
              this.isReadOnly = true;
              throw new Error(this.$t('exoplatform.wallet.warning.walletNotConfigured'));
            } else if (!this.settings.browserWalletExists) {
              this.isReadOnly = true;
              throw new Error(this.$t('exoplatform.wallet.warning.walletReadonly'));
            }
            this.needPassword = this.settings.browserWalletExists && !this.settings.storedPassword;
            this.storedPassword = this.settings.storedPassword && this.settings.browserWalletExists;
            this.isReadOnly = this.settings.isReadOnly;
            this.principalContractDetails = this.tokenUtils.getContractDetails(this.walletAddress);

            if(!this.principalContractDetails || !this.principalContractDetails.address || this.principalContractDetails.address.indexOf('0x') !== 0) {
              console.debug('Principal token seems inconsistent', this.principalContractDetails);
              this.isReadOnly = true;
              throw new Error(this.$t('exoplatform.wallet.warning.noConfiguredToken'));
            } else if(this.principalContractDetails.error) {
              console.debug('Error retrieving principal contract details', this.principalContractDetails.error, this.principalContractDetails);
              this.isReadOnly = true;
              throw new Error(this.$t('exoplatform.wallet.warning.networkConnectionFailure'));
            } else if(!this.principalContractDetails.contract) {
              console.debug('Principal account in wallet isn\'t a token contract', this.principalContractDetails);
              this.isReadOnly = true;
              throw new Error(this.$t('exoplatform.wallet.warning.noConfiguredToken'));
            } else if(!this.principalContractDetails.contract.options || !this.principalContractDetails.contract.options.from) {
              console.debug('Error retrieving sender address', this.principalContractDetails);
              this.isReadOnly = true;
              throw new Error(this.$t('exoplatform.wallet.warning.walletInitializationFailure'));
            }
          })
          .catch((e) => {
            const error = `${e}`;

            if (error.indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) >= 0) {
              this.error = this.$t('exoplatform.wallet.warning.walletNotConfigured');
            } else if (error.indexOf(this.constants.ERROR_WALLET_SETTINGS_NOT_LOADED) >= 0) {
              this.error = this.$t('exoplatform.wallet.warning.walletInitializationFailure');
            } else if (error.indexOf(this.constants.ERROR_WALLET_DISCONNECTED) >= 0) {
              this.error = this.$t('exoplatform.wallet.warning.networkConnectionFailure');
            } else {
              console.debug('init method - error', e);
              this.error = (e && e.message) || e;
            }
          })
          .finally(() => {
            console.debug("Wallet API application finished loading");
            this.loading = false;
            const result = {
              error : this.error,
              needPassword : this.needPassword,
              enabled : this.settings && this.settings.enabled,
              symbol : this.principalContractDetails && this.principalContractDetails.symbol,
            };
            document.dispatchEvent(new CustomEvent('exo-wallet-init-result', {detail : result}));
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
          throw new Error(this.$t('exoplatform.wallet.warning.walletReadonly'));
        } else if(!this.principalContractDetails || !this.principalContractDetails.address || this.principalContractDetails.address.indexOf('0x') !== 0) {
          console.debug('Principal token seems inconsistent', this.principalContractDetails);
          throw new Error(this.$t('exoplatform.wallet.warning.noConfiguredToken'));
        } else if(this.principalContractDetails.error || !this.principalContractDetails.contract || !this.principalContractDetails.contract.options || !this.principalContractDetails.contract.options.from) {
          console.debug('Principal token seems inconsistent', this.principalContractDetails);
          throw new Error(this.$t('exoplatform.wallet.warning.networkConnectionFailure'));
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
            detail : this.$t('exoplatform.wallet.warning.emptyPaymentRequest')
          }));
          return;
        }
  
        const amount = Number(sendTokensRequest.amount);
        const password = sendTokensRequest.password;
        const receiver = sendTokensRequest.receiver;
        const sender = sendTokensRequest.sender;
        const label = sendTokensRequest.label;
        const message = sendTokensRequest.message;

        const network = this.settings.network;

        network.minGasPriceEther = network.minGasPriceEther || window.localWeb3.utils.fromWei(String(network.minGasPrice * Number(network.gasLimit)), 'ether').toString();
        network.normalGasPriceEther = network.normalGasPriceEther || window.localWeb3.utils.fromWei(String(network.normalGasPrice * Number(network.gasLimit)), 'ether').toString();
        network.maxGasPriceEther = network.maxGasPriceEther || window.localWeb3.utils.fromWei(String(network.maxGasPrice * Number(network.gasLimit)), 'ether').toString();

        let gasPrice = sendTokensRequest.gasPrice || network.normalGasPrice;
        const gasPriceEther = window.localWeb3.utils.fromWei(String(Number(gasPrice) * Number(network.gasLimit)), 'ether').toString();

        if (this.wallet.etherBalance < gasPriceEther) {
          console.debug(`User can't be charged for transaction fees using parametered gas price. Thus normal gas price will be used`);
          if (this.wallet.etherBalance < network.normalGasPriceEther) {
            console.debug(`User can't be charged for transaction fees using normal gas price. Thus min gas price will be used`);
            if (this.wallet.etherBalance < network.minGasPriceEther) {
              console.debug(`User can't be charged for transaction fees using minimal gas price. Thus return an error`);
              document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
                detail : this.$t('exoplatform.wallet.warning.noEnoughFunds')
              }));
              return;
            }
            gasPrice = network.minGasPrice;
          }
        }

        if (!amount || Number.isNaN(parseFloat(amount)) || !Number.isFinite(amount) || amount <= 0) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : this.$t('exoplatform.wallet.warning.invalidPaymentAmount')
          }));
          return;
        }

        if (!receiver || !receiver.type || !receiver.id) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : this.$t('exoplatform.wallet.warning.emptyPaymentAmount')
          }));
          return;
        }
  
        if (!this.storedPassword && (!password || !password.length)) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : this.$t('exoplatform.wallet.warning.emptyPassword')
          }));
          return;
        }

        try {
          const unlocked = this.walletUtils.unlockBrowserWallet(this.storedPassword ? this.settings.userP : this.walletUtils.hashCode(password));
          if (!unlocked) {
            document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
              detail : this.$t('exoplatform.wallet.warning.wrongPassword')
            }));
            return;
          }
        } catch(e) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : this.$t('exoplatform.wallet.warning.wrongPassword')
          }));
          return;
        }
  
        if (!this.wallet.tokenBalance || this.wallet.tokenBalance < amount) {
          document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
            detail : this.$t('exoplatform.wallet.warning.unsufficientFunds')
          }));
          return;
        }
  
        const defaultGas = network.gasLimit;
        const transfer = this.principalContractDetails.contract.methods.transfer;
        const contractAddress = this.principalContractDetails.address;
        const amountWithDecimals = this.walletUtils.convertTokenAmountToSend(amount, this.principalContractDetails.decimals);

        let approvedSender = false;
        let approvedReceiver = false;
        let receiverAddress = null;
        let senderAddress = null;
        let receiverWallet = null;
        let senderWallet = null;

        return this.addressRegistry.searchWalletByTypeAndId(receiver.id, receiver.type)
          .then((wallet) => {
            receiverWallet = wallet;
            approvedReceiver = receiverWallet && receiverWallet.isApproved;
            if(!approvedReceiver) {
              throw new Error(this.$t('exoplatform.wallet.warning.receiverNotApprovedByAdministrator'));
            }

            receiverAddress = receiverWallet && receiverWallet.address;
            if (!receiverAddress || !receiverAddress.length) {
              throw new Error(this.$t('exoplatform.wallet.warning.receiverDoesntHaveWallet'));
            }
          })
          .then(() => this.addressRegistry.searchWalletByTypeAndId(sender.id, sender.type))
          .then((wallet) => {
            senderWallet = wallet;
            approvedSender = senderWallet && senderWallet.isApproved;
            if(!approvedSender) {
              throw new Error(this.$t('exoplatform.wallet.warning.senderNotApprovedByAdministrator'));
            }

            senderAddress = senderWallet && senderWallet.address;

            if (!senderAddress || !senderAddress.length) {
              throw new Error(this.$t('exoplatform.wallet.warning.senderDoesntHaveWallet'));
            }

            if(senderAddress.toLowerCase() !== this.walletAddress.toLowerCase()) {
              throw new Error(this.$t('exoplatform.wallet.warning.incoherentSenderWallet'));
            }

            if(senderAddress.toLowerCase() === receiverAddress.toLowerCase()) {
              throw new Error(this.$t('exoplatform.wallet.warning.receiverMustBeDifferentFromSender'));
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
                balance: this.wallet.tokenBalance,
                amount: amount,
              }, e);

              document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-error', {
                detail : this.$t('exoplatform.wallet.warning.errorSimulatingTransaction')
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
                detail : this.$t('exoplatform.wallet.warning.lowPaymentTransactionFeeError')
              }));
              return;
            }

            const transactionDetail = {
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

            return this.tokenUtils.sendContractTransaction(transactionDetail, transfer, [receiverAddress, amountWithDecimals]);
          })
          .then((savedTransaction) => {
            if (!savedTransaction) {
              return;
            }
            // The transaction has been hashed and is marked as pending in internal database
            document.dispatchEvent(new CustomEvent('exo-wallet-send-tokens-pending', {
              detail : savedTransaction
            }));
            this.walletUtils.lockBrowserWallet();
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
