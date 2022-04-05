<template v-if="metamaskFeatureEnabled">
  <v-app>
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
          <span class="mr-3 text-light-color">{{ $t('exoplatform.wallet.settings.metamaskInstallation') }}</span>
          <a
            :href="metamaskInstallLink"
            target="_blank"
            rel="noopener nofollow">{{ metamaskInstallLink }}</a>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item class="mt-n2" v-if="metamaskAddress && isMetamaskInstalled">
      <v-list-item-content>
        <v-list-item-subtitle
          class="text-sub-title pl-5">
          <v-chip class="grey-background">  
            <span class="mr-3 dark-grey-color walletTitle">
              {{ walletAddress }}
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
  </v-app>
</template>
<script>
import {switchProvider, switchInternalProvider} from '../../js/AddressRegistry.js';
export default {
  props: {
    metamaskFeatureEnabled: {
      type: Boolean,
      default: false
    },
    useMetamask: {
      type: Boolean,
      default: false
    },
    metamaskAddress: {
      type: String,
      default: null,
    }
  },
  data: () => ({
    displayAlert: false,
    alertMessage: null,
    alertType: null,
    savingMetamaskAddress: false,
  }),
  computed: {
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
    },
    walletAddress(){
      return this.metamaskAddress && `${this.metamaskAddress.substring(0,5)}...${this.metamaskAddress.substring(this.metamaskAddress.length-4,this.metamaskAddress.length)}`;
    }
  },
  created() {
    this.$root.$on('show-alert', alert => {
      this.alertMessage = alert.message;
      this.alertType = alert.type;
      this.displayAlert= true;
      window.setTimeout(() => this.displayAlert = false, 5000); 
    });
  },
  methods: {
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
        .then(() =>{
          this.$root.$emit('wallet-settings-metamask-address', null);
          this.$root.$emit('wallet-settings-metamask-state', false);
        })
        .catch(() => this.$root.$emit('wallet-settings-metamask-state', true))
        .finally(() => this.savingMetamaskAddress = false);
    },
    connectToMetamask() {
      if (this.useMetamask) {
        this.$root.$emit('wallet-settings-metamask-state', true);
        this.savingMetamaskAddress = true;
        return window.ethereum.request({ method: 'wallet_requestPermissions', 
          params: [ { eth_accounts: {} } ] 
        })
          .then(() => this.retrieveAddress())
          .then((retrievedAddress) => this.signMessage(retrievedAddress))
          .then(() => this.savingMetamaskAddress = false)
          .catch(() => {
            this.savingMetamaskAddress = false;
            this.$root.$emit('wallet-settings-metamask-state', false);
            this.$root.$emit('wallet-settings-metamask-address', null);
          });
      }
    },
    retrieveAddress() {
      return window.ethereum.request({ method: 'eth_requestAccounts'
      })
        .then(retrievedAddress => {
          return retrievedAddress[0];
        });
    },
    signMessage(address) {
      const rawMessage = this.$t('exoplatform.wallet.metamask.welcomeMessage');
      return window.ethereum.request({
        method: 'personal_sign',
        params: [rawMessage, address, ''],
      }).then(signedMessage => this.saveProvider('METAMASK', address, rawMessage, signedMessage));
    },
    saveProvider(provider, address, rawMessage, signedMessage){
      this.savingMetamaskAddress = true;
      return switchProvider(provider, address, rawMessage, signedMessage)
        .then(() => {
          this.$root.$emit('wallet-settings-metamask-address', address);
          this.$root.$emit('show-alert', {type: 'success',message: this.$t('exoplatform.wallet.metamask.message.connectedSuccess')});
        });
    }
  }
};
</script>