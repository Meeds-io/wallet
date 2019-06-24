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
              <strong>Wallet contracts</strong>
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
            class="mb-3"
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
                Loading ...
                <v-progress-linear
                  indeterminate
                  color="white"
                  class="mb-0" />
              </v-card-text>
            </v-card>
          </v-dialog>

          <contracts-tab
            ref="contractsTab"
            :network-id="networkId"
            :wallet-address="walletAddress"
            :loading="loading"
            :fiat-symbol="fiatSymbol"
            :address-etherscan-link="addressEtherscanLink"
            :token-etherscan-link="tokenEtherscanLink"
            :is-admin="isAdmin"
            @pending-transaction="watchPendingTransaction" />
        </v-flex>
      </v-layout>
      <div id="walletDialogsParent"></div>
    </main>
  </v-app>
</template>

<script>
import ContractsTab from './contracts/WalletAdminContractsTab.vue';

export default {
  components: {
    ContractsTab,
  },
  data() {
    return {
      loading: false,
      isAdmin: false,
      fiatSymbol: '$',
      refreshIndex: 1,
      walletAddress: null,
      networkId: null,
      tokenEtherscanLink: null,
      addressEtherscanLink: null,
    };
  },
  created() {
    this.init()
      .then(() => (this.tokenEtherscanLink = this.walletUtils.getTokenEtherscanlink()))
      .then(() => (this.addressEtherscanLink = this.walletUtils.getAddressEtherscanlink(this.networkId)));
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
          throw new Error('Wallet settings are empty for current user');
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
          this.error = 'Error connecting to network';
        } else {
          this.error = 'Please configure your wallet';
          throw error;
        }
      })
      .then(() => this.walletUtils.getWallets())
      .then((wallets) => {
        this.wallets = wallets;

        this.contractDetails = window.walletSettings.contractDetail;
        if (this.contractDetails) {
          return this.tokenUtils.retrieveContractDetails(this.walletAddress, this.contractDetails, true);
        }
      })
      .then(() => this.$refs.walletSetup && this.$refs.walletSetup.init())
      .then(() => this.$refs.contractsTab && this.$refs.contractsTab.init())
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
  },
};
</script>
