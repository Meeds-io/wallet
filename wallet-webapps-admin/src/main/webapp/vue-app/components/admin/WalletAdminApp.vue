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
        <v-flex class="white text-xs-center" flat>
          <div v-if="error && !loading" class="alert alert-error v-content">
            <i class="uiIconError"></i>{{ error }}
          </div>

          <wallet-setup
            ref="walletSetup"
            :wallet-address="walletAddress"
            :refresh-index="refreshIndex"
            :loading="loading"
            is-administration
            @refresh="init()"
            @error="error = $event" />

          <v-dialog
            v-model="loading"
            attach="#walletDialogsParent"
            persistent
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
              {{ contractDetails.adminLevel >= 4 ? $t('exoplatform.wallet.title.contractTab') : $t('exoplatform.wallet.title.transactionHistoryTab') }}
            </v-tab>
          </v-tabs>

          <v-tabs-items v-model="selectedTab">
            <v-tab-item
              id="wallets"
              value="wallets">
              <wallets-tab
                ref="walletsTab"
                :wallet-address="walletAddress"
                :loading="loading"
                :fiat-symbol="fiatSymbol"
                :refresh-index="refreshIndex"
                :address-etherscan-link="addressEtherscanLink"
                :contract-details="contractDetails"
                :is-admin="isAdmin"
                @pending="pendingTransaction"
                @wallets-loaded="wallets = $event" />
            </v-tab-item>
            <v-tab-item
              id="funds"
              value="funds">
              <initial-funds-tab
                ref="fundsTab"
                :loading="loading"
                :contract-details="contractDetails"
                @saved="refreshSettings" />
            </v-tab-item>
            <v-tab-item
              v-if="contractDetails"
              id="contract"
              value="contract">
              <contract-tab
                ref="contractDetail"
                :wallet-address="walletAddress"
                :user-wallet="userWallet"
                :contract-details="contractDetails"
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
      walletAddress: null,
      refreshIndex: 1,
      contractDetails: null,
      isAdmin: null,
      addressEtherscanLink: null,
      contracts: [],
      wallets: [],
      anchor: document.URL.indexOf('#') >= 0 ? document.URL.split('#')[1] : null,
    };
  },
  computed: {
    userWallet() {
      const address = this.walletAddress && this.walletAddress.toLowerCase();
      return this.wallets && this.wallets.find((wallet) => wallet && wallet.address && wallet.address.toLowerCase() === address);
    }
  },
  created() {
    this.init()
      .then(() => (this.addressEtherscanLink = this.walletUtils.getAddressEtherscanlink()));
  },
  methods: {
    init() {
      this.loading = true;
      this.showAddContractModal = false;
      this.forceUpdate();
      this.error = null;

      return this.walletUtils.initSettings()
        .then(() => {
          if (!window.walletSettings) {
            this.forceUpdate();
            throw new Error(this.$t('exoplatform.wallet.error.emptySettings'));
          }
          this.fiatSymbol = window.walletSettings.fiatSymbol || '$';
          this.isAdmin = window.walletSettings.admin;
        })
        .then(() => this.walletUtils.initWeb3(false, true))
        .then(() => {
          this.walletAddress = window.walletSettings.wallet.address;
        })
        .catch((error) => {
          if (String(error).indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug('Error connecting to network', error);
            this.error = this.$t('exoplatform.wallet.warning.networkConnectionFailure');
          } else {
            this.error = this.$t('exoplatform.wallet.warning.walletNotConfigured');
          }
        })
        .then(() => this.reloadContract())
        .then(() => this.$refs.walletSetup && this.$refs.walletSetup.init())
        .then(() => this.$refs && this.$refs.walletsTab && this.$refs.walletsTab.init(true))
        .then(() => this.$refs.fundsTab && this.$refs.fundsTab.init())
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
    reloadContract() {
      return this.tokenUtils.getContractDetails(this.walletAddress, true)
        .then(contractDetails => this.contractDetails = contractDetails);
    },
    pendingTransaction(transaction) {
      const recipient = transaction.to.toLowerCase();
      const wallet = this.wallets.find((wallet) => wallet && wallet.address && wallet.address === recipient);
      if (wallet) {
        if (transaction.contractAddress) {
          if(!transaction.contractMethodName || transaction.contractMethodName === 'transfer'  || transaction.contractMethodName === 'transferFrom' || transaction.contractMethodName === 'approve') {
            this.$set(wallet, 'loadingTokenBalance', true);
          }
          this.watchPendingTransaction(transaction, this.contractDetails);
        } else {
          this.$set(wallet, 'loadingBalance', true);
          this.watchPendingTransaction(transaction);
        }
      } else {
        const contract = this.contracts.find((contract) => contract && contract.address && contract.address.toLowerCase() === recipient.toLowerCase());
        if (contract) {
          this.$set(contract, 'loadingBalance', true);
          if (this.$refs.contractDetail) {
            this.$refs.contractDetail.newTransactionPending(transaction, contract);
          }
        }
      }
    },
    watchPendingTransaction(transaction, contractDetails) {
      const thiss = this;
      this.walletUtils.watchTransactionStatus(transaction.hash, (receipt) => {
        if (receipt && receipt.status) {
          const wallet = thiss.wallets && thiss.wallets.find((wallet) => wallet && wallet.address && transaction.to && wallet.address.toLowerCase() === transaction.to.toLowerCase());
          if (transaction.contractMethodName === 'transferOwnership') {
            if (contractDetails && contractDetails.isContract && contractDetails.address && transaction.contractAddress && contractDetails.address.toLowerCase() === transaction.contractAddress.toLowerCase()) {
              this.$set(contractDetails, 'owner', transaction.to);
              return this.tokenUtils.refreshContractOnServer().then(() => this.init());
            }
          } else if (transaction.contractMethodName === 'addAdmin' || transaction.contractMethodName === 'removeAdmin') {
            if (wallet) {
              contractDetails.contract.methods
                .getAdminLevel(wallet.address)
                .call()
                .then((level) => {
                  if (!wallet.accountAdminLevel) {
                    wallet.accountAdminLevel = {};
                  }
                  level = Number(level);
                  thiss.$set(wallet.accountAdminLevel, contractDetails.address, level ? level : 'not admin');
                  thiss.$nextTick(() => thiss.forceUpdate());
                });
            }
          } else if (transaction.contractMethodName === 'unPause' || transaction.contractMethodName === 'pause') {
            thiss.$set(contractDetails, 'isPaused', transaction.contractMethodName === 'pause' ? true : false);
            thiss.$nextTick(thiss.forceUpdate);
          }
          if(wallet && thiss.$refs.walletsTab) {
            thiss.$refs.walletsTab.refreshWallet(wallet);
          }
        }
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
