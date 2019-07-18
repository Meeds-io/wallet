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
              :label="$t('exoplatform.wallet.label.walletPassword')"
              :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
              name="walletPassword"
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
              :label="$t('exoplatform.wallet.label.transactionLabel')"
              :placeholder="$t('exoplatform.wallet.label.transactionLabelPlaceholder')"
              type="text"
              name="transactionLabel" />
            <v-textarea
              v-model="transactionMessage"
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.transactionMessage')"
              :placeholder="$t('exoplatform.wallet.label.transactionMessagePlaceholder')"
              name="transactionMessage"
              class="mt-4"
              rows="3"
              flat
              no-resize />

            <gas-price-choice :estimated-fee="`${walletUtils.toFixed(transactionFeeFiat)} ${fiatSymbol}`" @changed="gasPrice = $event" />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <button
            :disabled="disableSend"
            :loading="loading"
            class="btn btn-primary mr-1"
            @click="send">
            {{ $t('exoplatform.wallet.button.send') }}
          </button> <button
            :disabled="loading"
            class="btn"
            color="secondary"
            @click="dialog = false">
            {{ $t('exoplatform.wallet.button.close') }}
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
      autocompleteValue: null,
      inputValue: null,
      transactionLabel: '',
      transactionMessage: '',
      fiatSymbol: null,
      gasEstimation: null,
      gasPrice: 0,
      warning: null,
      error: null,
      settings: null,
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
      this.settings = window.walletSettings || {network: {}}
      if (!this.gasPrice) {
        this.gasPrice = this.settings.network.minGasPrice;
      }
      this.fiatSymbol = this.settings.fiatSymbol || '$';
      this.storedPassword = this.settings.storedPassword && this.settings.browserWalletExists;
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
          gas: this.settings.network.gasLimit,
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
        this.error = this.$t('exoplatform.wallet.warning.passwordFieldMandatory');
        return;
      }

      if (this.autocompleteLabel && !window.localWeb3.utils.isAddress(this.autocompleteValue)) {
        this.error = this.$t('exoplatform.wallet.warning.invalidValueForField', {0: this.autocompleteLabel});
        return;
      }

      if (this.inputLabel && !this.inputValue) {
        this.error = this.$t('exoplatform.wallet.warning.invalidValueForField', {0: this.inputLabel});
        return;
      }

      const unlocked = this.walletUtils.unlockBrowserWallet(this.storedPassword ? this.settings.userP : this.walletUtils.hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = this.$t('exoplatform.wallet.warning.wrongPassword');
        return;
      }

      this.loading = true;
      try {
        this.method(...this.argumentsForEstimation)
          .estimateGas({
            from: this.contractDetails.contract.options.from,
            gas: this.settings.network.gasLimit,
            gasPrice: this.gasPrice,
          })
          .then((estimatedGas) => {
            if (estimatedGas > this.settings.network.gasLimit) {
              this.warning = `You have set a low gas ${this.settings.network.gasLimit} while the estimation of necessary gas is ${estimatedGas}. Please change it in your preferences.`;
              return;
            }
            return this.method(...this.arguments)
              .send({
                from: this.contractDetails.contract.options.from,
                gas: this.settings.network.gasLimit,
                gasPrice: this.gasPrice,
              })
              .on('transactionHash', (hash) => {
                this.transactionHash = hash;
                const sender = this.contractDetails.contract.options.from;
                const receiver = this.autocompleteValue ? this.autocompleteValue : this.contractDetails.address;

                const gas = this.settings.network.gasLimit || 35000;

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
                    this.$emit('sent', pendingTransaction);
                  });

                const thiss = this;
                // FIXME workaround when can't execute .then(...) method, especially in pause, unpause.
                this.walletUtils.watchTransactionStatus(hash, () => {
                  thiss.$emit('success', thiss.transactionHash, thiss.methodName, thiss.autocompleteValue, thiss.inputValue);
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
                this.$emit('success', this.transactionHash, this.methodName, this.autocompleteValue, this.inputValue);
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
            this.walletUtils.lockBrowserWallet();
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
