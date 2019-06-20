<template>
  <v-card class="walletSummaryBalance">
    <v-card-title class="title pb-1 ellipsis">
      Total Rewarded Cauri
    </v-card-title>
    <v-card-title class="rewardBalance headline pt-0 pb-1">
      <template v-if="loadingBalance">
        <v-progress-circular
          color="primary"
          class="mb-2"
          indeterminate />
      </template>
      <v-container v-else fluid>
        <v-layout row>
          <v-flex grow class="amount">
            {{ rewardBalance }} {{ contractDetails.symbol }}
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
    displayTransactionList() {
      this.$emit('display-transactions', this.contractDetails);
    },
    refreshBalance() {
      if (!this.contractDetails || !this.contractDetails.contract || !this.walletAddress) {
        return;
      }
      this.loadingBalance = true;
      this.contractDetails.contract.methods.rewardBalanceOf(this.walletAddress).call()
        .then(rewardBalance => {
          this.rewardBalance = this.contractDetails.rewardBalance = (rewardBalance && this.walletUtils.convertTokenAmountReceived(rewardBalance, this.contractDetails.decimals)) || 0;
        })
        .catch(e => {
          this.$emit('error', e);
        })
        .finally(() => this.loadingBalance = false);
    }
  }
}
</script>
