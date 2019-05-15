<template>
  <v-card id="sendTokenForm">
    <v-card-text class="pt-0">
      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div> <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
        <i class="uiIconWarning"></i>{{ warning }}
      </div> <div v-if="!error && !warning && information && information.length" class="alert alert-info v-content">
        <i class="uiIconInfo"></i>{{ information }}
      </div>
      <v-form
        ref="form"
        @submit="
          $event.preventDefault();
          $event.stopPropagation();
        ">
        <address-auto-complete
          ref="autocomplete"
          :disabled="loading"
          input-label="Recipient"
          input-placeholder="Select a user, a space or an address to send to"
          autofocus
          required
          @item-selected="
            recipient = $event.address;
            $emit('receiver-selected', $event);
          " />

        <v-container
          flat
          fluid
          grid-list-lg
          class="mt-4 pl-2">
          <v-layout row wrap>
            <v-text-field
              v-model.number="amount"
              :disabled="loading"
              name="amount"
              label="Amount"
              required
              placeholder="Select an amount of tokens to send"
              @input="$emit('amount-selected', amount)" />
            <slot></slot>
          </v-layout>
        </v-container>

        <v-text-field
          v-if="!storedPassword"
          v-model="walletPassword"
          :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
          :type="walletPasswordShow ? 'text' : 'password'"
          :disabled="loading"
          :rules="mandatoryRule"
          name="walletPassword"
          label="Wallet password"
          placeholder="Enter your wallet password"
          counter
          required
          autocomplete="current-passord"
          class="mb-2"
          @click:append="walletPasswordShow = !walletPasswordShow" />
        <gas-price-choice :estimated-fee="transactionFeeString" @changed="gasPrice = $event" />
        <v-text-field
          v-model="transactionLabel"
          :disabled="loading"
          type="text"
          name="transactionLabel"
          label="Label (Optional)"
          placeholder="Enter label for your transaction" />
        <v-textarea
          v-model="transactionMessage"
          :disabled="loading"
          name="tokenTransactionMessage"
          label="Message (Optional)"
          placeholder="Enter a custom message to send to the receiver with your transaction"
          class="mt-4"
          rows="3"
          flat
          no-resize />
      </v-form>
      <qr-code-modal
        ref="qrCodeModal"
        :to="recipient"
        :from="account"
        :amount="0"
        :is-contract="true"
        :args-names="['_to', '_value']"
        :args-types="['address', 'uint256']"
        :args-values="[recipient, amount]"
        :open="showQRCodeModal"
        :function-payable="false"
        function-name="transfer"
        title="Send Tokens QR Code"
        information="You can scan this QR code by using a different application that supports QR code transaction generation to send tokens"
        @close="showQRCodeModal = false" />
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <button
        :disabled="loading || !recipient || !amount || !canSendToken"
        :loading="loading"
        class="btn btn-primary mr-1"
        @click="sendTokens">
        Send
      </button> <button
        :disabled="loading || !recipient || !amount || !canSendToken"
        class="btn"
        color="secondary"
        @click="showQRCodeModal = true">
        QRCode
      </button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import AddressAutoComplete from './AddressAutoComplete.vue';
import QrCodeModal from './QRCodeModal.vue';
import GasPriceChoice from './GasPriceChoice.vue';

import {unlockBrowserWallet, lockBrowserWallet, truncateError, hashCode, convertTokenAmountToSend, etherToFiat, saveWalletInitializationStatus} from '../js/WalletUtils.js';
import {saveTransactionDetails} from '../js/TransactionUtils.js';
import {retrieveContractDetails, sendContractTransaction} from '../js/TokenUtils.js';

