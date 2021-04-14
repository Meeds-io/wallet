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
        {{ $t('exoplatform.wallet.button.upgradeContract', {0: contractDetails && contractDetails.name}) }}
      </button>
    </template>
    <v-card class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a>
        <span class="ignore-vuetify-classes PopupTitle popupTitle">
          {{ $t('exoplatform.wallet.button.upgradeContract', {0: contractDetails && contractDetails.name}) }}
        </span>
      </div>

      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
        <i class="uiIconWarning"></i>{{ warning }}
      </div>

      <v-card flat>
        <v-card-title v-show="loading" class="pb-0">
          <v-spacer />
          <v-progress-circular
            color="primary"
            indeterminate
            size="20" />
          <v-spacer />
        </v-card-title>
        <v-card-title v-show="loading && step" class="pb-0">
          <v-spacer />
          <div>{{ $t('exoplatform.wallet.label.step') }} {{ step }} / 3</div>
          <v-spacer />
        </v-card-title>
        <v-card-text class="pt-0">
          <v-form
            ref="form"
            @submit="
              $event.preventDefault();
              $event.stopPropagation();
            ">
            <v-text-field
              v-if="dialog && !storedPassword"
              v-model="walletPassword"
              :append-icon="walletPasswordShow ? 'mdi-eye' : 'mdi-eye-off'"
              :type="walletPasswordShow ? 'text' : 'password'"
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.walletPassword')"
              :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
              name="walletPassword"
              autofocus
              required
              autocomplete="current-passord"
              @click:append="walletPasswordShow = !walletPasswordShow" />
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
            class="ignore-vuetify-classes btn btn-primary me-1"
            @click="send">
            {{ $t('exoplatform.wallet.button.upgrade') }}
          </button>
          <button
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
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    implementationVersion: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    }
  },
  data() {
    return {
      dialog: null,
      loading: false,
      storedPassword: false,
      walletPassword: '',
      walletPasswordShow: false,
      fiatSymbol: null,
      gasEstimation: null,
      gasPrice: 0,
      gasLimit: 4700000,
      step: 0,
      warning: null,
      error: null,
    };
  },
  computed: {
    transactionFeeFiat() {
      return this.walletUtils.estimateTransactionFeeFiat(this.gasEstimation, this.gasPrice);
    },
    disableSend() {
      return this.loading || !this.gasPrice;
    },
    method() {
      return this.contractDetails.contract.methods[this.methodName];
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
      this.loading = false;
      this.autocompleteValue = null;
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.warning = null;
      this.error = null;
      this.gasEstimation = null;
      if (!this.gasPrice) {
        this.gasPrice = window.walletSettings.network.normalGasPrice;
      }
      this.fiatSymbol = window.walletSettings.fiatSymbol;
      this.storedPassword = window.walletSettings.storedPassword && window.walletSettings.browserWalletExists;
      this.$nextTick(this.estimateTransactionFee);
    },
    upgradeToken(estimateGas) {
      const currentUpgradeState = this.getUpgradeState();
      let ertTokenV2Address = (estimateGas && '0x1111111111111111111111111111111111111111') || (currentUpgradeState && currentUpgradeState.ertTokenV2Address);
      this.step = estimateGas ? 0 : (currentUpgradeState && currentUpgradeState.step) || 1;

      let estimatedGas = 0;

      this.loading = true;
      return this.tokenUtils.createNewContractInstanceByName('ERTTokenV2')
        .then((ertTokenV2Instance) => {
          if (estimateGas) {
            return this.tokenUtils.estimateContractDeploymentGas(ertTokenV2Instance);
          } else if (this.step < 2) {
            return this.tokenUtils.deployContract(ertTokenV2Instance, this.walletAddress, this.gasLimit, this.gasPrice);
          }
        })
        .then((data, error) => {
          if (error) {
            throw error;
          }
          if (estimateGas) {
            estimatedGas += parseInt(data * 1.1);
          } else if (this.step < 2) {
            if (!data || !data.options || !data.options.address) {
              throw new Error(this.$t('exoplatform.wallet.warning.notFoundContractAddress'));
            } else {
              ertTokenV2Address = data.options.address;
              this.saveUpgradeState({
                ertTokenV2Address: ertTokenV2Address,
                step: 2,
              });
            }
          }
        })
        .then(() => this.contractDetails.contract.methods.upgradeImplementation(this.contractDetails.address, this.implementationVersion, ertTokenV2Address))
        .then((operation) => {
          if (estimateGas) {
            return operation.estimateGas({
              from: this.walletAddress,
              gas: this.gasLimit,
              gasPrice: this.gasPrice,
            });
          } else {
            this.step = 3;
            return operation.send({
              from: this.walletAddress,
              gas: this.gasLimit,
              gasPrice: this.gasPrice,
            });
          }
        })
        .then((result) => {
          if (estimateGas) {
            estimatedGas += parseInt(result * 1.1);
            this.gasEstimation = estimatedGas;
          } else {
            this.removeUpgradeState();
            this.$emit('success', result && result.hash, 'upgrade');
          }
          return true;
        })
        .catch((e) => {
          console.error('deployContract method - error', e);
          this.error = this.$t('exoplatform.wallet.warning.contractUpgradeError', {0: this.walletUtils.truncateError(String(e))});
        })
        .finally(() => {
          this.loading = false;
        });
    },
    estimateTransactionFee() {
      return this.upgradeToken(true);
    },
    saveUpgradeState(state) {
      window.localStorage.setItem(`exo-wallet-upgrade-v2-${this.contractDetails.address}`, JSON.stringify(state));
    },
    getUpgradeState() {
      const state = window.localStorage.getItem(`exo-wallet-upgrade-v2-${this.contractDetails.address}`);
      if (state) {
        return JSON.parse(state);
      } else {
        return null;
      }
    },
    removeUpgradeState() {
      window.localStorage.removeItem(`exo-wallet-upgrade-v2-${this.contractDetails.address}`);
    },
    send() {
      this.error = null;
      this.warning = null;

      if (!this.$refs.form.validate()) {
        return;
      }

      const unlocked = this.walletUtils.unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : this.walletUtils.hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = this.$t('exoplatform.wallet.warning.wrongPassword');
        return;
      }

      return this.upgradeToken()
        .then(upgraded => {
          if (upgraded) {
            this.dialog = false;
            this.walletUtils.lockBrowserWallet();
          }
        });
    },
  },
};
</script>
