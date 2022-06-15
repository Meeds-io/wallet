<template>
  <v-snackbar
    v-model="snackbar"
    :left="!$vuetify.rtl"
    :right="$vuetify.rtl"
    color="transparent"
    elevation="0"
    app>
    <exo-notification-alert
      :alert="alert"
      @dismissed="clear">
      <template #actions>
        <a
          :href="transactionHashLink"
          :title="$t('exoplatform.wallet.message.transactionExplorerLink')"
          target="_blank">
          {{ transactionLinkLabel }}
        </a>
      </template>
    </exo-notification-alert>
  </v-snackbar>
</template>
<script>
export default {
  data: () => ({
    snackbar: false,
    alert: null,
  }),
  computed: {
    transactionLinkLabel() {
      return this.$t('exoplatform.wallet.message.followTransaction',{0: this.walletUtils.getUrlHostName(this.walletUtils.getTransactionEtherscanlink())});
    },
    transactionHashLink(){
      return this.alert.transactionHash;
    }
  },
  watch: {
    alert() {
      this.snackbar = !!this.alert;
    }
  },
  created() {
    this.$root.$on('wallet-notification-alert', alert => this.alert = alert);
  },
  methods: {
    clear() {
      this.alert = null;
    },
  },
};
</script>