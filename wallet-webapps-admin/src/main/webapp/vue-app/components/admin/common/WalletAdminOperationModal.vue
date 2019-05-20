<template>
  <v-dialog
    v-model="dialog"
    :disabled="disabled"
    attach="#walletDialogsParent"
    content-class="uiPopup with-overflow"
    width="500px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <v-bottom-nav
      v-if="!noButton"
      slot="activator"
      :value="true"
      color="white"
      class="elevation-0 buttomNavigation">
      <v-btn flat value="send">
        <span>
          {{ title }}
        </span>
        <v-icon>
          send
        </v-icon>
      </v-btn>
    </v-bottom-nav>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">
          {{ title }}
        </span>
      </div>

      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
        <i class="uiIconWarning"></i>{{ warning }}
      </div>

      <v-card flat>
        <v-card-text class="pt-0">
          <v-form
            @submit="
              $event.preventDefault();
              $event.stopPropagation();
            ">
            <address-auto-complete
              v-show="autocompleteLabel"
              ref="autocompleteInput"
              :disabled="loading"
              :input-label="autocompleteLabel"
              :input-placeholder="autocompletePlaceholder"
              class="mt-3"
              :autofocus="dialog && autocompleteLabel"
              @item-selected="autocompleteValue = $event.address" />

            <v-text-field
              v-if="dialog"
              v-show="inputLabel"
              v-model="inputValue"
              :disabled="loading"
              :label="inputLabel"
              :placeholder="inputPlaceholder"
              :autofocus="inputLabel && !autocompleteLabel"
              class="mt-3"
              name="inputValue" />

            <v-text-field
              v-if="dialog && !storedPassword"
              v-model="walletPassword"
              :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
              :type="walletPasswordShow ? 'text' : 'password'"
              :disabled="loading"
              :autofocus="!inputLabel && !autocompleteLabel"
              name="walletPassword"
              label="Wallet password"
              placeholder="Enter your wallet password"
              counter
              class="mt-3"
              autocomplete="current-passord"
              @click:append="walletPasswordShow = !walletPasswordShow" />

            <slot></slot>

            <v-text-field
              v-if="dialog"
              v-model="transactionLabel"
              :disabled="loading"
              :class="inputLabel || 'mt-3'"
              :autofocus="!inputLabel && !autocompleteLabel && storedPassword"
              type="text"
              name="transactionLabel"
              label="Label (Optional)"
              placeholder="Enter label for your transaction" />
            <v-textarea
              v-model="transactionMessage"
              :disabled="loading"
              name="transactionMessage"
              label="Message (Optional)"
              placeholder="Enter a custom message to send with your transaction"
              class="mt-4"
              rows="3"
              flat
              no-resize />

            <gas-price-choice :estimated-fee="`${toFixed(transactionFeeFiat)} ${fiatSymbol}`" @changed="gasPrice = $event" />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <button
            :disabled="disableSend"
            :loading="loading"
            class="btn btn-primary mr-1"
            @click="send">
            Send
          </button> <button
            :disabled="loading"
            class="btn"
            color="secondary"
            @click="dialog = false">
            Close
          </button>
          <v-spacer />
        </v-card-actions>
      </v-card>
    </v-card>
  </v-dialog>
</template>

<script>

