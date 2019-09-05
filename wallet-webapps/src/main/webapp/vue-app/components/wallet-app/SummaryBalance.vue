<template>
  <v-card class="walletSummaryBalance elevation-1">
    <v-card-title class="headline pb-1">
      {{ $t('exoplatform.wallet.label.currentBalance') }}
    </v-card-title>
    <v-card-title class="tokenBalance pt-0 display-1">
      {{ balance }} {{ contractDetails.symbol }}
    </v-card-title>
  </v-card>
</template>

<script>
export default {
  props: {
    wallet: {
      type: Object,
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
  computed: {
    balance() {
      return (this.wallet && this.wallet.tokenBalance && this.walletUtils.toFixed(this.wallet.tokenBalance)) || 0;
    },
    symbol() {
      return (this.contractDetails && this.contractDetails.symbol) || '';
    },
  },
}
</script>
