<template>
  <v-flex id="walletSummary" class="elevation-0 mr-3">
    <v-card-title
      v-if="walletInitializationStatus === 'NEW' || walletInitializationStatus === 'MODIFIED' || walletInitializationStatus === 'PENDING'"
      primary-title
      class="pb-0">
      <v-spacer />
      <div class="alert alert-info">
        <i class="uiIconInfo"></i>
        Almost done! Your wallet will be ready to use once an administrator approves it.
      </div>
      <v-spacer />
    </v-card-title>
    <v-card-title
      v-else-if="walletInitializationStatus === 'DENIED'"
      primary-title
      class="pb-0">
      <v-spacer />
      <div class="alert alert-info">
        <i class="uiIconInfo"></i>
        Wallet access is denied.
        <button class="btn" @click="requestAccessAuthorization()">
          Request authorization
        </button>
      </div>
      <v-spacer />
    </v-card-title>

    <v-card-title
      v-if="pendingTransactionsCount"
      primary-title
      class="pb-0">
      <v-spacer />
      <v-badge
        color="red"
        right
        title="A transaction is in progress">
        <span slot="badge">
          {{ pendingTransactionsCount }}
        </span>
        <v-progress-circular
          color="primary"
          indeterminate
          size="20" />
      </v-badge>
      <v-spacer />
    </v-card-title>

    <v-container
      v-if="principalAccountDetails"
      fluid
      grid-list-md
      pl-3
      pr-0>
      <v-layout col wrap>
        <v-flex
          md4
          xs12
          text-xs-center>
          <summary-balance :contract-details="principalAccountDetails" />
        </v-flex>
        <v-flex
          offset-md1
          offset-xs0
          md3
          xs6
          pr-0
          pl-0
          text-xs-center>
          <summary-reward
            :contract-details="principalAccountDetails"
            :wallet-address="walletAddress"
            @display-transactions="$emit('display-transactions', $event, null, 'reward')"
            @error="$emit('error', $event)" />
        </v-flex>
        <v-flex
          offset-md1
          offset-xs0
          md3
          xs6
          pr-0
          pl-0
          text-xs-center>
          <summary-transaction
            :contract-details="principalAccountDetails"
            :wallet-address="walletAddress"
            @display-transactions="$emit('display-transactions', $event)"
            @error="$emit('error', $event)" />
        </v-flex>
      </v-layout>
    </v-container>
  </v-flex>
</template>

<script>
import SummaryBalance from './SummaryBalance.vue';
import SummaryReward from './SummaryReward.vue';
import SummaryTransaction from './SummaryTransaction.vue';

export default {
  components: {
    SummaryBalance,
    SummaryReward,
    SummaryTransaction,
  },
  props: {
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
    networkId: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    isMaximized: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    overviewAccounts: {
      type: Array,
      default: function() {
        return [];
      },
    },
    principalAccount: {
      type: String,
      default: function() {
        return null;
      },
    },
    etherBalance: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    totalBalance: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    totalFiatBalance: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return '$';
      },
    },
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      updatePendingTransactionsIndex: 1,
      walletInitializationStatus: null,
      pendingTransactions: {},
      lastTransaction: null,
    };
  },
  computed: {
    disableSendButton() {
      return this.isReadOnly || !this.etherBalance || !Number(this.etherBalance);
    },
    pendingTransactionsCount() {
      return this.updatePendingTransactionsIndex && Object.keys(this.pendingTransactions).length;
    },
    principalAccountDetails() {
      if (this.principalAccount && this.accountsDetails[this.principalAccount]) {
        return this.accountsDetails[this.principalAccount];
      }
      return null;
    },
    overviewAccountsArray() {
      if(!this.overviewAccounts) {
        return [];
      }
      const accountsList = [];
      this.overviewAccounts.forEach((selectedValue) => {
        if (selectedValue !== this.principalAccount) {
          if (selectedValue === 'fiat') {
            const accountDetails = Object.assign({}, this.accountsDetails[this.walletAddress]);
            accountDetails.key = 'fiat';
            accountsList.push(accountDetails);
          } else if (selectedValue === 'ether') {
            const accountDetails = Object.assign({}, this.accountsDetails[this.walletAddress]);
            accountDetails.key = 'ether';
            accountsList.push(accountDetails);
          } else if (this.accountsDetails[selectedValue]) {
            accountsList.push(this.accountsDetails[selectedValue]);
          }
        }
      });

      return accountsList;
    },
  },
  created() {
    this.loadPendingTransactions();
  },
  methods: {
    init(isReadOnly) {
      this.walletInitializationStatus = window.walletSettings && window.walletSettings.userPreferences && window.walletSettings.userPreferences.wallet && window.walletSettings.userPreferences.wallet.initializationState;
    },
    refreshBalance(accountDetails) {
      if (accountDetails && accountDetails.isContract) {
        this.$emit('refresh-token-balance', accountDetails);
      } else {
        this.$emit('refresh-balance');
      }
    },
    requestAccessAuthorization() {
      if(window.walletSettings.userPreferences.wallet) {
        return fetch(`/portal/rest/wallet/api/account/requestAuthorization?address=${window.walletSettings.userPreferences.wallet.address}`, {
          credentials: 'include',
        }).then((resp) => {
          if(!resp || !resp.ok) {
            throw new Error('Error while requesting authorization for wallet');
          }
          return this.adressRegistry.refreshWallet(window.walletSettings.userPreferences.wallet);
        }).then(() => {
          this.walletInitializationStatus = window.walletSettings && window.walletSettings.userPreferences && window.walletSettings.userPreferences.wallet && window.walletSettings.userPreferences.wallet.initializationState;
        }).catch(e => {
          this.error = String(e);
        });
      }
    },
    loadLastTransaction() {
      this.lastTransaction = null;
      const lastTransactions = {};

      return this.transactionUtils.loadTransactions(
        this.networkId,
        this.walletAddress,
        null,
        lastTransactions,
        false,
        1,
        null,
        true
      ).then(() => {
        this.lastTransaction = lastTransactions && lastTransactions.length && lastTransactions[0];
      });
    },
    loadPendingTransactions() {
      Object.keys(this.pendingTransactions).forEach((key) => delete this.pendingTransactions[key]);

      return this.transactionUtils.loadTransactions(
        this.networkId,
        this.walletAddress,
        null,
        this.pendingTransactions,
        true,
        10,
        null,
        true,
        (transaction) => {
          const contractDetails = transaction.to && this.accountsDetails[transaction.to.toLowerCase()];
          this.refreshBalance(contractDetails);
          if (this.pendingTransactions[transaction.hash]) {
            delete this.pendingTransactions[transaction.hash];
          }
          this.updatePendingTransactionsIndex++;
        },
        (error, transaction) => {
          this.$emit('error', error);
          if (this.pendingTransactions[transaction.hash]) {
            delete this.pendingTransactions[transaction.hash];
          }
          this.updatePendingTransactionsIndex++;
        }
      ).then(() => {
        this.updatePendingTransactionsIndex++;
      });
    },
  },
};
</script>
