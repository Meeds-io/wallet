<template>
  <div class="d-flex justify-space-between">
    <v-icon
      color="warning"
      size="16"
      class="me-2">
      fa-exclamation-triangle
    </v-icon>
    {{ alertMessage }}
    <div class="align-self-center align-center">
      <v-btn
        v-if="notInstalled"
        :href="metamaskInstallLinlk"
        name="installMetamaskLink"
        target="_blank"
        rel="nofollow noreferrer noopener"
        class="btn primary">
        <span class="text-capitalize">{{ buttonLabel }}</span>
      </v-btn>
      <v-btn
        v-else
        class="btn primary"
        @click="operateMetamaskChange">
        <span class="text-capitalize">{{ buttonLabel }}</span>
      </v-btn>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    notInstalled: {
      type: Boolean,
      default: false,
    },
    notConnected: {
      type: Boolean,
      default: false,
    },
    invalidNetwork: {
      type: Boolean,
      default: false,
    },
    invalidAccount: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    alertMessage() {
      if (this.notInstalled) {
        return this.$t('exoplatform.wallet.warn.metamaskNotInstalled');
      } else if (this.notConnected) {
        return this.$t('exoplatform.wallet.warn.metamaskDisconnected');
      } else if (this.invalidNetwork) {
        return this.$t('exoplatform.wallet.warn.networkVersion');
      } else if (this.invalidAccount) {
        return this.$t('exoplatform.wallet.warn.selectedAddress');
      }
      return '';
    },
    buttonLabel() {
      if (this.notInstalled) {
        return this.$t('exoplatform.wallet.warn.installMetamask');
      } else if (this.notConnected) {
        return this.$t('exoplatform.wallet.warn.connectMetamask');
      } else if (this.invalidNetwork) {
        return this.$t('exoplatform.wallet.warn.changeNetwork');
      } else if (this.invalidAccount) {
        return this.$t('exoplatform.wallet.warn.changeAccount');
      }
      return '';
    },
    isMobile() {
      return this.$vuetify.breakpoint.smAndDown;
    },
    currentSiteLink() {
      return `${window.location.host}${window.location.pathname}`;
    },
    metamaskInstallLinlk() {
      return this.isMobile
        && `https://metamask.app.link/dapp/${this.currentSiteLink}`
        || 'https://metamask.io/';
    },
  },
  methods: {
    operateMetamaskChange(){
      if (this.notConnected) {
        this.walletUtils.connectToMetamask();
      } else if (this.invalidNetwork) {
        this.walletUtils.switchMetamaskNetwork();
      } else if (this.invalidAccount) {
        this.walletUtils.switchMetamaskAccount();
      }
    }
  }
};
</script>