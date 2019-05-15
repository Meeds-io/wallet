<template>
  <v-dialog
    v-model="dialog"
    attach="#walletDialogsParent"
    content-class="uiPopup with-overflow not-draggable"
    class="walletSettingsModal"
    width="700px"
    max-width="100vw"
    persistent>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
            Preferences
          </span>
      </div>
      <v-card-text v-if="loading || appLoading" class="text-xs-center">
        <v-progress-circular
          color="primary"
          class="mb-2"
          indeterminate />
      </v-card-text>
      <v-card-text v-show="!loading && !appLoading">
        <div v-if="error && !loading" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div>
        <v-flex>
          <v-tabs
            ref="settingsTabs"
            v-model="selectedTab"
            class="pl-3 pr-3">
            <v-tabs-slider />
            <v-tab
              v-if="!isSpace"
              key="display"
              href="#display">
              Display
            </v-tab>
            <v-tab
              key="security"
              href="#security">
              Security
            </v-tab>
            <v-tab
              v-if="!isSpace"
              key="advanced"
              href="#advanced">
              Advanced
            </v-tab>
            <v-tab
              v-if="walletAddress && selectedTab === 'keys'"
              key="keys"
              href="#keys">
              Manage keys
            </v-tab>
          </v-tabs>
          <v-tabs-items v-model="selectedTab">
            <v-tab-item
              v-if="!isSpace"
              id="display"
              value="display">
              <v-card>
                <v-card-text>
                  <div>
                    <div id="selectedCurrencyParent" class="selectBoxVuetifyParent">
                      <v-combobox
                        v-model="selectedCurrency"
                        :items="currencies"
                        label="Select fiat currency used to display ether amounts conversion" />
                    </div>
                  </div>
                  <div>
                    <div id="selectedOverviewAccountsParent" class="selectBoxVuetifyParent">
                      <v-combobox
                        v-model="selectedOverviewAccounts"
                        :items="accountsList"
                        label="List of currencies to use (by order)"
                        placeholder="List of contracts, ether and fiat to use in wallet application (by order)"
                        multiple
                        deletable-chips
                        chips />
                    </div>
                  </div>
                </v-card-text>
              </v-card>
            </v-tab-item>
            <v-tab-item
              id="security"
              value="security">
              <wallet-settings-security-tab
                ref="securityTab"
                :wallet-address="walletAddress"
                :is-space="isSpace"
                @settings-changed="$emit('settings-changed')"
                @manage-keys="selectedTab = 'keys'" />
            </v-tab-item>
            <v-tab-item
              v-if="!isSpace"
              id="advanced"
              value="advanced">
              <v-card>
                <v-card-text>
                  <div v-if="!isSpace">
                    <span>
                      Maximum transaction fee
                    </span>
                    <v-slider
                      v-model="defaultGas"
                      :label="`${defaultGas}${defaulGasPriceFiat ? ' (' + defaulGasPriceFiat + ' ' + fiatSymbol + ')' : ''}`"
                      :max="200000"
                      :min="35000"
                      :step="1000"
                      type="number" />
                  </div>
                  <div>
                    <v-switch
                      v-if="!isSpace"
                      v-model="enableDelegation"
                      label="Enable token delegation operations" />
                  </div>
                </v-card-text>
              </v-card>
            </v-tab-item>
            <v-tab-item
              v-if="walletAddress"
              id="keys"
              value="keys">
              <v-card>
                <v-card-text class="pb-0">
                  <qr-code
                    ref="qrCode"
                    :to="walletAddress"
                    title="Address QR Code"
                    information="You can send this Wallet address or QR code to other users to send you ether and tokens" />
                  <div class="text-xs-center">
                    <wallet-address :value="walletAddress" :allow-edit="false" />
                  </div>
                </v-card-text>
                <v-card-text v-if="browserWalletExists" class="text-xs-center pb-0">
                  <wallet-backup-modal
                    ref="walletBackupModal"
                    :display-complete-message="false"
                    @copied="
                      $emit('copied');
                      refreshFromSettings();
                    " />
                </v-card-text>
                <v-card-text class="text-xs-center">
                  <wallet-import-key-modal
                    ref="walletImportKeyModal"
                    :is-space="isSpace"
                    :wallet-address="walletAddress"
                    @configured="
                      $emit('settings-changed');
                      refreshFromSettings();
                    " />
                </v-card-text>
              </v-card>
            </v-tab-item>
          </v-tabs-items>
        </v-flex>
      </v-card-text>
      <v-divider />
      <v-card-actions>
        <v-spacer />
        <button
          v-if="!loading && !isSpace && (selectedTab === 'display' || selectedTab === 'advanced')"
          :disabled="loading"
          :loading="loading"
          class="btn btn-primary mr-1"
          @click="savePreferences">
          Save
        </button>
        <button
          :disabled="loading"
          class="btn"
          @click="dialog = false">
          Close
        </button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import QrCode from './QRCode.vue';
