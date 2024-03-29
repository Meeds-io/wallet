<template>
  <div>
    <v-list-item>
      <v-list-item-content transition="fade-transition" :class="(!isMetamaskInstalled || !useMetamask) && 'half-opacity'">
        <v-list-item-title class="text-color">
          <div class="d-flex align-center">
            <img
              class="pr-2 pl-1"
              :src="`/wallet/images/metamask.svg`"
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
          :disabled="(!isMetamaskInstalled && !useMetamask) || savingMetamaskAddress"
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
    <v-list-item class="mt-n2" v-if="metamaskAddress && !savingMetamaskAddress">
      <v-list-item-content>
        <v-list-item-subtitle
          class="text-sub-title pl-5">
          <v-chip class="grey-background">  
            <span class="mr-3 dark-grey-color walletTitle">
              {{ metamaskAddressPreview }}
            </span>
            <wallet-settings-jdenticon :address="metamaskAddress" />
          </v-chip>
        </v-list-item-subtitle>
      </v-list-item-content>
      <v-list-item-action>
        <v-btn
          small
          icon
          :disabled="savingMetamaskAddress"
          @click="openDetail">
          <v-icon size="24" class="text-sub-title">
            {{ $vuetify.rtl && 'fa-caret-left' || 'fa-caret-right' }}
          </v-icon>
        </v-btn>
      </v-list-item-action>
    </v-list-item>
  </div>
</template>
<script>
import {switchProvider, switchInternalProvider} from '../../js/AddressRegistry.js';
export default {
  props: {
    walletSettings: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    useMetamask: false,
    savingMetamaskAddress: false,
  }),
  computed: {
    isMetamaskInstalled(){
      return window.ethereum && window.ethereum.isMetaMask;
    },
    isMobile() {
      return this.$vuetify.breakpoint.smAndDown;
    },
    currentSiteLink() {
      return `${window.location.host}${window.location.pathname}`;
    },
    disableSwitchButton() {
      return (!this.isMetamaskInstalled && !this.useMetamask) || this.savingMetamaskAddress;
    },
    metamaskInstallLink() {
      return this.isMobile
        && `https://metamask.app.link/dapp/${this.currentSiteLink}`
        || 'https://metamask.io/';
    },
    metamaskAddress() {
      return this.useMetamask && this.walletSettings && this.walletSettings.wallet && this.walletSettings.wallet.address;
    },
    metamaskAddressPreview() {
      return this.metamaskAddress && `${this.metamaskAddress.substring(0,5)}...${this.metamaskAddress.substring(this.metamaskAddress.length-4,this.metamaskAddress.length)}`;
    },
    generatedToken() {
      return this.$root.generatedToken;
    },
    isEmptyPassphrase() {
      return this.walletSettings.wallet.passPhrase === null;
    },
    isDeleted() {
      return this.walletSettings.wallet.initializationState === 'DELETED';
    }
  },
  watch: {
    walletSettings: {
      immediate: true,
      handler() {
        this.useMetamask = this.walletSettings && this.walletSettings.wallet && this.walletSettings.wallet.provider === 'METAMASK';
        this.metamaskAddress = this.walletSettings && this.walletSettings.wallet && this.walletSettings.wallet.address;
      },
    },
  },
  created() {
    this.$root.$on('wallet-settings-provider-changing', provider => {
      this.useMetamask = provider === 'METAMASK';
    });
  },
  methods: {
    openDetail() {
      this.$emit('open-detail',
        this.$t('exoplatform.wallet.label.settings.metamask'),
        this.$t('exoplatform.wallet.message.settingsDescription.metamask'),
        this.$t('exoplatform.wallet.message.manageDigitalKey.metamask'));
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
      this.$root.$emit('wallet-settings-provider-changing', 'INTERNAL_WALLET');
      return switchInternalProvider()
        .then(() => {
          window.walletSettings.wallet.provider = 'INTERNAL_WALLET';
          this.$root.$emit('wallet-settings-provider-changed', 'INTERNAL_WALLET');
          if (this.isEmptyPassphrase || this.isDeleted) {
            if (this.walletSettings.wallet.type === 'user') {
              return window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet`;
            } else {
              return window.location.href =  `${eXo.env.portal.context}/g/:spaces:${eXo.env.portal.spaceGroup}/${eXo.env.portal.spaceName}/SpaceWallet`;
            }
          }
        })
        .catch(() => this.$root.$emit('wallet-settings-provider-changing', window.walletSettings.wallet.provider))
        .finally(() => {
          this.savingMetamaskAddress = false;
        });
    },
    connectToMetamask() {
      this.$root.$emit('wallet-settings-provider-changing', 'METAMASK');
      this.savingMetamaskAddress = true;
      let selectedAddress = null;
      return window.ethereum.request({ method: 'wallet_requestPermissions', 
        params: [ { eth_accounts: {} } ] 
      })
        .then(() => this.retrieveAddress())
        .then((retrievedAddress) => {
          selectedAddress = retrievedAddress;
          return this.signMessage(retrievedAddress);
        })
        .then(() => {
          window.walletSettings.wallet.address = selectedAddress;
          window.walletSettings.wallet.provider = 'METAMASK';
          this.$root.$emit('wallet-settings-provider-changed', 'METAMASK');
          this.savingMetamaskAddress = false;
        })
        .catch(e => {
          this.$root.$emit('wallet-settings-provider-changing', window.walletSettings.wallet.provider);
          this.savingMetamaskAddress = false;
          if (String(e).includes('wallet.addressConflict')) {
            this.$root.$emit('alert-message', this.$t('wallet.addressAlreadyInUse'), 'error');
          }
        });
    },
    retrieveAddress() {
      return window.ethereum.request({ method: 'eth_requestAccounts' })
        .then(retrievedAddress => {
          return retrievedAddress[0];
        });
    },
    signMessage(address) {
      let rawMessage = this.$t('exoplatform.wallet.metamask.welcomeMessage', {0: address, 1: this.generatedToken});
      rawMessage = rawMessage.split(/\\n/g).join('\u000A');
      return window.ethereum.request({
        method: 'personal_sign',
        params: [rawMessage, address, ''],
      })
        .then(signedMessage => this.saveProvider('METAMASK', address, rawMessage, signedMessage));
    },
    saveProvider(provider, address, rawMessage, signedMessage) {
      return switchProvider(provider, address, rawMessage, signedMessage)
        .then(() => {
          window.walletSettings.wallet.address = address;
          window.walletSettings.wallet.provider = 'METAMASK';

          this.$root.$emit('wallet-notification-alert', {
            type: 'success',
            message: this.$t('exoplatform.wallet.metamask.message.connectedSuccess')
          });
        });
    },
  }
};
</script>