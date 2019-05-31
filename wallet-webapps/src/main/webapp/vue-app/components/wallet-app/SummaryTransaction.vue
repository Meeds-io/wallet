<template>
  <v-card class="walletSummaryBalance elevation-3">
    <v-card-title class="title pb-1">
      Last transactions
    </v-card-title>
    <v-card-title class="lastTransactionBalance headline pt-0 pb-1">
      <template v-if="loadingTransaction">
        <v-progress-circular
          color="primary"
          class="mb-2"
          indeterminate />
      </template>
      <v-container
        v-else
        fluid
        grid-list-sm>
        <v-layout row>
          <v-flex grow class="amount">
            {{ lastTransactionSign }}{{ lastTransaction && lastTransaction.contractAmount }} {{ contractDetails.symbol }}
          </v-flex>
          <v-flex shrink>
            <v-btn
              icon
              small
              @click="displayTransactionList">
              <v-icon color="primary">fa-search-plus</v-icon>
            </v-btn>
          </v-flex>
        </v-layout>
      </v-container>
    </v-card-title>
  </v-card>
</template>

<script>
export default {
  props: {
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
  },
  data() {
    return {
      loadingTransaction: false,
      lastTransaction: null,
      lastTransactionSent: null,
    }
  },
  computed: {
    lastTransactionSign() {
      return (this.lastTransaction && this.lastTransaction.contractAmount && this.lastTransactionSent && '-') || '+';
    }
  },
  watch: {
    contractDetails() {
      this.refreshLastTransaction();
    }
  },
  created() {
    this.refreshLastTransaction();
  },
  methods: {
    displayTransactionList() {
      this.$emit('display-transactions', this.contractDetails);
    },
    refreshLastTransaction() {
      if (!this.contractDetails || !this.contractDetails.address || !this.walletAddress) {
        return;
      }
      this.loadingTransaction = true;
      this.transactionUtils.getStoredTransactions(this.contractDetails.networkId, this.walletAddress, this.contractDetails.address, 10)
        .then(transactions => {
          this.lastTransaction = transactions && transactions.length && transactions[0];
          this.lastTransactionSent = (this.lastTransaction && this.lastTransaction.contractAmount && this.lastTransaction.from && (this.lastTransaction.from.toLowerCase() === this.walletAddress.toLowerCase()));
        })
        .catch(e => {
          this.$emit('error', e);
        })
        .finally(() => this.loadingTransaction = false);
    }
  }
}
</script>
