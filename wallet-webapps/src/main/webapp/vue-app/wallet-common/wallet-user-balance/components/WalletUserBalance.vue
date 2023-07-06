<template>
  <v-app id="WalletApp" class="ma-0">
    <div
      v-show="!loading"
      flat
      @click="$refs.accountDetail.open()"
      class="clickable">
      <div class="pa-0 d-flex justify-center flex-nowrap text-color display-1 font-weight-bold big-number">
        <span class="my-2 tertiary-color">{{ symbol }}</span>
        <span
          :class="typographyClass"
          class="ma-2 text-color font-weight-bold d-flex align-self-center">
          {{ balanceToDisplay }}
        </span>
      </div>
    </div>
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
    loading: true,
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
      return Number.isFinite(Number(this.balance)) ? Math.trunc(this.balance) : '';
    },
    typographyClass() {
      switch (String(this.balanceToDisplay).length) {
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
        return 'text-h4';
      case 6:
        return 'text-h5';
      case 7:
      case 8:
        return 'text-h6';
      default:
        return 'body-1';
      }
    },
  },
  created() {
    this.walletUtils.initSettings(false, true, true)
      .then(() => {
        this.wallet = Object.assign({}, window.walletSettings.wallet);
        this.contractDetails = Object.assign({},window.walletSettings.contractDetail); 
      })
      .finally(() => this.loading = false);
  }
};
</script>