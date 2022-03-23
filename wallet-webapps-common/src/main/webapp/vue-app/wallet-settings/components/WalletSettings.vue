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
            <v-list-item-content>
              <v-list-item-title class="text-color">
                <div class="d-flex align-center">
                  <img
                    class="pr-2 pl-1"
                    :src="`/wallet-common/images/meeds.svg`"
                    alt="Meeds" 
                    width="16">
                  {{ $t('exoplatform.wallet.meedsChoice') }}
                </div>
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
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title class="text-color " :class="!metaMask && 'addonNotInstalled'">
                <div class="d-flex align-center">
                  <img
                    class="pr-2 pl-1"
                    :src="`/wallet-common/images/metamask.svg`"
                    alt="Metamask"
                    width="18">
                  {{ $t('exoplatform.wallet.metaMaskChoice') }}
                </div>
              </v-list-item-title>
              <v-list-item-subtitle
                class="text-sub-title pl-1 my-3"
                v-if="!metaMask">
                <span class="mr-3 useMetamask">{{ $t('exoplatform.wallet.metaMaskInstallation') }}</span><a
                  :href="linkMetamask"
                  target="_blank"
                  rel="noopener nofollow">{{ linkMetamask }}</a>
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-list-item-action>
              <v-switch
                class="pl-n1"
                :disabled="!metaMask"
                @click="connectToMetamask"
                v-model="choiceWallet" />
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
    displayed: true,
    displayDetails: false,
    wallet: null,
    from: '',
    choiceWallet: false,
    metaMask: false,
    linkMetamask: 'https://metamask.io/'
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
    this.checkWalletInstalled();
    this.init();
    this.checkMetaMaskInstalled();
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
  },
  mounted() {
    this.$nextTick().then(() => this.$root.$applicationLoaded());
  },
  methods: {
    checkMetaMaskInstalled(){
      if (typeof window.ethereum !== 'undefined' && window.ethereum.isMetaMask){this.metaMask= true;}
    },
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
    },
    saveWallet() {
      const walletParams = {
        provider: this.choiceWallet ? 'METAMASK': 'MEEDS_WALLET',
        id: this.getQueryParam('id'),
        type: this.getQueryParam('type'),
        address: window.walletSettings.wallet
      };
      return fetch('portal/rest/wallet/api/account/saveAddress', {
        method: 'POST',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(walletParams),
      })
        .then((resp) => {
          if (resp && resp.ok) {
            return resp.text();
          } else {
            throw new Error('Error saving wallet');
          }
        })
        .catch((e) => {
          this.loading = false;
          console.error('fetch save wallet - error', e);
        });
    },
    connectToMetamask() {
      if (this.choiceWallet){
        return window.ethereum.request({
          method: 'eth_requestAccounts'
        })
          .then((walletConnected)=>{
          // this.saveWallet();
            console.log('walletConnected', walletConnected[0]);
          });
      } 
    },
  },
};
</script>
