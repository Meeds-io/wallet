<template>
  <v-app
    id="WalletAdminApp"
    color="transaprent"
    class="VuetifyApp"
    flat>
    <main>
      <v-layout column>
        <v-flex>
          <v-card class="applicationToolbar mb-3" flat>
            <v-flex class="pl-3 pr-3 pt-2 pb-2">
              <strong>{{ $t('exoplatform.wallet.title.walletAdministration') }}</strong>
            </v-flex>
          </v-card>
        </v-flex>
        <v-flex class="white text-center" flat>
          <div v-if="error && !loading" class="alert alert-error v-content">
            <i class="uiIconError"></i>{{ error }}
          </div>

          <v-dialog
            v-model="loading"
            attach="#walletDialogsParent"
            persistent
            hide-overlay
            width="300">
            <v-card color="primary" dark>
              <v-card-text>
                {{ $t('exoplatform.wallet.label.loading') }} ...
                <v-progress-linear
                  indeterminate
                  color="white"
                  class="mb-0" />
              </v-card-text>
            </v-card>
          </v-dialog>

          <v-tabs v-model="selectedTab" grow>
            <v-tabs-slider color="primary" />
            <v-tab
              key="wallets"
              href="#wallets">
              {{ $t('exoplatform.wallet.title.walletAdministrationTab') }}
            </v-tab>
            <v-tab
              key="funds"
              href="#funds">
              {{ $t('exoplatform.wallet.title.initialFundsTab') }}
            </v-tab>
            <v-tab
              v-if="contractDetails"
              key="contract"
              href="#contract">
              {{ adminLevel >= 4 ? $t('exoplatform.wallet.title.contractTab') : $t('exoplatform.wallet.title.transactionHistoryTab') }}
            </v-tab>
          </v-tabs>

          <v-tabs-items v-model="selectedTab">
            <v-tab-item
              id="wallets"
              value="wallets"
              eager>
              <wallets-tab
                ref="walletsTab"
                :wallet-address="walletAddress"
                :loading="loading"
                :fiat-symbol="fiatSymbol"
                :refresh-index="refreshIndex"
                :address-etherscan-link="addressEtherscanLink"
                :contract-details="contractDetails"
                @pending="$refs && $refs.contractDetail && $refs.contractDetail.refreshTransactionList()"
                @wallets-loaded="wallets = $event" />
            </v-tab-item>
            <v-tab-item
              id="funds"
              value="funds"
              eager>
              <initial-funds-tab
                ref="fundsTab"
                :loading="loading"
                :settings="settings"
                :contract-details="contractDetails"
                @saved="refreshSettings" />
            </v-tab-item>
            <v-tab-item
              v-if="contractDetails"
              id="contract"
              value="contract"
              eager>
              <contract-tab
                ref="contractDetail"
                :wallet-address="walletAddress"
                :user-wallet="wallet"
                :wallets="wallets"
                :contract-details="contractDetails"
                :admin-level="adminLevel"
                :fiat-symbol="fiatSymbol"
                :address-etherscan-link="addressEtherscanLink"
                @back="back()"
                @success="reloadContract"
                @pending-transaction="pendingTransaction" />
            </v-tab-item>
          </v-tabs-items>
        </v-flex>
      </v-layout>
      <div id="walletDialogsParent"></div>
    </main>
  </v-app>
</template>

<script>
import WalletsTab from './wallets/WalletAdminWalletsTab.vue';
import InitialFundsTab from './settings/WalletAdminInitialFundsTab.vue';
import ContractTab from './contracts/WalletAdminContractTab.vue';

