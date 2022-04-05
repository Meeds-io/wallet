<template>
  <v-app>
    <template v-if="displayed">
      <wallet-settings-details
        v-if="displayDetails"
        :wallet-details="wallet"
        :is-space="isSpace"
        :class="walletSettingsClass"
        @back="closeDetail"
        @settings-changed="openDetail" />
      <v-card
        v-else
        class="border-radius"
        :class="walletSettingsClass"
        flat>
        <v-list>
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title class="title text-color">
                {{ $t('exoplatform.wallet.label.settings') }}
              </v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-list-item>
            <v-list-item-content transition="fade-transition" :class="useMetamask && 'half-opacity'">
              <v-list-item-title class="text-color">
                <div class="d-flex align-center">
                  <img
                    class="pr-2 pl-1"
                    :src="`/wallet-common/images/meeds.svg`"
                    alt="Meeds" 
                    width="16">
                  {{ $t('exoplatform.wallet.settings.meedsWallet') }}
                </div>
              </v-list-item-title>
            </v-list-item-content>
            <v-list-item-action>
              <v-btn
                :disabled="useMetamask"
                small
                icon
                @click="openDetail">
                <v-icon size="24" class="text-sub-title">
                  {{ $vuetify.rtl && 'fa-caret-left' || 'fa-caret-right' }}
                </v-icon>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
          <wallet-settings-metamask
            :metamask-feature-enabled="metamaskFeatureEnabled"
            :use-metamask="useMetamask"
            :metamask-address="metamaskAddress" />
        </v-list>
      </v-card>
    </template>
    <wallet-settings-alert />
  </v-app>
</template>
<script>
export default {
  data: () => ({
    id: `Wallet${parseInt(Math.random() * 10000)}`,
    displayed: true,
    displayDetails: false,
    wallet: null,
    from: '',
    useMetamask: false,
    initialized: false,
    metamaskAddress: null,
    metamaskFeatureEnabled: false,
  }),
  computed: {
    isSpace(){
      return this.wallet && this.wallet.spaceId && this.wallet.spaceId !== 0;
    },
    walletSettingsClass(){
      return eXo.env.portal.spaceName ? '': 'ma-4' ;
    }
  },
  created() {
    document.addEventListener('hideSettingsApps', (event) => {
      if (event && event.detail && this.id !== event.detail) {
        this.displayed = false;
      }
    });
    document.addEventListener('showSettingsApps', () => {
      this.displayed = true;
      this.checkWalletInstalled();
    });
    this.$root.$on('wallet-settings-metamask-state', value => this.useMetamask = value);
    this.$root.$on('wallet-settings-metamask-address', value => this.metamaskAddress = value);

    this.checkWalletInstalled();
    this.init();
    setTimeout( () => {
      this.from = this.getQueryParam('from');
      if (this.from === 'walletApp') {
        const id = this.getQueryParam('id');
        const type = this.getQueryParam('type');
        this.getWallet(id,type);
        window.history.replaceState('wallet', this.$t('exoplatform.wallet.title.myWalletPageTitle'), `${eXo.env.portal.context}/${eXo.env.portal.portalName}/settings`);
        document.location.hash = '#walletSettings';
      }
      if (this.from === 'space') {
        const id = this.getQueryParam('id');
        const type = this.getQueryParam('type');
        this.getWallet(id,type);
        window.history.replaceState('wallet', this.$t('exoplatform.wallet.title.myWalletPageTitle'), `${eXo.env.portal.context}/g/:spaces:${eXo.env.portal.spaceGroup}/${eXo.env.portal.spaceName}/settings`);
        document.location.hash = '#walletSettings';
      }
    }, 300);

    if (window.walletSettings && window.walletSettings.wallet) {
      this.useMetamask =  window.walletSettings.wallet && window.walletSettings.wallet.provider === 'METAMASK';
      this.metamaskFeatureEnabled = window.walletSettings.metamaskEnabled;
      this.initialized = true;
    } else {
      this.walletUtils.initSettings(this.isSpace)
        .then(() => {
          this.useMetamask =  window.walletSettings && window.walletSettings.wallet && window.walletSettings.wallet.provider === 'METAMASK';
          this.metamaskFeatureEnabled = window.walletSettings.metamaskEnabled;
          if (this.useMetamask) {
            this.metamaskAddress = window.walletSettings.wallet.address;
          }
          this.$nextTick().then(() => this.$root.$applicationLoaded());
        })
        .finally(() => this.initialized = true);
    }
  },
  methods: {
    checkWalletInstalled() {
      if (eXo.env.portal.spaceId) {
        this.displayed = false;
        this.$spaceService.getSpaceApplications(eXo.env.portal.spaceId)
          .then(applications => {
            this.applications = applications;
            this.displayed = this.applications.some( item => {
              return item.id === 'SpaceWallet';
            });
          });
      }
    },
    init() {
      this.walletUtils.initSettings(eXo.env.portal.spaceName !== '', true)
        .then(() => this.walletUtils.initWeb3(this.isSpace, true));
    },
    openDetail() {
      document.dispatchEvent(new CustomEvent('hideSettingsApps', {detail: this.id}));
      this.getWallet();
      this.displayDetails = true;
      this.init();
    },
    closeDetail() {
      document.dispatchEvent(new CustomEvent('showSettingsApps'));
      this.displayDetails = false;
      if (this.from === 'space' || eXo.env.portal.spaceName){
        window.history.replaceState('wallet', this.$t('exoplatform.wallet.title.myWalletPageTitle'), `${eXo.env.portal.context}/g/:spaces:${eXo.env.portal.spaceGroup}/${eXo.env.portal.spaceName}/settings`);
      } else {
        window.history.replaceState('wallet', this.$t('exoplatform.wallet.title.myWalletPageTitle'), `${eXo.env.portal.context}/${eXo.env.portal.portalName}/settings`);
      }
    },
    getQueryParam(paramName) {
      const uri = window.location.search.substring(1);
      const params = new URLSearchParams(uri);
      return params.get(paramName);
    },
    getWallet(id,type) {
      if (!id){
        id = eXo.env.portal.spaceName ? eXo.env.portal.spaceName : eXo.env.portal.userName ;
        type = eXo.env.portal.spaceName ? 'space' : 'user';
      }
      return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/account/detailsById?id=${id}&type=${type}`)
        .then((resp) => resp && resp.ok && resp.json())
        .then(wallet => {
          this.wallet = wallet;
        });
    }
  },
};
</script>