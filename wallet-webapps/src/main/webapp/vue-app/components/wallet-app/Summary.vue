<template>
  <v-flex id="walletSummary" class="elevation-0 mr-3">
    <template v-if="!isSpace || isSpaceAdministrator">
      <v-card-title
        v-if="initializationState === 'NEW' || initializationState === 'MODIFIED' || initializationState === 'PENDING'"
        primary-title
        class="pb-0">
        <v-spacer />
        <div class="alert alert-info">
          <i class="uiIconInfo"></i>
          {{ $t('exoplatform.wallet.info.pendingInitialization') }}
        </div>
        <v-spacer />
      </v-card-title>
      <v-card-title
        v-else-if="initializationState === 'DENIED'"
        primary-title
        class="pb-0">
        <v-spacer />
        <div class="alert alert-info">
          <i class="uiIconInfo"></i>
          {{ $t('exoplatform.wallet.info.initializationAccessDenied') }}
          <button class="btn" @click="requestAccessAuthorization()">
            {{ $t('exoplatform.wallet.button.requestAuthorization') }}
          </button>
        </div>
        <v-spacer />
      </v-card-title>
    </template>

    <template v-if="initializationState !== 'DENIED'">
      <v-card-title
        v-if="pendingTransactionsCount"
        primary-title
        class="pb-0">
        <v-spacer />
        <v-badge
          :title="$t('exoplatform.wallet.message.transactionInProgress')"
          color="red"
          right>
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
        v-if="contractDetails"
        fluid
        grid-list-md
        pl-3
        pr-0>
        <v-layout col wrap>
          <v-flex
            md4
            xs12
            text-xs-center>
            <summary-balance :wallet="wallet" :contract-details="contractDetails" />
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
              :wallet="wallet"
              :contract-details="contractDetails"
              @display-transactions="$emit('display-transactions', 'reward')"
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
              :contract-details="contractDetails"
              :wallet-address="walletAddress"
              @display-transactions="$emit('display-transactions')"
              @error="$emit('error', $event)" />
          </v-flex>
        </v-layout>
      </v-container>
    </template>
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
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    isSpaceAdministrator: {
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
    initializationState: {
      type: String,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      updatePendingTransactionsIndex: 1,
      pendingTransactions: {},
      lastTransaction: null,
    };
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    pendingTransactionsCount() {
      return this.updatePendingTransactionsIndex && Object.keys(this.pendingTransactions).length;
    },
  },
  created() {
    this.loadPendingTransactions();
  },
  methods: {
    refreshBalance(accountDetails) {
      if (accountDetails && accountDetails.isContract) {
        this.$emit('refresh-token-balance', accountDetails);
      } else {
        this.$emit('refresh-balance');
      }
    },
    requestAccessAuthorization() {
      return fetch(`/portal/rest/wallet/api/account/requestAuthorization?address=${this.walletAddress}`, {
        credentials: 'include',
      }).then((resp) => {
        if(!resp || !resp.ok) {
          throw new Error(this.$t('exoplatform.wallet.error.errorRequestingAuthorization'));
        }
        this.$emit('refresh');
      })
      .catch(e => {
        this.$emit('error', String(e));
      });
    },
    loadLastTransaction() {
      this.lastTransaction = null;
      const lastTransactions = {};

      return this.transactionUtils.loadTransactions(
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
        this.walletAddress,
        null,
        this.pendingTransactions,
        true,
        10,
        null,
        true,
        (transaction) => {
          this.refreshBalance(this.contractDetail);
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
