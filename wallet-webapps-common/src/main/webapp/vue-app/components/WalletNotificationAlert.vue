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
      <template #actions v-if="showTransactionLink">
        <a
          :href="transactionHashLink"
          :title="$t('exoplatform.wallet.message.transactionExplorerLink')"
          rel="external nofollow noreferrer noopener"
          class="d-block"
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
      return this.$t('exoplatform.wallet.message.followTransaction', {0: this.walletUtils.getTransactionExplorerName()});
    },
    transactionHashLink(){
      return this.walletUtils.getTransactionEtherscanlink().concat(this.alert.transactionHash);
    },
    showTransactionLink() {
      return this.alert?.transactionHash;
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