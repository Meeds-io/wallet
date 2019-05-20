<template>
  <v-app
    id="WalletAdminApp"
    color="transaprent"
    flat>
    <main>
      <v-layout column>
        <v-flex>
          <v-card class="applicationToolbar mb-3" flat>
            <v-flex class="pl-3 pr-3 pt-2 pb-2">
              <strong>Wallet settings</strong>
            </v-flex>
          </v-card>
        </v-flex>
        <v-flex class="white text-xs-center" flat>
          <div v-if="error && !loading" class="alert alert-error v-content">
            <i class="uiIconError"></i>{{ error }}
          </div>

          <v-dialog
            v-model="loading"
            attach="#walletDialogsParent"
            persistent
            width="300">
            <v-card color="primary" dark>
              <v-card-text>
                Loading ...
                <v-progress-linear
                  indeterminate
                  color="white"
                  class="mb-0" />
              </v-card-text>
            </v-card>
          </v-dialog>

          <v-tabs
            v-model="selectedTab"
            grow>
            <v-tabs-slider color="primary" />
            <v-tab
              key="general"
              href="#general">
              Settings
            </v-tab>
            <v-tab
              key="funds"
              href="#funds">
              Initial accounts funds
            </v-tab>
            <v-tab
              key="network"
              href="#network">
              Network
            </v-tab>
          </v-tabs>

          <v-tabs-items v-model="selectedTab">
            <v-tab-item
              id="general"
              value="general">
              <general-tab
                ref="generalTab"
                :loading="loading"
                @principal-contract-loaded="principalContract = $event"
                @save="saveGlobalSettings" />
            </v-tab-item>

            <v-tab-item
              id="funds"
              value="funds">
              <initial-funds-tab
                ref="fundsTab"
                :loading="loading"
                :principal-contract="principalContract"
                @save="saveGlobalSettings" />
            </v-tab-item>

            <v-tab-item
              id="network"
              value="network">
              <network-tab
                ref="networkTab"
                :network-id="networkId"
                :loading="loading"
                :fiat-symbol="fiatSymbol"
                @save="saveGlobalSettings" />
            </v-tab-item>
          </v-tabs-items>
        </v-flex>
      </v-layout>
      <div id="walletDialogsParent">
      </div>
    </main>
  </v-app>
</template>

<script>
import GeneralTab from './settings/WalletAdminSettingsTab.vue';
import InitialFundsTab from './settings/WalletAdminInitialFundsTab.vue';
import NetworkTab from './settings/WalletAdminNetworkTab.vue';

export default {
  components: {
    GeneralTab,
    InitialFundsTab,
    NetworkTab,
  },
  data() {
    return {
      loading: false,
      selectedTab: 'general',
      fiatSymbol: '$',
    };
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.error = null;
      this.loading = true;
      return this.walletUtils.initSettings()
        .then(() => {
          if (window.walletSettings) {
            this.fiatSymbol = (window.walletSettings && window.walletSettings.fiatSymbol) || '$';
          } else {
            window.walletSettings = {};
          }
          this.$forceUpdate();
        })
        .then(() => this.$refs.generalTab && this.$refs.generalTab.init())
        .then(() => this.$refs.fundsTab && this.$refs.fundsTab.init())
        .then(() => this.$refs.networkTab && this.$refs.networkTab.init())
        .catch((e) => {
          console.debug('init method - error', e);
          this.error = String(e);
        })
        .finally(() => {
          this.loading = false;
          this.$forceUpdate();
        });
    },
    saveGlobalSettings(globalSettings) {
      this.loading = true;
      const defaultInitialFundsMap = {};
      if (!globalSettings.initialFunds) {
        if (window.walletSettings.initialFunds && window.walletSettings.initialFunds.length) {
          window.walletSettings.initialFunds.forEach((initialFund) => {
            defaultInitialFundsMap[initialFund.address] = initialFund.amount;
          });
        }
      }
      const currentGlobalSettings = Object.assign({}, window.walletSettings, {initialFunds: defaultInitialFundsMap});
      const globalSettingsToSave = Object.assign(currentGlobalSettings, globalSettings);
      if(globalSettingsToSave.contractAbi) {
        delete globalSettingsToSave.contractAbi;
      }
      if(globalSettingsToSave.contractBin) {
        delete globalSettingsToSave.contractBin;
      }
      if(globalSettingsToSave.contractBin) {
        delete globalSettingsToSave.contractBin;
      }
      if(globalSettingsToSave.userPreferences) {
        delete globalSettingsToSave.userPreferences;
      }
      delete globalSettingsToSave.walletEnabled;
      delete globalSettingsToSave.admin;

      return fetch('/portal/rest/wallet/api/global-settings/save', {
        method: 'POST',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(globalSettingsToSave),
      })
        .then((resp) => {
          if (resp && resp.ok) {
            return resp.text();
          } else {
            throw new Error('Error saving global settings');
          }
        })
        .then(() => {
          window.setTimeout(() => {
            this.init();
          }, 200);
        })
        .catch((e) => {
          this.loading = false;
          console.debug('fetch global-settings - error', e);
          this.error = 'Error saving global settings';
        });
    },
  },
};
</script>
