<template>
  <v-app>
    <v-card flat>
      <v-card-text class="pa-0 d-flex justify-center flex-nowrap text-color display-1 font-weight-bold big-number">
        <span class="my-2 secondary--text">{{ symbol }}</span>
        <span class="text-truncate ma-2 display-1 font-weight-bold">{{ balanceToDisplay }}</span>
      </v-card-text>
    </v-card>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    wallet: null,
    contractDetails: null,
  }),
  computed: {
    symbol() {
      return this.contractDetails?.symbol;
    },
    balance() {
      return this.wallet?.tokenBalance;
    },
    balanceToDisplay() {
      return this.balance?.toFixed(2);
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