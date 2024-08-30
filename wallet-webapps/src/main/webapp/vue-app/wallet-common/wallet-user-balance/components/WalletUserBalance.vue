<template>
  <div>
    <v-card
      v-show="!loading"
      flat
      v-on="balanceToDisplay && {
        click: () => $root.$emit('wallet-overview-drawer', 'transactions'),
      }">
      <div class="pa-0 d-flex justify-center flex-nowrap text-color font-weight-bold big-number">
        <span class="my-2 tertiary-color">{{ symbol }}</span>
        <span
          class="ma-2 text-color text-h5 font-weight-bold d-flex align-self-center">
          {{ balanceToDisplay || '-' }}
        </span>
      </div>
    </v-card>
    <wallet-overview-drawer
      ref="walletOverviewDrawer"
      :symbol="currencySymbol" />
  </div>
</template>
<script>
export default {
  data: () => ({
    wallet: null,
    contractDetails: null,
    loading: true,
  }),
  computed: {
    symbol() {
      return this.contractDetails?.symbol;
    },
    balance() {
      return this.wallet?.tokenBalance;
    },
    balanceToDisplay() {
      return Number.isFinite(Number(this.balance))
        ? new Intl.NumberFormat(eXo.env.portal.language, {
          style: 'decimal',
          minimumFractionDigits: 0,
          maximumFractionDigits: 0,
        }).format(Math.trunc(this.balance))
        : 0;
    },
  },
  created() {
    this.walletUtils.initSettings(false, true, true)
      .then(() => {
        this.wallet = window.walletSettings.wallet
          && {...window.walletSettings.wallet}
          || {};
        this.contractDetails = window.walletSettings.contractDetail
          && {...window.walletSettings.contractDetail}
          || {};
      })
      .finally(() => this.loading = false);
  }
};
</script>