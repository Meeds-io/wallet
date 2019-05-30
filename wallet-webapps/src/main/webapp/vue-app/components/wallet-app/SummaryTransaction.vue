<template>
  <v-card class="walletSummaryBalance elevation-3">
    <v-card-title class="title">
      Last transactions
    </v-card-title>
    <v-card-title class="lastTransactionBalance pt-0 headline">
      {{ lastTransactionSign }}{{ lastTransaction && lastTransaction.contractAmount }} {{ contractDetails.symbol }}
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
      return this.lastTransaction && this.lastTransaction.contractAmount && this.lastTransactionSent && '-';
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
