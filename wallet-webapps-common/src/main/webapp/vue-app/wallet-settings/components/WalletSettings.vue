<template>
  <v-app>
    <template v-if="displayed">
      <v-alert
        v-model="alert"
        :type="type"
        dismissible>
        {{ message }}
      </v-alert>
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
          <template v-if="metamaskFeatureEnabled">
            <v-list-item>
              <v-list-item-content transition="fade-transition" :class="(!isMetamaskInstalled || !useMetamask) && 'half-opacity'">
                <v-list-item-title class="text-color">
                  <div class="d-flex align-center">
                    <img
                      class="pr-2 pl-1"
                      :src="`/wallet-common/images/metamask.svg`"
                      alt="Metamask"
                      width="18">
                    <span></span>
                    {{ $t('exoplatform.wallet.settings.useMetamask') }}
                  </div>
                </v-list-item-title>
              </v-list-item-content>
              <v-list-item-action>
                <v-switch
                  v-model="useMetamask"
                  :loading="savingMetamaskAddress"
                  :disabled="!isMetamaskInstalled || savingMetamaskAddress"
                  class="pl-n1"
                  @click="switchMetamask" />
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
            <v-list-item class="mt-n2" v-if="metamaskAddress">
              <v-list-item-content>
                <v-list-item-subtitle
                  class="text-sub-title pl-5">
                  <v-chip class="connectedWalletChip">  
                    <span class="mr-3 useMetamask  walletText walletTitle">
                      {{ metamaskAddress.substring(0,5)+"..."+metamaskAddress.substring(metamaskAddress.length-4,metamaskAddress.length) }}
                    </span>
                  </v-chip>
                </v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action>
                <v-btn
                  small
                  icon>
                  <v-icon size="24" class="text-sub-title">
                    {{ $vuetify.rtl && 'fa-caret-left' || 'fa-caret-right' }}
                  </v-icon>
                </v-btn>
              </v-list-item-action>
            </v-list-item>
          </template>
        </v-list>
      </v-card>
    </template>
  </v-app>
</template>
<script>
import {switchProvider, switchInternalProvider} from '../../js/AddressRegistry.js';
export default {
  data: () => ({
    id: `Wallet${parseInt(Math.random() * 10000)}`,
    displayed: true,
    displayDetails: false,
    wallet: null,
    from: '',
    useMetamask: false,
    initialized: false,
    savingMetamaskAddress: false,
    metamaskAddress: null,
    metamaskFeatureEnabled: false,
    message: '',
    alert: false,
    type: '',
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
    this.$root.$on('show-alert', message => {
      this.displayMessage(message);
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
    },
    switchMetamask() {
      if (this.useMetamask) {
        this.connectToMetamask();
      } else {
        this.resetMetamask();
      }
    },
    resetMetamask() {
      this.savingMetamaskAddress = true;
      return switchInternalProvider()
        .then(() => this.metamaskAddress = null)
        .catch(() => this.useMetamask = true)
        .finally(() => this.savingMetamaskAddress = false);
    },
    connectToMetamask() {
      if (this.useMetamask) {
        this.savingMetamaskAddress = true;
        return window.ethereum.request({ method: 'wallet_requestPermissions', 
          params: [ { eth_accounts: {} } ] 
        })
          .then(() => this.retrieveAddress())
          .then(() => this.signMessage())
          .then(() => this.savingMetamaskAddress = false)
          .catch(() => {
            this.savingMetamaskAddress = false;
            this.useMetamask = false;
            this.metamaskAddress = null;
          });
      }
    },
    retrieveAddress() {
      return window.ethereum.request({ method: 'eth_requestAccounts'
      })
        .then(retrievedAddress => {
          this.metamaskAddress = retrievedAddress[0];          
        });
    },
    signMessage() {
      const rawMessage = this.$t('exoplatform.wallet.metamask.welcomeMessage');
      return window.ethereum.request({
        method: 'personal_sign',
        params: [rawMessage, this.metamaskAddress, ''],
      }).then(signedMessage => this.saveProvider('METAMASK', this.metamaskAddress, rawMessage, signedMessage));
    },
    saveProvider(provider, address, rawMessage, signedMessage){
      this.savingMetamaskAddress = true;
      return switchProvider(provider, address, rawMessage, signedMessage)
        .then(() => {
          this.metamaskAddress = address;
          this.$root.$emit('show-alert', {type: 'success',message: this.$t('exoplatform.wallet.metamask.message.connectedSuccess')});
        });
    },
    displayMessage(message) {
      this.message=message.message;
      this.type=message.type;
      this.alert = true;
      window.setTimeout(() => this.alert = false, 5000);
    }
  },
};
</script>