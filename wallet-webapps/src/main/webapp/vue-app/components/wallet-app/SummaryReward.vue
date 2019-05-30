<template>
  <v-card class="walletSummaryBalance elevation-3">
    <v-card-title class="title">
      Total Rewarded Cauri
    </v-card-title>
    <v-card-title class="rewardBalance pt-0 headline">
      <template v-if="loadingBalance">
        <v-progress-circular
          color="primary"
          class="mb-2"
          indeterminate />
      </template>
      <template v-else>
        {{ rewardBalance }} {{ contractDetails.symbol }}
      </template>
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
      loadingBalance: false,
      rewardBalance: 0,
    }
  },
  watch: {
    contractDetails() {
      if (this.contractDetails && this.contractDetails.contract) {
        this.refreshBalance();
      }
    }
  },
  created() {
    if (this.contractDetails && this.contractDetails.contract) {
      this.refreshBalance();
    }
  },
  methods: {
    refreshBalance() {
      if (!this.contractDetails || !this.contractDetails.contract || !this.walletAddress) {
        return;
      }
      this.loadingBalance = true;
      this.contractDetails.contract.methods.rewardBalanceOf(this.walletAddress).call()
        .then(rewardBalance => {
          this.rewardBalance = (rewardBalance && this.walletUtils.convertTokenAmountReceived(rewardBalance, this.contractDetails.decimals)) || 0;
        })
        .catch(e => {
          this.$emit('error', e);
        })
        .finally(() => this.loadingBalance = false);
    }
  }
}
</script>
