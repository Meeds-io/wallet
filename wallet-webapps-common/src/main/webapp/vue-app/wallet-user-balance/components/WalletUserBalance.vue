<template>
  <v-app>
    <v-card flat @click="$refs.accountDetail.open()">
      <v-card-text class="pa-0 d-flex justify-center flex-nowrap text-color display-1 font-weight-bold big-number">
        <span class="my-2 tertiary-color">{{ symbol }}</span>
        <span class="text-truncate ma-2 display-1 font-weight-bold">{{ balanceToDisplay }}</span>
      </v-card-text>
    </v-card>
    <wallet-reward-account-detail
      ref="accountDetail"
      :fiat-symbol="symbol"
      :wallet="wallet"
      :contract-details="contractDetails" />
  </v-app>
</template>
<script>
export default {
  data: () => ({
    wallet: null,
    contractDetails: null,
    isOverviewDisplay: {
      type: Boolean,
      default: () => false,
    },
  }),
  computed: {
    symbol() {
      return this.contractDetails?.symbol;
    },
    balance() {
      return this.wallet?.tokenBalance;
    },
    balanceToDisplay() {
      return this.isOverviewDisplay ? Math.trunc(this.balance) : this.balance?.toFixed(2);
    }
  },
  created() {
    this.walletUtils.initSettings(false, true, true)
      .then(() => {
        this.wallet = Object.assign({}, window.walletSettings.wallet);
        this.contractDetails = Object.assign({},window.walletSettings.contractDetail); 
      });
  }
};
</script>