export default {
  components: {
    WalletsTab,
    InitialFundsTab,
    ContractTab,
  },
  data() {
    return {
      loading: false,
      selectedTab: 'wallets',
      fiatSymbol: '$',
      settings: null,
      wallet: null,
      refreshIndex: 1,
      contractDetails: null,
      addressEtherscanLink: null,
      wallets: [],
      anchor: document.URL.indexOf('#') >= 0 ? document.URL.split('#')[1] : null,
    };
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address && this.wallet.address.toLowerCase();
    },
    adminLevel() {
      return this.wallet && this.wallet.adminLevel;
    },
  },
  created() {
    document.addEventListener('exo.wallet.modified', this.walletUpdated);
    document.addEventListener('exo.contract.modified', this.reloadContract);
    this.init()
      .then(() => {
        this.addressEtherscanLink = this.walletUtils.getAddressEtherscanlink()
      });
  },
  methods: {
    init() {
      this.loading = true;
      this.showAddContractModal = false;
      this.forceUpdate();
      this.error = null;

      return this.walletUtils.initSettings(false, true, true)
        .then(() => {
          if (!window.walletSettings) {
            this.forceUpdate();
            throw new Error(this.$t('exoplatform.wallet.error.emptySettings'));
          }
          this.settings = window.walletSettings;
          this.fiatSymbol = window.walletSettings.fiatSymbol || '$';
          this.wallet = window.walletSettings.wallet;
        })
        .then(() => this.walletUtils.initWeb3(false, true))
        .then(() => {
          this.walletAddress = this.wallet.address;
        })
        .catch((error) => {
          if (String(error).indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug('Error connecting to network', error);
            this.error = this.$t('exoplatform.wallet.warning.networkConnectionFailure');
          }
        })
        .then(() => {
          if (this.contractDetails) {
            return this.tokenUtils.reloadContractDetails(this.walletAddress).then(result => this.contractDetails = result);
          } else {
            this.contractDetails = this.tokenUtils.getContractDetails(this.walletAddress);
          }
        })
        .then(() => this.$refs.walletsTab.init())
        .catch((error) => {
          if (String(error).indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug(error);
            if (!this.error) {
              this.error = String(error);
            }
          } else {
            this.error = this.$t('exoplatform.wallet.warning.walletNotConfigured');
          }
        })
        .catch((e) => {
          console.debug('init method - error', e);
          this.error = String(e);
        })
        .finally(() => {
          this.loading = false;
          this.forceUpdate();
        });
    },
    walletUpdated(event) {
      if(event && event.detail && event.detail.string) {
        const updatedWalletAddress = event.detail.string.toLowerCase();
        if(this.walletAddress === updatedWalletAddress) {
          this.refreshWallet(this.wallet);
        }
        const updatedWallet = this.wallets.find(wallet => wallet && wallet.address && wallet.address.toLowerCase() === updatedWalletAddress);
        if (updatedWallet) {
          this.refreshWallet(updatedWallet);
        }
      }
    },
    reloadContract() {
      return this.tokenUtils.reloadContractDetails(this.walletAddress).then(result => this.contractDetails = result);
    },
    pendingTransaction(transaction) {
      const recipient = transaction.to.toLowerCase();
      const wallet = this.wallets.find((wallet) => wallet && wallet.address && wallet.address === recipient);
      if (wallet) {
        if (transaction.contractAddress) {
          this.$set(wallet, 'loadingTokenBalance', true);
          this.walletUtils.watchTransactionStatus(transaction.hash, () => {
            return this.refreshWallet(wallet).then(() => {
              this.$set(wallet, 'loadingTokenBalance', false);
            });
          });
        } else {
          this.$set(wallet, 'loadingBalance', true);
          this.walletUtils.watchTransactionStatus(transaction.hash, () => {
            return this.refreshWallet(wallet).then(() => {
              this.$set(wallet, 'loadingBalance', false);
            });
          });
        }
      }
    },
    refreshWallet(wallet) {
      return this.addressRegistry.refreshWallet(wallet).then(() => {
        wallet.fiatBalance = wallet.fiatBalance || (wallet.etherBalance && this.walletUtils.etherToFiat(wallet.etherBalance))
      });
    },
    forceUpdate() {
      this.refreshIndex++;
      this.$forceUpdate();
    },
    refreshSettings() {
      this.init();
    },
  },
};
</script>
