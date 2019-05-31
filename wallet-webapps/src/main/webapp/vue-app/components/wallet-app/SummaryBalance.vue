<template>
  <v-card class="walletSummaryBalance">
    <v-card-title class="headline pb-1">
      Current balance
    </v-card-title>
    <v-card-title class="tokenBalance pt-0 display-1">
      {{ contractDetails.balance }} {{ contractDetails.symbol }}
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
  }
}
</script>