import WalletAddress from './WalletAddress.vue';
import WalletSettingsSecurityTab from './WalletSettingsSecurityTab.vue';
import WalletBackupModal from './WalletBackupModal.vue';
import WalletImportKeyModal from './WalletImportKeyModal.vue';

import {setDraggable, gasToFiat, enableMetamask, disableMetamask, removeServerSideBackup} from '../js/WalletUtils.js';
import {FIAT_CURRENCIES} from '../js/Constants.js';

export default {
  components: {
    QrCode,
    WalletAddress,
    WalletSettingsSecurityTab,
    WalletBackupModal,
    WalletImportKeyModal,
  },
  props: {
    title: {
      type: String,
      default: function() {
        return null;
      },
    },
    appLoading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    open: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    displayResetOption: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      },
    },
    overviewAccounts: {
      type: Array,
      default: function() {
        return [];
      },
    },
    principalAccountAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
  },
  data() {
    return {
      loading: false,
      dialog: false,
      error: null,
      walletAddress: null,
      selectedTab: null,
      selectedCurrency: FIAT_CURRENCIES['usd'],
      currencies: [],
      defaultGas: 0,
      accountType: 0,
      enableDelegation: true,
      autoGeneratedPassword: false,
      hasKeyOnServerSide: false,
      browserWalletExists: false,
      backedUp: false,
      defaulGasPriceFiat: 0,
      selectedOverviewAccounts: [],
      selectedPrincipalAccount: null,
      etherAccount: {text: 'Ether', value: 'ether', disabled: false},
      fiatAccount: {text: 'Fiat ($, €...)', value: 'fiat', disabled: false},
      accountsList: [],
    };
  },
  computed: {
    displayWalletResetOption() {
      return this.displayResetOption && !this.useMetamaskChoice;
    },
    useMetamaskChoice() {
      return this.accountType === '2';
    },
  },
  watch: {
    walletAddress() {
      if (this.walletAddress) {
        this.$nextTick(() => {
          this.$refs.qrCode.computeCanvas();
        });
      }
    },
    appLoading() {
      if (!this.appLoading) {
        this.refreshFromSettings();
      }
    },
    open() {
      if (this.open) {
        this.error = null;
        this.walletAddress = window.walletSettings.userPreferences.walletAddress;
        this.defaultGas = window.walletSettings.userPreferences.defaultGas ? window.walletSettings.userPreferences.defaultGas : 35000;
        this.defaulGasPriceFiat = this.defaulGasPriceFiat || gasToFiat(this.defaultGas, window.walletSettings.maxGasPriceEther);
        this.accountType = window.walletSettings.userPreferences.useMetamask ? '2' : '1';
        this.enableDelegation = window.walletSettings.userPreferences.enableDelegation;
        if (window.walletSettings.userPreferences.currency) {
          this.selectedCurrency = FIAT_CURRENCIES[window.walletSettings.userPreferences.currency];
        }

        this.accountsList = [];
        this.selectedOverviewAccounts = [];
        this.selectedPrincipalAccount = null;

        this.accountsList.push(Object.assign({}, this.etherAccount), Object.assign({}, this.fiatAccount));
        if (this.accountsDetails) {
          Object.keys(this.accountsDetails).forEach((key) => {
            const accountDetails = this.accountsDetails[key];
            if (accountDetails.isContract) {
              this.accountsList.push({text: accountDetails.name, value: accountDetails.address, disabled: false});
            }
          });
        }

        if(this.overviewAccounts) {
          this.overviewAccounts.forEach((selectedValue) => {
            const selectedObject = this.getOverviewAccountObject(selectedValue);
            if (selectedObject) {
              this.selectedOverviewAccounts.push(selectedObject);
            }
          });
        }

        this.selectedPrincipalAccount = this.getOverviewAccountObject(this.principalAccountAddress);

        // Workaround to display slider on first popin open
        this.$refs.settingsTabs.callSlider();

        this.dialog = true;
        this.$nextTick(() => {
          this.refreshFromSettings();
          setDraggable();
        });
      }
    },
    selectedPrincipalAccount() {
      if (this.selectedPrincipalAccount) {
        this.selectedOverviewAccounts.forEach((account) => {
          account.disabled = false;
        });

        this.accountsList.forEach((account, index) => {
          if (this.selectedPrincipalAccount.value === account.value) {
            account.disabled = true;
            const accountIndex = this.selectedOverviewAccounts.findIndex((foundSelectedAccount) => foundSelectedAccount.value === account.value);
            if (accountIndex >= 0) {
              this.selectedOverviewAccounts.splice(accountIndex, 1);
            }
            this.selectedOverviewAccounts.unshift(account);
          } else {
            account.disabled = false;
          }
        });
        this.$forceUpdate();
      }
    },
    dialog() {
      if (!this.dialog) {
        this.$emit('close');
      }
    },
    defaultGas() {
      this.defaulGasPriceFiat = gasToFiat(this.defaultGas, window.walletSettings.maxGasPriceEther);
    },
  },
  created() {
    Object.keys(FIAT_CURRENCIES).forEach((key) => this.currencies.push(FIAT_CURRENCIES[key]));
  },
  methods: {
    refreshFromSettings() {
      this.hasKeyOnServerSide = window.walletSettings.userPreferences.hasKeyOnServerSide;
      this.autoGeneratedPassword = window.walletSettings.userPreferences.autoGenerated;
      this.backedUp = window.walletSettings.userPreferences.backedUp;
      this.browserWalletExists = window.walletSettings.browserWalletExists;
      if (this.$refs.securityTab) {
        this.$refs.securityTab.init();
      }
    },
    savePreferences() {
      try {
        if (!this.isSpace) {
          this.loading = true;
          fetch('/portal/rest/wallet/api/account/savePreferences', {
            method: 'POST',
            headers: {
              Accept: 'application/json',
              'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify({
              defaultGas: this.defaultGas,
              currency: this.selectedCurrency.value,
              principalAccount: this.selectedPrincipalAccount ? this.selectedPrincipalAccount.value : null,
              overviewAccounts: this.selectedOverviewAccounts ? this.selectedOverviewAccounts.map((item) => item.value) : null,
              enableDelegation: this.enableDelegation,
            }),
          })
            .then((resp) => {
              if (resp && resp.ok) {
                window.walletSettings.userPreferences.defaultGas = this.defaultGas;
                window.walletSettings.userPreferences.currency = this.selectedCurrency.value;

                this.$emit('settings-changed', {
                  defaultGas: this.defaultGas,
                  currency: this.selectedCurrency.value,
                });
                this.dialog = false;
              } else {
                this.error = 'Error saving preferences';
              }
              this.loading = false;
            })
            .catch((e) => {
              console.debug('savePreferences method - error', e);
              this.error = `Error while proceeding: ${e}`;
              this.loading = false;
            });
        }
      } catch (e) {
        console.debug('savePreferences method - error', e);
        this.loading = false;
        this.error = `Error while proceeding: ${e}`;
        this.$emit('end-loading');
      }
    },
    changeAccountType() {
      if (this.useMetamaskChoice) {
        enableMetamask(this.isSpace);
      } else {
        disableMetamask(this.isSpace);
      }
      this.$emit('settings-changed');
    },
    removeServerSideBackup() {
      return removeServerSideBackup(this.walletAddress)
        .then((removed) => {
          if (removed) {
            window.walletSettings.userPreferences.hasKeyOnServerSide = false;
            this.refreshFromSettings();
          }
        })
        .catch(e => {
          this.error = String(e);
        });
    },
    getOverviewAccountObject(selectedValue) {
      if (selectedValue === 'fiat') {
        return Object.assign({}, this.fiatAccount);
      } else if (selectedValue === 'ether') {
        return Object.assign({}, this.etherAccount);
      } else if (this.accountsList && this.accountsList.length) {
        const selectedContractAddress = this.accountsList.findIndex((contract) => contract.value === selectedValue);
        if (selectedContractAddress >= 0) {
          const contract = this.accountsList[selectedContractAddress];
          if (!contract.error) {
            return {text: contract.text, value: contract.value, disabled: false};
          }
        }
      }
    },
  },
};
</script>
