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
                  {{ $t('exoplatform.wallet.settings.meedsWallet') }}
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
              <v-list-item-title class="text-color " :class="!isMetamaskInstalled && 'addonNotInstalled'">
                <div class="d-flex align-center">
                  <img
                    class="pr-2 pl-1"
                    :src="`/wallet-common/images/metamask.svg`"
                    alt="Metamask"
                    width="18">
                  {{ $t('exoplatform.wallet.settings.useMetamask') }}
                </div>
              </v-list-item-title>
            </v-list-item-content>
            <v-list-item-action>
              <v-switch
                class="pl-n1"
                :disabled="!isMetamaskInstalled"
                @click="connectToMetamask"
                v-model="useMetamask" />
            </v-list-item-action>
          </v-list-item>
          <v-list-item class="mt-n2" v-if="!isMetamaskInstalled">
            <v-list-item-content>
              <v-list-item-subtitle
                class="text-sub-title pl-1">
                <span class="mr-3 useMetamask">{{ $t('exoplatform.wallet.settings.metamaskInstallation') }}</span>
                <a
                  :href="metamaskInstallLink"
                  target="_blank"
                  rel="noopener nofollow">{{ metamaskInstallLink }}</a>
              </v-list-item-subtitle>
            </v-list-item-content>
          </v-list-item>
           <v-list-item class="mt-n2" v-if="newAddress !== ''">
            <v-list-item-content>
              <v-list-item-subtitle
                class="text-sub-title pl-1">
                <span class="mr-3 useMetamask">{{ newAddress }}</span>
              </v-list-item-subtitle>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-card>
    </template>
  </v-app>
</template>
<script>
import {saveNewProvider} from '../../js/AddressRegistry.js';
export default {
  data: () => ({
    id: `Wallet${parseInt(Math.random() * 10000)}`,
    displayed: true,
    displayDetails: false,
    wallet: null,
    from: '',
    useMetamask: false,
    rawMessage: 'Signature request message',
    newAddress: ''
  }),
  computed: {
    isSpace(){
      return this.wallet && this.wallet.spaceId && this.wallet.spaceId !== 0;
    },
    walletSettingsClass(){
      return eXo.env.portal.spaceName ? '': 'ma-4' ;
    },
    isMetamaskInstalled(){
      return  window.ethereum && window.ethereum.isMetaMask;
    },
    isMobile() {
      return this.$vuetify.breakpoint.smAndDown;
    },
    currentSiteLink() {
      return `${window.location.host}${window.location.pathname}`;
    },
    metamaskInstallLink() {
      return this.isMobile
        && `https://metamask.app.link/dapp/${this.currentSiteLink}`
        || 'https://metamask.io/';
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
    saveProvider(provider, address, rawMessage, signedMessage){
      return saveNewProvider(provider, address, rawMessage, signedMessage)
        .then(() => {
          this.newAddress = address;
        })
        .catch((e) => {
          console.error('save provider - error', e);
        });
    },
    signMessage(address){
      return window.ethereum.request({
        method: 'personal_sign',
        params: [this.rawMessage, address],
      }).then(signedMessage => {
        this.saveProvider('METAMASK', address, this.rawMessage, signedMessage);
      });
    },
    connectToMetamask() {
      if (this.useMetamask){
        return window.ethereum.request({
          method: 'eth_requestAccounts'
        })
          .then((connectedWallet)=>{
            this.signMessage(connectedWallet[0]);
          });
      } 
    },
  },
};
</script>
