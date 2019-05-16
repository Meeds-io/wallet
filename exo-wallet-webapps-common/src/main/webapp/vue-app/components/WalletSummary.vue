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

    <v-card-title primary-title class="pt-2 pb-0">
      <v-flex class="flex-center">
        <h4 v-if="loading" class="headline">
          -
        </h4>
        <template v-else>
          <h4 v-if="principalAccount === 'fiat'" class="headline">{{ toFixed(totalFiatBalance) }} {{ fiatSymbol }}</h4>
          <h4 v-else-if="principalAccount === 'ether'" class="headline">{{ toFixed(totalBalance) }} ether</h4>
          <h4 v-else class="headline">{{ principalAccountDetails && toFixed(principalAccountDetails.balance) }} {{ principalAccountDetails && principalAccountDetails.symbol }}</h4>
        </template>
      </v-flex>
    </v-card-title>

    <v-card-title primary-title class="pt-0 flex-center">
      <v-flex v-for="(accountDetails, index) in overviewAccountsArray" :key="index">
        <template
          v-if="loading"
          class="headline">
          -
        </template>
        <template
          v-else-if="accountDetails.key === 'fiat'"
          class="headline">
          {{ toFixed(totalFiatBalance) }} {{ fiatSymbol }}
        </template>
        <template
          v-else-if="accountDetails.key === 'ether'"
          class="headline">
          {{ toFixed(totalBalance) }} ether
        </template>
        <template
          v-else
          class="headline">
          {{ toFixed(accountDetails.balance) }} {{ accountDetails && accountDetails.symbol }}
        </template>
      </v-flex>
    </v-card-title>

    <v-card-actions v-if="!hideActions">
      <v-spacer />
      <v-layout row wrap>
        <v-flex>
          <v-bottom-nav
            :value="true"
            color="white"
            class="elevation-0 buttomNavigation">
            <send-funds-modal
              v-if="!isSpace || isSpaceAdministrator"
              ref="sendFundsModal"
              :accounts-details="accountsDetails"
              :overview-accounts="overviewAccounts"
              :principal-account="principalAccount"
              :refresh-index="refreshIndex"
              :network-id="networkId"
              :wallet-address="walletAddress"
              :disabled="disableSendButton"
              :icon="!isMaximized"
              @success="refreshBalance($event)"
              @pending="loadPendingTransactions()"
              @error="
                loadPendingTransactions();
                $emit('error', $event);
              " />
            <v-divider v-if="!isMaximized" vertical />
            <wallet-receive-modal
              ref="walletReceiveModal"
              :wallet-address="walletAddress"
              :icon="!isMaximized"
              @pending="loadPendingTransactions()"
              @error="$emit('error', $event)" />
            <v-divider v-if="!isMaximized" vertical />
            <wallet-request-funds-modal
              v-if="!isSpace || isSpaceAdministrator"
              ref="walletRequestFundsModal"
              :accounts-details="accountsDetails"
              :overview-accounts="overviewAccounts"
              :principal-account="principalAccount"
              :refresh-index="refreshIndex"
              :wallet-address="walletAddress"
              :icon="!isMaximized" />
          </v-bottom-nav>
        </v-flex>
      </v-layout>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import WalletReceiveModal from './WalletReceiveModal.vue';
import WalletRequestFundsModal from './WalletRequestFundsModal.vue';
import SendFundsModal from './SendFundsModal.vue';

import {loadTransactions} from '../js/TransactionUtils.js';
import {refreshWallet} from '../js/AddressRegistry.js';

export default {
  components: {
    SendFundsModal,
    WalletRequestFundsModal,
    WalletReceiveModal,
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
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
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
      if (this.refreshIndex > 0 && this.principalAccount && this.accountsDetails[this.principalAccount]) {
        return this.accountsDetails[this.principalAccount];
      } else {
        // Return ether/fiat
        return this.accountsDetails[this.walletAddress];
      }
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
  },
};
</script>
