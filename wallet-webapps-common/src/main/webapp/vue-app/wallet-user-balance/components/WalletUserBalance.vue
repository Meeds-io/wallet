<template>
  <v-app>
    <div flat @click="$refs.accountDetail.open()" class="clickable">
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
import {initSettings} from '../../js/WalletUtils.js';
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
    initSettings(false, true, true)
      .then(() => {
        this.wallet = Object.assign({}, window.walletSettings.wallet);
        this.contractDetails = Object.assign({},window.walletSettings.contractDetail); 
        document.dispatchEvent(new CustomEvent('balanceAmount', {detail: this.wallet.tokenBalance}));
      });
  }
};
</script>