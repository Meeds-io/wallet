<template>
  <v-card id="waletSummary" class="elevation-0">
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
      grid-list-md>
      <v-layout row wrap>
        <v-flex xs12 md3>
          <v-card class="walletSummaryBalance">
            <v-card-title class="headline">
              Current balance
            </v-card-title>
            <v-card-title>
              {{ principalAccountDetails.balance }} {{ principalAccountDetails.symbol }}
            </v-card-title>
          </v-card>
        </v-flex>
        <v-flex xs12 md3>
          <v-card class="walletSummaryBalance elevation-3">
            <v-card-title class="title">
              Total rewarded Cauri
            </v-card-title>
            <v-card-title>
              {{ principalAccountDetails.rewardBalance }} {{ principalAccountDetails.symbol }}
            </v-card-title>
          </v-card>
        </v-flex>
        <v-flex xs12 md3>
          <v-card class="walletSummaryBalance elevation-3">
            <v-card-title class="title">
              Last transactions
            </v-card-title>
            <v-card-title>
              {{ lastTransaction && lastTransaction.amount }} {{ principalAccountDetails.symbol }}
            </v-card-title>
          </v-card>
        </v-flex>
        <v-flex
          xs12
          md3
          class="walletSummaryActions">
          <v-card flat>
            <div class="walletSummaryAction">
              <send-funds-modal
                v-if="!isSpace || isSpaceAdministrator"
                ref="sendFundsModal"
                :accounts-details="accountsDetails"
                :overview-accounts="overviewAccounts"
                :principal-account="principalAccount"
                :network-id="networkId"
                :wallet-address="walletAddress"
                :disabled="disableSendButton"
                :icon="!isMaximized"
                regular-btn
                @success="refreshBalance($event)"
                @pending="loadPendingTransactions()"
                @error="
                  loadPendingTransactions();
                  $emit('error', $event);
                " />
            </div>
            <div class="walletSummaryAction">
              <request-funds-modal
                v-if="!isSpace || isSpaceAdministrator"
                ref="walletRequestFundsModal"
                :accounts-details="accountsDetails"
                :overview-accounts="overviewAccounts"
                :principal-account="principalAccount"
                :wallet-address="walletAddress"
                :icon="!isMaximized" />
            </div>
          </v-card>
        </v-flex>
      </v-layout>
    </v-container>
  </v-card>
</template>

<script>
import {loadTransactions} from '../js/TransactionUtils.js';
import {refreshWallet} from '../js/AddressRegistry.js';
import {toFixed} from '../js/WalletUtils.js';

export default {
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
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isSpaceAdministrator: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    hideActions: {
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
    loading: {
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
      if (document.location.search && document.location.search.length) {
        const search = document.location.search.substring(1);
        const parameters = JSON.parse(
          `{"${decodeURI(search)
            .replace(/"/g, '\\"')
            .replace(/&/g, '","')
            .replace(/=/g, '":"')}"}`
        );
        if (parameters && parameters.receiver && parameters.receiver_type) {
          if (isReadOnly) {
            throw new Error('Your wallet is in readonly state');
          }
          let contractAddress = parameters.contract;
          if (!contractAddress && parameters.principal && this.principalAccount && this.principalAccount.indexOf('0x') === 0) {
            contractAddress = this.principalAccount;
          }
          this.$refs.sendFundsModal.prepareSendForm(parameters.receiver, parameters.receiver_type, parameters.amount, contractAddress, parameters.id);
        }
      }
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
          return refreshWallet(window.walletSettings.userPreferences.wallet);
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

      return loadTransactions(
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

      return loadTransactions(
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
    toFixed(args) {
      return toFixed(args);
    },
  },
};
</script>
