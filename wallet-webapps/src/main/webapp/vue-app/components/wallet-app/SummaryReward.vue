<template>
  <v-card class="walletSummaryBalance">
    <v-card-title class="title pb-1 ellipsis">
      {{ $t('exoplatform.wallet.label.totalWalletRewards') }}
    </v-card-title>
    <v-card-title class="rewardBalance headline pt-0 pb-1">
      <v-container fluid>
        <v-layout row>
          <v-flex grow class="amount">
            {{ walletUtils.toFixed(rewardBalance) }} {{ contractDetails.symbol }}
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
    rewardBalance() {
      return (this.wallet && this.wallet.rewardBalance) || 0;
    }
  },
  methods: {
    displayTransactionList() {
      this.$emit('display-transactions');
    }
  }
}
</script>
