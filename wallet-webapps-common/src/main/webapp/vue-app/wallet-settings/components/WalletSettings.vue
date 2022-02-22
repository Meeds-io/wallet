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
            <v-list-item-action>
              <v-btn
                small
                icon
                @click="openDetail">
                <v-icon size="24" class="text-sub-title">
                  {{ $vuetify.rtl && 'fa-caret-left' || 'fa-caret-right' }}
                </v-icon>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
        </v-list>
      </v-card>
    </template>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    id: `Wallet${parseInt(Math.random() * 10000)}`,
    displayed: false,
    displayDetails: false,
    wallet: null,
    from: '',
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
    this.checkWalletInstalled();
    document.addEventListener('hideSettingsApps', (event) => {
      if (event && event.detail && this.id !== event.detail) {
        this.displayed = false;
      }
    });
    document.addEventListener('showSettingsApps', () => this.displayed = true);
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
    document.addEventListener('reloadSpaceApplications', this.checkWalletInstalled());
  },
  mounted() {
    this.$nextTick().then(() => this.$root.$applicationLoaded());
  },
  methods: {
    checkWalletInstalled() {
      const spaceId = eXo.env.portal.spaceId;
      const self = this;
      if (spaceId) {
        this.$spaceService.getSpaceApplications(spaceId)
          .then(applications => {
            this.applications = applications;
            this.applications.forEach( function (item) {
              if (item.id === 'SpaceWallet') {
                self.displayed = true;
              }
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
    },
  },
};
</script>
