<template>
  <v-dialog
    v-model="createNewToken"
    attach="#walletDialogsParent"
    content-class="uiPopup createNewToken"
    fullscreen
    hide-overlay
    transition="dialog-bottom-transition"
    persistent>
    <button
      slot="activator"
      class="btn btn-primary mt-3"
      @click="createNewToken = true">
      Deploy new Token
    </button>
    <v-card flat>
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="createNewToken = false"></a> <span class="PopupTitle popupTitle">
            Deploy new token contracts
          </span>
      </div> <div v-if="error" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <v-stepper v-model="step">
        <v-stepper-header flat>
          <v-stepper-step :complete="step > 1" step="1">
            Deployment of Data contract
          </v-stepper-step>
          <v-divider />
          <v-stepper-step :complete="step > 2" step="2">
            Deployment of Token contract
          </v-stepper-step>
          <v-divider />
          <v-stepper-step :complete="step > 3" step="3">
            Deployment of Proxy contract
          </v-stepper-step>
          <v-divider />
          <v-stepper-step :complete="step > 4" step="4">
            Transfer data ownership
          </v-stepper-step>
          <v-divider />
          <v-stepper-step :complete="step > 5" step="5">
            ERC 20 initialization
          </v-stepper-step>
          <v-divider />
          <v-stepper-step :complete="step === 6" step="6">
            Completed
          </v-stepper-step>
        </v-stepper-header>
        <v-stepper-items>
          <v-stepper-content step="1" class="deploymentStep">
            <contract-deployment-step
              :network-id="networkId"
              :stored-password="storedPassword"
              :transaction-hash="transactionHashByStep[step]"
              :gas="gasByStep[step]"
              :contract-address="contractAddressByStep[step]"
              :processing="processingStep[step]"
              :processed="processedStep[step]"
              :transaction-fee="gasFee"
              :disabled-button="disabledButton"
              :fiat-symbol="fiatSymbol"
              button-title="Deploy"
              @proceed="proceedStep($event)"
              @next="step++">
              <v-slider
                v-model="gasPrice"
                :label="`Gas price: ${gasPriceGwei} Gwei ${gasFee}`"
                :min="1000000000"
                :max="20000000000"
                :step="1000000000"
                type="number"
                class="mt-4 mr-5"
                required />
            </contract-deployment-step>
          </v-stepper-content>
          <v-stepper-content step="2" class="deploymentStep">
            <contract-deployment-step
              :network-id="networkId"
              :stored-password="storedPassword"
              :transaction-hash="transactionHashByStep[step]"
              :gas="gasByStep[step]"
              :contract-address="contractAddressByStep[step]"
              :processing="processingStep[step]"
              :processed="processedStep[step]"
              :transaction-fee="gasFee"
              :disabled-button="disabledButton"
              :fiat-symbol="fiatSymbol"
              button-title="Deploy"
              @proceed="proceedStep($event)"
              @next="step++">
              <v-slider
                v-model="gasPrice"
                :label="`Gas price: ${gasPriceGwei} Gwei ${gasFee}`"
                :min="1000000000"
                :max="20000000000"
                :step="1000000000"
                type="number"
                class="mt-4 mr-5"
                required />
            </contract-deployment-step>
          </v-stepper-content>
          <v-stepper-content step="3" class="deploymentStep">
            <contract-deployment-step
              :network-id="networkId"
              :stored-password="storedPassword"
              :transaction-hash="transactionHashByStep[step]"
              :gas="gasByStep[step]"
              :contract-address="contractAddressByStep[step]"
              :processing="processingStep[step]"
              :processed="processedStep[step]"
              :transaction-fee="gasFee"
              :disabled-button="disabledButton"
              :fiat-symbol="fiatSymbol"
              button-title="Deploy"
              @proceed="proceedStep($event)"
              @next="step++">
              <v-slider
                v-model="gasPrice"
                :label="`Gas price: ${gasPriceGwei} Gwei ${gasFee}`"
                :min="1000000000"
                :max="20000000000"
                :step="1000000000"
                type="number"
                class="mt-4 mr-5"
                required />
            </contract-deployment-step>
          </v-stepper-content>
          <v-stepper-content step="4" class="deploymentStep">
            <contract-deployment-step
              :network-id="networkId"
              :stored-password="storedPassword"
              :transaction-hash="transactionHashByStep[step]"
              :gas="gasByStep[step]"
              :processing="processingStep[step]"
              :processed="processedStep[step]"
              :transaction-fee="gasFee"
              :disabled-button="disabledButton"
              :fiat-symbol="fiatSymbol"
              button-title="Send"
              @proceed="proceedStep($event)"
              @next="step++">
              <v-slider
                v-model="gasPrice"
                :label="`Gas price: ${gasPriceGwei} Gwei ${gasFee}`"
                :min="1000000000"
                :max="20000000000"
                :step="1000000000"
                type="number"
                class="mt-4 mr-5"
                required />
            </contract-deployment-step>
          </v-stepper-content>
          <v-stepper-content step="5" class="deploymentStep">
            <contract-deployment-step
              :network-id="networkId"
              :stored-password="storedPassword"
              :transaction-hash="transactionHashByStep[step]"
              :gas="gasByStep[step]"
              :processing="processingStep[step]"
              :processed="processedStep[step]"
              :transaction-fee="gasFee"
              :disabled-button="disabledButton"
              :fiat-symbol="fiatSymbol"
              button-title="Send"
              @proceed="proceedStep($event)"
              @next="step++">
              <v-slider
                v-model="gasPrice"
                :label="`Gas price: ${gasPriceGwei} Gwei ${gasFee} ${fiatSymbol}`"
                :min="1000000000"
                :max="20000000000"
                :step="1000000000"
                type="number"
                class="mt-4 mr-5"
                required />

              <v-form ref="form" class="pl-5 pr-5 pt-3 flex">
                <v-text-field
                  v-model="newTokenName"
                  :rules="mandatoryRule"
                  label="Token name"
                  placeholder="Enter the ERC20 token name"
                  required
                  autofocus />
                <v-text-field
                  v-model="newTokenSymbol"
                  :rules="mandatoryRule"
                  label="Token symbol"
                  placeholder="Enter the token symbol to uses to display token amounts"
                  required />
                <v-text-field
                  v-model="newTokenInitialCoins"
                  :rules="mandatoryRule"
                  label="Initial token coins supply"
                  placeholder="Enter an amount of initial token supply"
                  required />
                <v-slider
                  v-model="newTokenDecimals"
                  :label="`Token coins decimals: ${newTokenDecimals}`"
                  :min="0"
                  :max="18"
                  :step="1"
                  type="number"
                  required />
              </v-form>
            </contract-deployment-step>
          </v-stepper-content>
          <v-stepper-content step="6">
            <v-card flat>
              <v-card-title>
                The Token has been deployed. <a :href="tokenEtherscanLink" target="_blank">
                  See it on etherscan
                </a>
              </v-card-title>
              <v-card-actions>
                <v-btn
                  :loading="processingStep[step]"
                  :disabled="processingStep[step]"
                  color="primary"
                  @click="finishInstallation">
                  <span class="ml-2 mr-2">
                    Finish deployment
                  </span>
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-stepper-content>
        </v-stepper-items>
      </v-stepper>
    </v-card>
  </v-dialog>
