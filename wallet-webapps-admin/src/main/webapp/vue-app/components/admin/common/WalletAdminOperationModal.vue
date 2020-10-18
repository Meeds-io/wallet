<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <v-dialog
    v-model="dialog"
    :disabled="disabled"
    content-class="uiPopup with-overflow walletDialog"
    width="500px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <template v-if="!noButton" v-slot:activator="{ on }">
      <button
        :value="true"
        class="ignore-vuetify-classes btn"
        v-on="on">
        {{ title }}
      </button>
    </template>
    <v-card class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a>
        <span class="ignore-vuetify-classes PopupTitle popupTitle">
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
              name="inputValue" />

            <v-text-field
              v-if="dialog && !storedPassword"
              v-model="walletPassword"
              :append-icon="walletPasswordShow ? 'mdi-eye' : 'mdi-eye-off'"
              :type="walletPasswordShow ? 'text' : 'password'"
              :disabled="loading"
              :autofocus="!inputLabel && !autocompleteLabel"
              :label="$t('exoplatform.wallet.label.walletPassword')"
              :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
              name="walletPassword"
              autocomplete="current-passord"
              @click:append="walletPasswordShow = !walletPasswordShow" />

            <slot></slot>

            <v-text-field
              v-if="dialog"
              v-model="transactionLabel"
              :disabled="loading"
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
              rows="3"
              flat
              no-resize />

            <gas-price-choice
              :wallet="wallet"
              :estimated-fee="`${walletUtils.toFixed(transactionFeeFiat)} ${fiatSymbol}`"
              @changed="gasPrice = $event" />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <button
            :disabled="disableSend"
            :loading="loading"
            class="ignore-vuetify-classes btn btn-primary mr-1"
            @click="send">
            {{ $t('exoplatform.wallet.button.send') }}
          </button> <button
            :disabled="loading"
            class="ignore-vuetify-classes btn"
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
    wallet: {
      type: Object,
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
      return this.loading || !this.gasPrice || (this.inputLabel && this.inputValue !== 0 && !this.inputValue) || (this.autocompleteLabel && !this.autocompleteValue);
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
      this.settings = window.walletSettings || {network: {}}
      if (!this.gasPrice) {
        this.gasPrice = this.settings.network.normalGasPrice;
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
          this.gasEstimation = parseInt(estimatedGas * 1.1);
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

          const transactionDetail = {
            from: this.contractDetails.contract.options.from,
            to: this.autocompleteValue ? this.autocompleteValue : this.contractDetails.address,
            value: 0,
            gas: this.settings.network.gasLimit,
            gasPrice: this.gasPrice,
            pending: true,
            adminOperation: true,
            contractAddress: this.contractDetails.address,
            contractMethodName: this.methodName,
            contractAmount: this.inputValue,
            label: this.transactionLabel,
            message: this.transactionMessage,
            timestamp: Date.now()
          };

          return this.tokenUtils.sendContractTransaction(transactionDetail, this.method, this.arguments);
        })
        .then((savedTransaction) => {
          this.$emit('sent', savedTransaction);
          this.dialog = false;

          const thiss = this;
          this.walletUtils.watchTransactionStatus(savedTransaction.hash, () => {
            thiss.$emit('success', savedTransaction.hash, thiss.methodName, thiss.autocompleteValue, thiss.inputValue);
          });
        })
        .catch((e) => {
          console.debug('Error while sending admin transaction', e);
          this.error = this.walletUtils.truncateError(e);
        })
        .finally(() => {
          this.walletUtils.lockBrowserWallet();
          this.loading = false;
        });
    },
  },
};
</script>
