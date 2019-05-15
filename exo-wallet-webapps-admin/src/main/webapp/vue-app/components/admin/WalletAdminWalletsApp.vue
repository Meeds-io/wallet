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
              <strong>Wallet accounts administration</strong>
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

          <v-tabs v-model="selectedTab" grow>
            <v-tabs-slider color="primary" />
            <v-tab
              key="wallets"
              href="#wallets">
              Wallets
            </v-tab>
            <v-tab
              v-if="isAdmin"
              key="adminAccount"
              href="#adminAccount">
              Admin account
            </v-tab>
          </v-tabs>

          <v-tabs-items v-model="selectedTab">
            <v-tab-item
              id="wallets"
              value="wallets">
              <wallets-tab
                ref="walletsTab"
                :network-id="networkId"
                :wallet-address="walletAddress"
                :loading="loading"
                :fiat-symbol="fiatSymbol"
                :refresh-index="refreshIndex"
                :address-etherscan-link="addressEtherscanLink"
                :principal-account-address="principalAccountAddress"
                :principal-contract="principalContract"
                :is-admin="isAdmin"
                @pending="pendingTransaction"
                @wallets-loaded="wallets = $event" />
            </v-tab-item>
            <v-tab-item
              v-if="isAdmin"
              id="adminAccount"
              value="adminAccount">
              <admin-account-tab
                ref="adminAccountTab"
                :network-id="networkId"
                :loading="loading"
                :wallet-address="walletAddress"
                :principal-contract="principalContract" />
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
import WalletsTab from './wallets/WalletAdminWalletsTab.vue';
import AdminAccountTab from './wallets/WalletAdminAccountTab.vue';

export default {
  components: {
    WalletsTab,
    AdminAccountTab,
  },
  data() {
    return {
      loading: false,
      selectedTab: 'wallets',
      fiatSymbol: '$',
      walletAddress: null,
      refreshIndex: 1,
      principalContract: null,
      principalAccountAddress: null,
      networkId: null,
      isAdmin: null,
      addressEtherscanLink: null,
      contracts: [],
      wallets: [],
      anchor: document.URL.indexOf('#') >= 0 ? document.URL.split('#')[1] : null,
    };
  },
  created() {
    this.init()
      .then(() => (this.addressEtherscanLink = this.walletUtils.getAddressEtherscanlink(this.networkId)));
  },
  methods: {
    init() {
      this.loading = true;
      this.showAddContractModal = false;
      this.forceUpdate();
      this.selectedOverviewAccounts = [];
      this.error = null;

      return this.walletUtils.initSettings()
        .then(() => {
          if (!window.walletSettings) {
            this.forceUpdate();
            throw new Error('Wallet settings are empty for current user');
          }
          this.fiatSymbol = window.walletSettings.fiatSymbol || '$';
          this.networkId = window.walletSettings.defaultNetworkId;
          this.walletAddress = (window.walletSettings.userPreferences && window.walletSettings.userPreferences.walletAddress) || window.walletSettings.detectedMetamaskAccount;
          this.isAdmin = window.walletSettings.isAdmin;
          if (window.walletSettings.defaultPrincipalAccount && window.walletSettings.defaultPrincipalAccount.indexOf('0x') === 0) {
            this.principalAccountAddress = window.walletSettings.defaultPrincipalAccount;
          }
        })
        .then(() => this.walletUtils.initWeb3(false, true))
        .catch((error) => {
          if (String(error).indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug('Error connecting to network', error);
            this.error = 'Error connecting to network';
          } else {
            this.error = 'Please configure your wallet';
            throw error;
          }
        })
        .then(() => this.walletUtils.getWallets())
        .then((wallets) => {
          this.wallets = wallets;

          if (this.principalAccountAddress) {
            return this.tokenUtils.retrieveContractDetails(this.walletAddress, {address: this.principalAccountAddress, networkId: this.networkId}, true)
          }
        })
        .then((contractDetails) => {
          this.principalContract = contractDetails;
        })
        .then(() => this.$refs.adminAccountTab && this.$refs.adminAccountTab.init())
        .then(() => this.$refs && this.$refs.walletsTab && this.$refs.walletsTab.init(true))
        .catch((error) => {
          if (String(error).indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug(error);
            if (!this.error) {
              this.error = String(error);
            }
          } else {
            this.error = 'Please configure your wallet';
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
    pendingTransaction(transaction) {
      const recipient = transaction.to.toLowerCase();
      const wallet = this.wallets.find((wallet) => wallet && wallet.address && wallet.address === recipient);
      if (wallet) {
        if (transaction.contractAddress) {
          if(!transaction.contractMethodName || transaction.contractMethodName === 'transfer'  || transaction.contractMethodName === 'transferFrom' || transaction.contractMethodName === 'approve') {
            this.$set(wallet, 'loadingBalancePrincipal', true);
          }
          this.watchPendingTransaction(transaction, this.principalContract);
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
              contractDetails.networkId = this.networkId;
              return this.tokenUtils.saveContractAddressOnServer(contractDetails).then(() => this.init());
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
          // Wait for Block synchronization with Metamask
          setTimeout(() => {
            if(wallet && thiss.$refs.walletsTab) {
              thiss.$refs.walletsTab.refreshWallet(wallet);
            }
          }, 2000);
        }
      });
    },
    forceUpdate() {
      this.refreshIndex++;
      this.$forceUpdate();
    },
  },
};
</script>
