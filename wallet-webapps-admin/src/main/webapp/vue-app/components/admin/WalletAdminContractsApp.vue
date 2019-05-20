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
            :wallet-address="originalWalletAddress"
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
      originalWalletAddress: null,
      walletAddress: null,
      networkId: null,
      tokenEtherscanLink: null,
      addressEtherscanLink: null,
    };
  },
  created() {
    this.init()
      .then(() => (this.tokenEtherscanLink = this.walletUtils.getTokenEtherscanlink(this.networkId)))
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
          this.isAdmin = window.walletSettings.isAdmin;
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
        .then(() => {
          this.walletAddress = window.localWeb3 && window.localWeb3.eth.defaultAccount && window.localWeb3.eth.defaultAccount.toLowerCase();
          this.originalWalletAddress = window.walletSettings.userPreferences.walletAddress;
          this.networkId = window.walletSettings.currentNetworkId;
          this.fiatSymbol = (window.walletSettings && window.walletSettings.fiatSymbol) || '$';
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
        .finally(() => {
          this.loading = false;
          this.forceUpdate();
        });
    },
    watchPendingTransaction(transaction, contractDetails) {
      const thiss = this;
      this.walletUtils.watchTransactionStatus(transaction.hash, (receipt) => {
        if (receipt && receipt.status) {
          if (transaction.contractMethodName === 'transferOwnership') {
            if (contractDetails && contractDetails.isContract && contractDetails.address && transaction.contractAddress && contractDetails.address.toLowerCase() === transaction.contractAddress.toLowerCase()) {
              this.$set(contractDetails, 'owner', transaction.to);
              contractDetails.networkId = this.networkId;
              return this.tokenUtils.saveContractAddressOnServer(contractDetails).then(() => this.init());
            }
          } else if (transaction.contractMethodName === 'unPause' || transaction.contractMethodName === 'pause') {
            thiss.$set(contractDetails, 'isPaused', transaction.contractMethodName === 'pause' ? true : false);
            thiss.$nextTick(thiss.forceUpdate);
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
