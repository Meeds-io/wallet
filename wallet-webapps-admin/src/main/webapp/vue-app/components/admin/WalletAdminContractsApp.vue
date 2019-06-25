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
              <strong>Wallet contract</strong>
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

          <wallet-setup
            ref="walletSetup"
            :wallet-address="walletAddress"
            :refresh-index="refreshIndex"
            :loading="loading"
            is-administration
            @refresh="init()"
            @error="error = $event" />

          <contract-detail
            v-if="contractDetails"
            ref="contractDetail"
            :wallet-address="walletAddress"
            :contract-details="contractDetails"
            :fiat-symbol="fiatSymbol"
            :wallets="wallets"
            :address-etherscan-link="addressEtherscanLink"
            @back="back()"
            @pending-transaction="watchPendingTransaction" />
        </v-flex>
      </v-layout>
      <div id="walletDialogsParent"></div>
    </main>
  </v-app>
</template>

<script>
import ContractDetail from './contracts/WalletAdminContractDetail.vue';

export default {
  components: {
    ContractDetail,
  },
  data() {
    return {
      loading: false,
      fiatSymbol: '$',
      refreshIndex: 1,
      walletAddress: null,
      tokenEtherscanLink: null,
      addressEtherscanLink: null,
    };
  },
  created() {
    this.init()
      .then(() => (this.tokenEtherscanLink = this.walletUtils.getTokenEtherscanlink()))
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
          throw new Error('Wallet settings are empty for current user');
        }
        this.fiatSymbol = window.walletSettings.fiatSymbol || '$';
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
        return this.tokenUtils.getContractDetails(this.walletAddress, true);
      })
      .then(contractDetails => this.contractDetails = contractDetails)
      .then(() => this.$refs.walletSetup && this.$refs.walletSetup.init())
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
    watchPendingTransaction(transaction) {
      const thiss = this;
      this.walletUtils.watchTransactionStatus(transaction.hash, (receipt) => {
        if (receipt && receipt.status) {
          const wallet = thiss.wallets && thiss.wallets.find((wallet) => wallet && wallet.address && transaction.to && wallet.address.toLowerCase() === transaction.to.toLowerCase());
          if (transaction.contractMethodName === 'transferOwnership') {
            if (this.contractDetails && this.contractDetails.isContract && this.contractDetails.address && transaction.contractAddress && this.contractDetails.address.toLowerCase() === transaction.contractAddress.toLowerCase()) {
              this.$set(this.contractDetails, 'owner', transaction.to);
              return this.tokenUtils.refreshContractOnServer().then(() => this.init());
            }
          } else if (transaction.contractMethodName === 'addAdmin' || transaction.contractMethodName === 'removeAdmin') {
            if (wallet) {
              this.contractDetails.contract.methods
                .getAdminLevel(wallet.address)
                .call()
                .then((level) => {
                  if (!wallet.accountAdminLevel) {
                    wallet.accountAdminLevel = {};
                  }
                  level = Number(level);
                  thiss.$set(wallet.accountAdminLevel, this.contractDetails.address, level ? level : 'not admin');
                  thiss.$nextTick(() => thiss.forceUpdate());
                });
            }
          } else if (transaction.contractMethodName === 'unPause' || transaction.contractMethodName === 'pause') {
            thiss.$set(this.contractDetails, 'isPaused', transaction.contractMethodName === 'pause' ? true : false);
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