export default {
  components: {
    QrCodeModal,
    GasPriceChoice,
    AddressAutoComplete,
  },
  props: {
    account: {
      type: String,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
    defaultLabel: {
      type: String,
      default: function() {
        return null;
      },
    },
    defaultMessage: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      loading: false,
      showQRCodeModal: false,
      storedPassword: false,
      transactionLabel: '',
      transactionMessage: '',
      walletPassword: '',
      walletPasswordShow: false,
      useMetamask: false,
      recipient: null,
      isApprovedRecipient: true,
      canSendToken: true,
      amount: null,
      gasPrice: 0,
      estimatedGas: 0,
      fiatSymbol: null,
      warning: null,
      information: null,
      error: null,
      mandatoryRule: [(v) => !!v || 'Field is required'],
    };
  },
  computed: {
    transactionFeeString() {
      if (this.transactionFeeToken) {
        if (this.contractDetails) {
          return `${this.toFixed(this.transactionFeeToken)} ${this.contractDetails && this.contractDetails.symbol}`;
        } else {
          return '';
        }
      } else if (this.transactionFeeFiat) {
        return `${this.toFixed(this.transactionFeeFiat)} ${this.fiatSymbol}`;
      }
      return '';
    },
    sellPriceInWei() {
      return this.contractDetails && this.contractDetails.sellPrice ? window.localWeb3.utils.toWei(String(this.contractDetails.sellPrice), 'ether') : 0;
    },
    transactionFeeInWei() {
      return this.estimatedGas && this.gasPrice ? parseInt(this.estimatedGas * this.gasPrice) : 0;
    },
    transactionFeeEther() {
      return this.transactionFeeInWei ? window.localWeb3.utils.fromWei(String(this.transactionFeeInWei), 'ether') : 0;
    },
    transactionFeeFiat() {
      return this.transactionFeeEther ? etherToFiat(this.transactionFeeEther) : 0;
    },
    transactionFeeToken() {
      return !this.contractDetails || this.contractDetails.isOwner || !this.transactionFeeInWei || !this.sellPriceInWei ? 0 : this.toFixed(this.transactionFeeInWei / this.sellPriceInWei);
    },
  },
  watch: {
    contractDetails() {
      if (this.contractDetails && this.contractDetails.isPaused) {
        this.warning = `Contract '${this.contractDetails.name}' is paused, thus you will be unable to send tokens`;
      }
    },
    amount() {
      this.checkErrors();
    },
    recipient(newValue, oldValue) {
      if (oldValue !== newValue) {
        this.error = null;
        this.warning = null;
        this.information = null;

        this.isApprovedRecipient = true;
        this.canSendToken = true;
      }

      if (newValue && oldValue !== newValue) {
        this.checkErrors();
        if (this.error) {
          return;
        }

        // Admin will implicitly approve account, so not necessary
        // to check if the receiver is approved or not
        if (this.contractDetails.contractType > 0) {
          this.warning = null;
          this.information = null;

          return this.contractDetails.contract.methods
            .isApprovedAccount(this.recipient)
            .call()
            .then((isApproved) => {
              this.isApprovedRecipient = isApproved;
              if (this.contractDetails && this.contractDetails.isPaused) {
                this.warning = `Contract '${this.contractDetails.name}' is paused, thus you can't send tokens`;
                this.canSendToken = false;
              } else if (!this.isApprovedRecipient) {
                this.warning = `The recipient isn't approved to receive tokens`;
                this.canSendToken = false;
              } else {
                this.canSendToken = true;
              }

              // Async gas estimation if current address is owner of contract
              if (this.canSendToken && this.contractDetails && this.contractDetails.isOwner) {
                this.estimateTransactionFee();
              }
            });
        }
      }
    },
  },
  methods: {
    init() {
      this.$nextTick(() => {
        if (this.$refs.autocomplete) {
          this.$refs.autocomplete.clear();
          this.$refs.autocomplete.focus();
        }
      });
      this.loading = false;
      this.showQRCodeModal = false;
      this.recipient = null;
      this.amount = null;
      this.warning = null;
      this.error = null;
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.error = null;
      this.transactionLabel = this.defaultLabel;
      this.transactionMessage = this.defaultMessage;
      if (!this.gasPrice) {
        this.gasPrice = window.walletSettings.minGasPrice;
      }
      this.useMetamask = window.walletSettings.userPreferences.useMetamask;
      this.fiatSymbol = window.walletSettings.fiatSymbol;
      this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
      this.$nextTick(() => {
        if (this.contractDetails && this.contractDetails.isPaused) {
          this.warning = `Contract '${this.contractDetails.name}' is paused, thus you will be unable to send tokens`;
        } else {
          if (this.contractDetails && this.contractDetails.address && !this.contractDetails.hasOwnProperty('isApproved')) {
            // Load contract details in async mode for adminLevel test
            retrieveContractDetails(this.account, this.contractDetails, true);
          }
          this.estimateTransactionFee();
          this.warning = null;
        }
      });
    },
    estimateTransactionFee() {
      if (this.contractDetails && !this.contractDetails.isPaused && this.contractDetails.balance && this.contractDetails.sellPrice && this.contractDetails.owner && this.contractDetails.contractType) {
        const recipient = this.contractDetails.isOwner ? this.recipient : this.contractDetails.owner;

        if (recipient) {
          // Estimate gas
          this.contractDetails.contract.methods
            .transfer(recipient, String(Math.pow(10, this.contractDetails.decimals ? this.contractDetails.decimals : 0)))
            .estimateGas({
              from: this.contractDetails.contract.options.from,
              gas: window.walletSettings.userPreferences.defaultGas,
              gasPrice: this.gasPrice,
            })
            .then((estimatedGas) => {
              // Add 10% to ensure that the operation doesn't take more than the estimation
              this.estimatedGas = estimatedGas * 1.1;
            })
            .catch((e) => {
              console.debug('Error while estimating gas', e);
            });
        }
      }
    },
    sendTokens() {
      this.error = null;
      this.warning = null;

      const setWalletInitialized = !this.isApprovedRecipient && Number(this.contractDetails.adminLevel) > 0; 

      this.$refs.form.validate();
      if (this.contractDetails && this.contractDetails.isPaused) {
        this.warning = `Contract '${this.contractDetails.name}' is paused, thus you will be unable to send tokens`;
        return;
      }

      if (!window.localWeb3.utils.isAddress(this.recipient)) {
        this.error = 'Invalid recipient address';
        return;
      }

      if (!this.amount || isNaN(parseFloat(this.amount)) || !isFinite(this.amount) || this.amount <= 0) {
        this.error = 'Invalid amount';
        return;
      }

      if (!this.storedPassword && (!this.walletPassword || !this.walletPassword.length)) {
        this.error = 'Password field is mandatory';
        return;
      }

      const unlocked = this.useMetamask || unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = 'Wrong password';
        return;
      }

      if (this.contractDetails.balance < this.amount) {
        this.error = 'Unsufficient funds';
        return;
      }

      if (!this.canSendToken) {
        this.error = "Can't send token, please verify previous errors or contact your administrator";
        return;
      }

      this.error = null;

      this.loading = true;
      try {
      return this.contractDetails.contract.methods
          .transfer(this.recipient, convertTokenAmountToSend(this.amount, this.contractDetails.decimals).toString())
          .estimateGas({
            from: this.contractDetails.contract.options.from,
            gas: window.walletSettings.userPreferences.defaultGas,
            gasPrice: this.gasPrice,
          })
          .catch((e) => {
            console.error('Error estimating necessary gas', e);
            return 0;
          })
          .then((estimatedGas) => {
            if (estimatedGas > window.walletSettings.userPreferences.defaultGas) {
              this.warning = `You have set a low gas ${window.walletSettings.userPreferences.defaultGas} while the estimation of necessary gas is ${estimatedGas}. Please change it in your preferences.`;
              return;
            }
            const sender = this.contractDetails.contract.options.from;
            const receiver = this.recipient;
            const contractDetails = this.contractDetails;
            const transfer =  contractDetails.contract.methods.transfer;

            return sendContractTransaction(this.useMetamask, window.walletSettings.defaultNetworkId, {
              contractAddress: contractDetails.address,
              senderAddress: sender,
              gas: window.walletSettings.userPreferences.defaultGas,
              gasPrice: this.gasPrice,
              method: transfer,
              parameters: [receiver, convertTokenAmountToSend(this.amount, contractDetails.decimals).toString()],
            },
              (hash) => {
                const gas = window.walletSettings.userPreferences.defaultGas ? window.walletSettings.userPreferences.defaultGas : 35000;

                const pendingTransaction = {
                  hash: hash,
                  from: sender.toLowerCase(),
                  to: receiver,
                  value: 0,
                  gas: gas,
                  gasPrice: this.gasPrice,
                  pending: true,
                  contractAddress: contractDetails.address,
                  contractMethodName: 'transfer',
                  contractAmount: this.amount,
                  label: this.transactionLabel,
                  message: this.transactionMessage,
                  timestamp: Date.now(),
                  fee: this.transactionFeeEther,
                  feeFiat: this.transactionFeeFiat,
                  feeToken: this.transactionFeeToken,
                };

                // *async* save transaction message for contract, sender and receiver
                saveTransactionDetails(pendingTransaction)
                  .then(() => {
                    // The transaction has been hashed and will be sent
                    this.$emit(
                      'sent',
                      pendingTransaction,
                      this.contractDetails
                    );
                    this.$emit('close');
                  });

                if (setWalletInitialized) {
                  // *async* set wallet as initialized
                  saveWalletInitializationStatus(pendingTransaction.to, 'INITIALIZED');
                }
              },
              null,
              null,
              (error, receipt) => {
                console.debug('Web3 contract.transfer method - error', error);
                // The transaction has failed
                this.error = `Error sending tokens: ${truncateError(error)}`;
              });
          })
          .catch((e) => {
            console.debug('Web3 contract.transfer method - error', e);
            this.error = `Error sending tokens: ${truncateError(e)}`;
          })
          .finally(() => {
            this.loading = false;
            if (!this.useMetamask) {
              lockBrowserWallet();
            }
          });
      } catch (e) {
        console.debug('Web3 contract.transfer method - error', e);
        this.loading = false;
        this.error = `Error sending tokens: ${truncateError(e)}`;
      }
    },
    checkErrors() {
      this.error = null;

      if(!this.contractDetails) {
        return;
      }

      if (this.recipient === this.account && this.contractDetails.contractType > 0) {
        this.error = `You can't send '${this.contractDetails.name}' to yourself`;
        this.canSendToken = false;
        return;
      }

      if (this.amount && (isNaN(parseFloat(this.amount)) || !isFinite(this.amount) || this.amount <= 0)) {
        this.error = 'Invalid amount';
        return;
      } else if (this.amount && $.isNumeric(this.amount)) {
        this.error = (!this.contractDetails || this.contractDetails.balance >= this.amount) ? null : 'Unsufficient funds';
        return;
      }
    },
  },
};
</script>