export default {
  props: {
    title: {
      type: String,
      default: function() {
        return null;
      },
    },
    autocompleteLabel: {
      type: String,
      default: function() {
        return null;
      },
    },
    autocompletePlaceholder: {
      type: String,
      default: function() {
        return null;
      },
    },
    inputLabel: {
      type: String,
      default: function() {
        return null;
      },
    },
    inputPlaceholder: {
      type: String,
      default: function() {
        return null;
      },
    },
    walletAddress: {
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
    convertWei: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    methodName: {
      type: String,
      default: function() {
        return null;
      },
    },
    noButton: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      dialog: null,
      loading: false,
      transactionHash: null,
      storedPassword: false,
      walletPassword: '',
      walletPasswordShow: false,
      useMetamask: false,
      autocompleteValue: null,
      inputValue: null,
      transactionLabel: '',
      transactionMessage: '',
      fiatSymbol: null,
      gasEstimation: null,
      gasPrice: 0,
      warning: null,
      error: null,
    };
  },
  computed: {
    transactionFeeFiat() {
      return this.walletUtils.estimateTransactionFeeFiat(this.gasEstimation, this.gasPrice);
    },
    disableSend() {
      return this.loading || (this.inputLabel && this.inputValue !== 0 && !this.inputValue) || (this.autocompleteLabel && !this.autocompleteValue);
    },
    method() {
      return this.contractDetails.contract.methods[this.methodName];
    },
    inputValueForEstimation() {
      return this.inputValueFormatted || 1;
    },
    autocompleteValueForEstimation() {
      return this.autocompleteValue || '0x1111111111111111111111111111111111111111';
    },
    argumentsForEstimation() {
      const parameters = [];
      if (this.autocompleteLabel) {
        parameters.push(this.autocompleteValueForEstimation);
      }
      if (this.inputLabel) {
        parameters.push(this.inputValueForEstimation);
      }
      return parameters;
    },
    arguments() {
      const parameters = [];
      if (this.autocompleteLabel) {
        parameters.push(this.autocompleteValue);
      }
      if (this.inputLabel) {
        parameters.push(this.inputValueFormatted);
      }
      return parameters;
    },
    inputValueFormatted() {
      if (this.inputValue && this.convertWei) {
        return window.localWeb3.utils.toWei(this.inputValue, 'ether').toString();
      } else {
        return this.inputValue;
      }
    },
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.init();
        this.$nextTick(() => {
          this.walletUtils.setDraggable();
        });
      } else {
        this.$emit('close');
      }
    },
  },
  methods: {
    init() {
      if (this.$refs.autocompleteInput) {
        this.$refs.autocompleteInput.clear();
      }
      this.loading = false;
      this.autocompleteValue = null;
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.inputValue = null;
      this.warning = null;
      this.error = null;
      this.gasEstimation = null;
      this.transactionLabel = '';
      this.transactionMessage = '';
      this.transactionHash = null;
      if (!this.gasPrice) {
        this.gasPrice = window.walletSettings.minGasPrice;
      }
      this.useMetamask = window.walletSettings.userPreferences.useMetamask;
      this.fiatSymbol = window.walletSettings.fiatSymbol;
      this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
      this.$nextTick(this.estimateTransactionFee);
    },
    preselectAutocomplete(id, type, address) {
      if ((!id || !type) && !address) {
        console.debug('preselectAutocomplete - empty parameters', id, type, address);
        return;
      }
      this.dialog = true;
      this.$nextTick(() => {
        if (this.$refs.autocompleteInput) {
          this.$refs.autocompleteInput.selectItem(id ? id : address, type);
        }
      });
    },
    estimateTransactionFee() {
      // Estimate gas
      this.method(...this.argumentsForEstimation)
        .estimateGas({
          from: this.contractDetails.contract.options.from,
          gas: window.walletSettings.userPreferences.defaultGas,
          gasPrice: this.gasPrice,
        })
        .then((estimatedGas) => {
          // Add 10% of estimated gas
          this.gasEstimation = estimatedGas * 1.1;
        })
        .catch((e) => {
          console.debug('Error while estimating gas', e);
        });
    },
    send() {
      this.error = null;
      this.warning = null;

      if (!this.storedPassword && (!this.walletPassword || !this.walletPassword.length)) {
        this.error = 'Password field is mandatory';
        return;
      }

      if (this.autocompleteLabel && !window.localWeb3.utils.isAddress(this.autocompleteValue)) {
        this.error = `Invalid input value for field ${this.autocompleteLabel}`;
        return;
      }

      if (this.inputLabel && !this.inputValue) {
        this.error = `Invalid input value for field ${this.inputLabel}`;
        return;
      }

      const unlocked = this.useMetamask || this.walletUtils.unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : this.walletUtils.hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = 'Wrong password';
        return;
      }

      this.loading = true;
      try {
        this.method(...this.argumentsForEstimation)
          .estimateGas({
            from: this.contractDetails.contract.options.from,
            gas: window.walletSettings.userPreferences.defaultGas,
            gasPrice: this.gasPrice,
          })
          .then((estimatedGas) => {
            if (estimatedGas > window.walletSettings.userPreferences.defaultGas) {
              this.warning = `You have set a low gas ${window.walletSettings.userPreferences.defaultGas} while the estimation of necessary gas is ${estimatedGas}. Please change it in your preferences.`;
              return;
            }
            return this.method(...this.arguments)
              .send({
                from: this.contractDetails.contract.options.from,
                gas: window.walletSettings.userPreferences.defaultGas,
                gasPrice: this.gasPrice,
              })
              .on('transactionHash', (hash) => {
                this.transactionHash = hash;
                const sender = this.contractDetails.contract.options.from;
                const receiver = this.autocompleteValue ? this.autocompleteValue : this.contractDetails.address;

                const gas = window.walletSettings.userPreferences.defaultGas ? window.walletSettings.userPreferences.defaultGas : 35000;

                const pendingTransaction = {
                  hash: hash,
                  from: sender,
                  to: receiver,
                  type: 'contract',
                  value: 0,
                  gas: gas,
                  gasPrice: this.gasPrice,
                  pending: true,
                  adminOperation: true,
                  contractAddress: this.contractDetails.address,
                  contractMethodName: this.methodName,
                  contractAmount: this.inputValue,
                  contractSymbol: this.contractSymbol,
                  contractAmountLabel: this.contractAmountLabel,
                  label: this.transactionLabel,
                  message: this.transactionMessage,
                  timestamp: Date.now(),
                  feeFiat: this.transactionFeeFiat,
                };

                // *async* save transaction message for contract, sender and (avoid alarm receiver for admin operations)
                this.transactionUtils.saveTransactionDetails(pendingTransaction)
                  .then(() => {
                    // The transaction has been hashed and will be sent
                    this.$emit('sent', pendingTransaction, this.contractDetails);
                  });

                const thiss = this;
                // FIXME workaround when can't execute .then(...) method, especially in pause, unpause.
                this.walletUtils.watchTransactionStatus(hash, () => {
                  thiss.$emit('success', thiss.transactionHash, thiss.contractDetails, thiss.methodName, thiss.autocompleteValue, thiss.inputValue);
                });
                this.dialog = false;
              })
              .on('error', (error, receipt) => {
                console.debug('Error while sending admin transaction', error);
                // The transaction has failed
                this.error = this.walletUtils.truncateError(error);
                // Display error on main screen only when dialog is not opened
                if (!this.dialog) {
                  this.$emit('error', this.error);
                }
              })
              .then(() => {
                this.$emit('success', this.transactionHash, this.contractDetails, this.methodName, this.autocompleteValue, this.inputValue);
              })
              .finally(() => {
                this.loading = false;
              });
          })
          .catch((e) => {
            console.debug('Error while sending admin transaction', e);
            this.error = this.walletUtils.truncateError(e);
          })
          .finally(() => {
            this.loading = false;
            if (!this.useMetamask) {
              this.walletUtils.lockBrowserWallet();
            }
          });
      } catch (e) {
        console.debug('Error while sending admin transaction', e);
        this.loading = false;
        this.error = this.walletUtils.truncateError(e);
      }
    },
  },
};
</script>
