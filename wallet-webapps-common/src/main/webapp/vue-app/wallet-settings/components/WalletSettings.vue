<template>
  <v-app>
    <wallet-settings-details
      v-if="displayDetails"
      :wallet-settings="walletSettings"
      :is-space="isSpace"
      :title="detailsTitle"
      :description="detailsDescription"
      :message-digital-key="detailsmessageDigitalKey"
      :class="walletSettingsClass"
      @back="closeDetail" />
    <v-card
      v-else-if="displayed"
      :class="walletSettingsClass"
      class="border-radius"
      flat>
      <v-list>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="title text-color">
              {{ $t('exoplatform.wallet.label.settings.internal') }}
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <wallet-settings-internal
          :wallet-settings="walletSettings"
          @open-detail="openDetail" />
        <wallet-settings-metamask
          v-if="!isSpace"
          :wallet-settings="walletSettings"
          @open-detail="openDetail" />
      </v-list>
    </v-card>
    <wallet-notification-alert />
  </v-app>
</template>
<script>
export default {
  data: () => ({
    id: `Wallet${parseInt(Math.random() * 10000)}`,
    walletSettings: {},
    displayed: true,
    displayDetails: false,
  }),
  computed: {
    isSpace() {
      return eXo.env.portal.spaceId !== '' || (this.wallet && this.wallet.spaceId && this.wallet.spaceId !== 0);
    },
    walletSettingsClass() {
      return eXo.env.portal.spaceName ? '': 'ma-4' ;
    },
  },
  created() {
    document.addEventListener('hideSettingsApps', (event) => {
      if (event && event.detail && this.id !== event.detail) {
        this.displayed = false;
      }
    });
    document.addEventListener('showSettingsApps', () => {
      this.displayed = true;
      if (this.isSpace) {
        this.checkWalletInstalled();
      }
    });

    this.$root.$on('wallet-settings-provider-changed', provider => {
      if (provider === 'INTERNAL_WALLET') {
        this.walletUtils.initSettings(this.isSpace)
          .then(() => this.walletSettings = Object.assign({}, window.walletSettings));
      } else {
        this.walletSettings = Object.assign({}, window.walletSettings);
      }
    });

    if (this.isSpace) {
      this.checkWalletInstalled();
    }

    if (window.walletSettings && window.walletSettings.wallet) {
      this.walletSettings = Object.assign({}, window.walletSettings);
      this.walletUtils.initWeb3(this.isSpace, true);
      this.$root.$applicationLoaded();
    } else {
      this.walletUtils.initSettings(this.isSpace)
        .then(() => {
          this.walletSettings = window.walletSettings && Object.assign({}, window.walletSettings) || {};
          return this.walletUtils.initWeb3(this.isSpace, true);
        })
        .finally(() => this.$root.$applicationLoaded());
    }
  },
  mounted(){
    if (window.location.href.includes('walletSetting')) {
      window.location.hash='#walletSettingsApp';
      return window.location.href;
    }
  },
  methods: {
    checkWalletInstalled() {
      this.displayed = false;
      this.$spaceService.getSpaceApplications(eXo.env.portal.spaceId)
        .then(applications => {
          this.applications = applications;
          this.displayed = this.applications.some( item => {
            return item.id === 'SpaceWallet';
          });
        });
    },
    openDetail(title, description, messageDigitalKey) {
      document.dispatchEvent(new CustomEvent('hideSettingsApps', { detail: this.id }));
      this.detailsTitle = title;
      this.detailsDescription = description;
      this.detailsmessageDigitalKey = messageDigitalKey;
      this.displayDetails = true;
    },
    closeDetail() {
      document.dispatchEvent(new CustomEvent('showSettingsApps'));
      this.displayDetails = false;
      window.history.replaceState('', window.document.title, window.location.href.split('#')[0]);
    },
  },
};
</script>