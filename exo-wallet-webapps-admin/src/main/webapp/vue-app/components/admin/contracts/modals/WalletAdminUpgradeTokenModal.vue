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
          Upgrade Token
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
          Upgrade Token to version 2
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
          <div>Step {{ step }} / 4</div>
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
              :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
              :type="walletPasswordShow ? 'text' : 'password'"
              :disabled="loading"
              autofocus
              name="walletPassword"
              label="Wallet password"
              placeholder="Enter your wallet password"
              counter
              required
              class="mt-3"
              autocomplete="current-passord"
              @click:append="walletPasswordShow = !walletPasswordShow" />
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
            Upgrade
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
    }
  },
  data() {
    return {
      dialog: null,
      loading: false,
      storedPassword: false,
      walletPassword: '',
      walletPasswordShow: false,
      useMetamask: false,
      fiatSymbol: null,
      gasEstimation: null,
      gasPrice: 0,
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
      return this.loading;
    },
    method() {
      return this.contractDetails.contract.methods[this.methodName];
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
      this.loading = false;
      this.autocompleteValue = null;
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.warning = null;
      this.error = null;
      this.gasEstimation = null;
      if (!this.gasPrice) {
        this.gasPrice = window.walletSettings.minGasPrice;
      }
      this.useMetamask = window.walletSettings.userPreferences.useMetamask;
      this.fiatSymbol = window.walletSettings.fiatSymbol;
      this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
      this.$nextTick(this.estimateTransactionFee);
    },
    upgradeToken(estimateGas) {
      const currentUpgradeState = this.getUpgradeState();
      let ertTokenV2Address = (estimateGas && '0x1111111111111111111111111111111111111111') || (currentUpgradeState && currentUpgradeState.ertTokenV2Address);
      let ertTokenDataV2Address = (estimateGas && '0x1111111111111111111111111111111111111111') || (currentUpgradeState && currentUpgradeState.ertTokenDataV2Address);
      this.step = estimateGas ? 0 : (currentUpgradeState && currentUpgradeState.step) || 1;

      let estimatedGas = 0;

      this.loading = true;
      return this.tokenUtils.createNewContractInstanceByName('ERTTokenV2')
        .then((ertTokenV2Instance) => {
          if (estimateGas) {
            return this.tokenUtils.estimateContractDeploymentGas(ertTokenV2Instance);
          } else if (this.step < 2) {
            return this.tokenUtils.deployContract(ertTokenV2Instance, this.walletAddress, 4700000, this.gasPrice);
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
              throw new Error('Cannot find address of newly deployed address');
            } else {
              ertTokenV2Address = data.options.address;
              this.saveUpgradeState({
                ertTokenV2Address: ertTokenV2Address,
                step: 2,
              });
            }
          }
        })
        .then(() => this.tokenUtils.createNewContractInstanceByName('ERTTokenDataV2', this.contractDetails.address, ertTokenV2Address))
        .then((ertTokenDataV2Instance) => {
          if (estimateGas) {
            return this.tokenUtils.estimateContractDeploymentGas(ertTokenDataV2Instance);
          } else if (this.step < 3) {
            this.step = 2;
            return this.tokenUtils.deployContract(ertTokenDataV2Instance, this.walletAddress, 4700000, this.gasPrice);
          }
        })
        .then((data, error) => {
          if (error) {
            throw error;
          }

          if (estimateGas) {
            estimatedGas += parseInt(data * 1.1);
          } else if (this.step < 3) {
            if (!data || !data.options || !data.options.address) {
              throw new Error('Cannot find address of newly deployed address');
            } else {
              ertTokenDataV2Address = data.options.address;
              this.saveUpgradeState({
                ertTokenV2Address: ertTokenV2Address,
                ertTokenDataV2Address: ertTokenDataV2Address,
                step: 3,
              });
            }
          }
        })
        .then(() => this.step < 4 && this.contractDetails.contract.methods.upgradeData(2, ertTokenDataV2Address))
        .then((operation) => {
          if (estimateGas) {
            return operation.estimateGas({
              from: this.walletAddress,
              gas: 4700000,
              gasPrice: this.gasPrice,
            });
          } else if (this.step < 4) {
            this.step = 3;
            return operation.send({
              from: this.walletAddress,
              gas: 4700000,
              gasPrice: this.gasPrice,
            });
          }
        })
        .then((gasEstimation) => {
          if (estimateGas) {
            estimatedGas += parseInt(gasEstimation * 1.1);
          } else if (this.step < 4) {
            this.saveUpgradeState({
              ertTokenV2Address: ertTokenV2Address,
              ertTokenDataV2Address: ertTokenDataV2Address,
              step: 4,
            });
          }
        })
        .then(() => this.contractDetails.contract.methods.upgradeImplementation(this.contractDetails.address, 2, ertTokenV2Address))
        .then((operation) => {
          if (estimateGas) {
            return operation.estimateGas({
              from: this.walletAddress,
              gas: 4700000,
              gasPrice: this.gasPrice,
            });
          } else {
            this.step = 4;
            return operation.send({
              from: this.walletAddress,
              gas: 4700000,
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
            this.$emit('success', result && result.hash, this.contractDetails, 'upgrade');
            // TODO add three trasactions in the list
          }
          return true;
        })
        .catch((e) => {
          console.debug('deployContract method - error', e);
          this.error = `Error during contract upgrade: ${this.walletUtils.truncateError(String(e))}`;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    estimateTransactionFee() {
      return this.upgradeToken(true);
    },
    saveUpgradeState(state) {
      window.localStorage.setItem(`exo-wallet-upgrade-v2-${this.contractDetails.address}`, JSON.stringify(state))
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

      const unlocked = this.useMetamask || this.walletUtils.unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : this.walletUtils.hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = 'Wrong password';
        return;
      }

      return this.upgradeToken()
        .then(upgraded => {
          if (upgraded) {
            this.dialog = false;

            if (!this.useMetamask) {
              this.walletUtils.lockBrowserWallet();
            }
          }
        });
    },
  },
};
</script>