</template>

<script>
import ContractDeploymentStep from './WalletAdminContractDeploymentStep.vue';

export default {
  components: {
    ContractDeploymentStep,
  },
  props: {
    account: {
      type: String,
      default: function() {
        return null;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return '$';
      },
    },
    networkId: {
      type: Number,
      default: function() {
        return 0;
      },
    },
  },
  data() {
    return {
      error: '',
      storedPassword: false,
      newTokenName: '',
      newTokenSymbol: '',
      newTokenDecimals: 18,
      newTokenInitialCoins: 0,
      createNewToken: false,
      useMetamask: false,
      transactionHashByStep: {},
      contractAddressByStep: {},
      processedStep: {},
      step: 0,
      gasPrice: 0,
      contractInstancesByStep: {},
      gasByStep: {},
      processingStep: {},
      contractNameByStep: {
        1: 'ERTTokenDataV1',
        2: 'ERTTokenV1',
        3: 'ERTToken',
      },
      mandatoryRule: [(v) => !!v || 'Field is required'],
    };
  },
  computed: {
    gasPriceGwei() {
      return this.gasPrice && window.localWeb3 && window.localWeb3.utils.fromWei(String(this.gasPrice), 'gwei');
    },
    gasFee() {
      return this.calculateGasPriceInFiat(this.gasByStep[this.step], this.gasPrice);
    },
    tokenEtherscanLink() {
      return this.contractAddressByStep && this.contractAddressByStep[2] ? this.walletUtils.getTokenEtherscanlink(this.networkId) + this.contractAddressByStep[2] : null;
    },
    contractDeploymentParameters() {
      if (this.step === 3) {
        // ERTToken parameters
        return [this.contractAddressByStep[2], this.contractAddressByStep[1]];
      }
      return [];
    },
    disabledButton() {
      return !this.gasByStep[this.step] || this.contractAddressByStep[this.step] || this.processingStep[this.step];
    },
  },
  watch: {
    newTokenGasPrice() {
      this.newTokenGasPriceGWEI = window.localWeb3 && window.localWeb3.utils.fromWei(this.newTokenGasPrice.toString(), 'gwei');
      this.calculateGasPriceInFiat();
    },
    createNewToken() {
      if (this.createNewToken) {
        this.resetContractForm();
      }
    },
    step() {
      this.initializeStep();
    },
  },
  methods: {
    resetContractForm() {
      this.step = 0;
      this.error = '';
      this.newTokenName = '';
      this.newTokenSymbol = '';
      this.newTokenDecimals = 18;
      this.newTokenInitialCoins = 1000000;
      this.contractInstancesByStep = {};
      this.gasByStep = {};
      this.processingStep = {};
      this.useMetamask = window.walletSettings.userPreferences.useMetamask;
      this.gasPrice = window.walletSettings.normalGasPrice;
      this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
      this.loadState();
    },
    initializeStep(stepToInitialize) {
      // Add it inside a constant in case it changes in parallel
      const step = stepToInitialize ? stepToInitialize : this.step;
      if (step > 0 && !this.gasByStep[step]) {
        if (step < 4) {
          if (this.contractAddressByStep[step] && this.contractNameByStep[step]) {
            return this.tokenUtils.createNewContractInstanceByNameAndAddress(this.contractNameByStep[step === 3 ? 2 : step], this.contractAddressByStep[step])
              .then((instance) => this.$set(this.contractInstancesByStep, step, instance) && this.$set(this.processedStep, step, true))
              .catch((e) => (this.error = `Error getting contract with address ${this.contractAddressByStep[step]}: ${e}`));
          } else if (this.contractNameByStep[step]) {
            return this.tokenUtils.createNewContractInstanceByName(this.contractNameByStep[step], ...this.contractDeploymentParameters)
              .then((instance) => {
                this.$set(this.contractInstancesByStep, step, instance);
                return this.tokenUtils.estimateContractDeploymentGas(instance);
              })
              .then((estimatedGas) => {
                this.$set(this.gasByStep, step, parseInt(estimatedGas * 1.1));
              })
              .catch((e) => (this.error = `Error processing contract deployment estimation: ${e}`));
          }
        } else if (step === 4 && !this.processedStep[step]) {
          return this.contractInstancesByStep[1].methods
            .transferDataOwnership(this.contractAddressByStep[3], this.contractAddressByStep[2])
            .estimateGas({
              from: this.contractInstancesByStep[1].options.from,
              gas: 4700000,
              gasPrice: this.gasPrice,
            })
            .then((estimatedGas) => {
              this.$set(this.gasByStep, step, parseInt(estimatedGas * 1.1));
            });
        } else if (step === 5 && !this.processedStep[step]) {
          return this.contractInstancesByStep[3].methods
            .initialize(this.walletUtils.convertTokenAmountToSend(1000000, 18).toString(), 'Token name', 18, 'T')
            .estimateGas({
              from: this.contractInstancesByStep[3].options.from,
              gas: 4700000,
              gasPrice: this.gasPrice,
            })
            .then((estimatedGas) => {
              this.$set(this.gasByStep, step, parseInt(estimatedGas * 1.1));
            })
            .catch((e) => {
              console.error('Error while estimating initialization gas. Try to display contracts details', this.contractInstancesByStep, this.contractAddressByStep, e);
              return this.contractInstancesByStep[1].methods
                .implementation()
                .call()
                .then((implementationAddress) => {
                  console.warn(`Detected implementation address in Data contract ${implementationAddress}, effective implementation address: ${this.contractAddressByStep[2]}`, implementationAddress === this.contractAddressByStep[2] ? ': OK' : ': KO');
                  return this.contractInstancesByStep[1].methods.proxy().call();
                })
                .then((proxyAddress) => {
                  console.warn(`Detected proxy address in Data contract ${proxyAddress}, effective proxy address: ${this.contractAddressByStep[3]}`, proxyAddress === this.contractAddressByStep[3] ? ': OK' : ': KO');
                  return this.contractInstancesByStep[3].methods.getDataAddress(1).call();
                })
                .then((dataAddress) => {
                  console.warn(`Detected data address in Proxy contract ${dataAddress}, effective proxy address: ${this.contractAddressByStep[1]}`, dataAddress === this.contractAddressByStep[1] ? ': OK' : ': KO');
                });
            });
        }
      }
      return Promise.resolve(false);
    },
    calculateGasPriceInFiat(gas, gasPrice) {
      gasPrice = gasPrice ? gasPrice : this.gasPrice;
      const gasPriceInEther = window.localWeb3 && window.localWeb3.utils.fromWei(String(gasPrice), 'ether');
      return this.walletUtils.gasToFiat(gas, gasPriceInEther);
    },
    proceedStep(password) {
      const gasLimit = this.gasByStep[this.step];
      if (!gasLimit) {
        this.error = "Gas estimation isn't done";
        return;
      }

      // Increase gas limit by 10% to ensure that the transaction doesn't go 'Out of Gas'
      const gasPrice = this.gasPrice;

      this.error = null;

      if (!this.storedPassword && (!password || !password.length)) {
        this.error = 'Password field is mandatory';
        return;
      }

      const unlocked = this.useMetamask || this.walletUtils.unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : this.walletUtils.hashCode(password));
      if (!unlocked) {
        this.error = 'Wrong password';
        return;
      }

      const step = this.step;
      try {
        if (step < 4) {
          const contractInstance = this.contractInstancesByStep[step];
          if (!contractInstance) {
            this.error = 'Contract instance not initialized';
            return;
          }

          this.tokenUtils.deployContract(contractInstance, this.account, gasLimit, gasPrice, this.updateTransactionHash)
            .then((newContractInstance, error) => {
              try {
                if (error) {
                  throw error;
                }
                if (!newContractInstance || !newContractInstance.options || !newContractInstance.options.address) {
                  throw new Error('Cannot find address of newly deployed address');
                }
                this.$set(this.contractAddressByStep, step, newContractInstance.options.address);
                this.$set(this.processedStep, step, true);
                this.saveState();
                if (step === 3) {
                  // For Proxy contract, use ABI and BIN files of Implementation instead
                  return this.tokenUtils.createNewContractInstanceByNameAndAddress(this.contractNameByStep[2], this.contractAddressByStep[step]).then((newContractInstance) => {
                    this.$set(this.contractInstancesByStep, step, newContractInstance);
                  });
                } else {
                  // For Proxy contract, use ABI and BIN files of Implementation instead
                  return this.tokenUtils.createNewContractInstanceByNameAndAddress(this.contractNameByStep[step], this.contractAddressByStep[step]).then((newContractInstance) => {
                    this.$set(this.contractInstancesByStep, step, newContractInstance);
                  });
                }
              } catch (e) {
                console.error('Error while setting step as proceeded', e);
              }
            })
            .catch((e) => {
              console.debug('deployContract method - error', e);
              this.error = `Error during contract deployment: ${e}`;
            })
            .finally(() => this.$set(this.processingStep, step, false));
        } else if (step === 4) {
          this.contractInstancesByStep[1].methods
            .transferDataOwnership(this.contractAddressByStep[3], this.contractAddressByStep[2])
            .send({
              from: this.contractInstancesByStep[1].options.from,
              gasPrice: this.gasPrice,
              gas: gasLimit,
            })
            .on('transactionHash', (hash) => {
              this.updateTransactionHash(hash);
            })
            .then(() => {
              this.$set(this.processedStep, step, true);
              this.saveState();
            })
            .finally(() => this.$set(this.processingStep, step, false));
        } else if (step === 5) {
          this.contractInstancesByStep[3].methods
            .initialize(this.walletUtils.convertTokenAmountToSend(this.newTokenInitialCoins, this.newTokenDecimals).toString(), this.newTokenName, this.newTokenDecimals, this.newTokenSymbol)
            .send({
              from: this.contractInstancesByStep[3].options.from,
              gasPrice: this.gasPrice,
              gas: gasLimit,
            })
            .on('transactionHash', (hash) => {
              this.updateTransactionHash(hash);
            })
            .then(() => {
              this.$set(this.processedStep, step, true);
              this.saveState();
            })
            .finally(() => this.$set(this.processingStep, step, false));
        } else if (step === 6) {
          this.saveState();
        }
      } catch (e) {
        this.walletUtils.lockBrowserWallet();
        console.debug('proceedStep method - error', e);
        this.$set(this.processingStep, step, false);
        this.error = `Error during contract deployment: ${e}`;
      }
    },
    clearState() {
      localStorage.removeItem(`exo-wallet-contract-deployment-${this.networkId}`);
    },
    loadState() {
      this.contractAddressByStep = {};
      this.processedStep = {};
      this.transactionHashByStep = {};
      this.step = 1;
      if (localStorage.getItem(`exo-wallet-contract-deployment-${this.networkId}`) != null) {
        const storedState = JSON.parse(localStorage.getItem(`exo-wallet-contract-deployment-${this.networkId}`));
        this.contractAddressByStep = storedState.contractAddressByStep;
        this.processedStep = storedState.processedStep;
        this.transactionHashByStep = storedState.transactionHashByStep;
        // To initialize steps
        if (storedState.step > 0) {
          this.initializeStepsFromStorage(1, storedState.step).then(() => {
            this.step = storedState.step;
          });
        }
      }
    },
    initializeStepsFromStorage(step, maxStep) {
      if (this.transactionHashByStep[step] && ((step < 4 && !this.contractAddressByStep[step]) || !this.processedStep[step])) {
        this.$set(this.processingStep, step, true);
        const thiss = this;
        this.walletUtils.watchTransactionStatus(this.transactionHashByStep[step], (receipt) => {
          if (!receipt || !receipt.status) {
            thiss.error = 'Error processing contract deployment';
            return;
          }
          if (step < 4) {
            if (receipt.contractAddress) {
              thiss.$set(thiss.contractAddressByStep, step, receipt.contractAddress);
            } else {
              thiss.error = 'Error processing contract deployment, not associated contract address in transaction receipt';
              return;
            }
          }
          thiss.$set(thiss.processedStep, step, true);
          thiss.$set(thiss.processingStep, step, false);
          thiss.initializeStep(step).then(() => {
            thiss.saveState();
            if (step < maxStep) {
              return thiss.initializeStepsFromStorage(step + 1, maxStep);
            }
          });
        });
      } else if (this.transactionHashByStep[step] && this.processedStep[step]) {
        return this.initializeStep(step).then(() => {
          if (step < maxStep) {
            return this.initializeStepsFromStorage(step + 1, maxStep);
          }
        });
      }
      return Promise.resolve(false);
    },
    saveState() {
      localStorage.setItem(
        `exo-wallet-contract-deployment-${this.networkId}`,
        JSON.stringify({
          step: this.step,
          processedStep: this.processedStep,
          transactionHashByStep: this.transactionHashByStep,
          contractAddressByStep: this.contractAddressByStep,
        })
      );
    },
    updateTransactionHash(hash) {
      this.$set(this.transactionHashByStep, this.step, hash);
      this.$set(this.processingStep, this.step, true);
      this.saveState();
    },
    finishInstallation() {
      const contractAddress = this.contractAddressByStep[3];
      const contractDetails = {
        networkId: this.networkId,
        address: contractAddress,
        isContract: true,
      };

      return this.tokenUtils.retrieveContractDetails(this.account, contractDetails, true).then(() => {
        this.$set(this.processingStep, this.step, true);
        // Save conract address to display for all users
        return this.tokenUtils.saveContractAddressAsDefault({
          networkId: this.networkId,
          address: contractAddress,
          isContract: true,
          name: contractDetails.name,
          symbol: contractDetails.symbol,
          decimals: contractDetails.decimals,
        })
          .then((resp) => {
            if (resp && resp.ok) {
              this.$emit('list-updated', contractAddress);
              this.createNewToken = false;
              this.clearState();
            } else {
              this.loading = false;
              this.error = `Contract deployed, but an error occurred while saving it as default contract to display for all users`;
            }
          })
          .catch((e) => {
            console.debug('saveContractAddressAsDefault method - error', e);
            this.error = `An error occurred while saving it as default contract to display for all users: ${e}`;
          })
          .finally(() => this.$set(this.processingStep, this.step, false));
      });
    },
  },
};
</script>
