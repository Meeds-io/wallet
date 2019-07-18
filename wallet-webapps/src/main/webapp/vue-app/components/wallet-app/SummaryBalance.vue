<template>
  <v-card class="walletSummaryCurrentBalance">
    <v-card-title class="headline pb-1">
      {{ $t('exoplatform.wallet.label.currentBalance') }}
    </v-card-title>
    <v-card-title class="tokenBalance pt-0 display-1">
      {{ walletUtils.toFixed(contractDetails.balance) }} {{ contractDetails.symbol }}
    </v-card-title>
  </v-card>
</template>

<script>
export default {
  props: {
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
  },
  computed: {
    balance() {
      return (this.contractDetails && this.contractDetails.balance) || 0;
    },
    symbol() {
      return (this.contractDetails && this.contractDetails.symbol) || '';
    },
  },
}
</script>
