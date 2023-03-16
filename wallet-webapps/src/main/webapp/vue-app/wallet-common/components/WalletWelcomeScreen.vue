<template>
  <v-flex
    id="walletWelcomeScreen"
    class="mx-4">
    <v-row no-gutters>
      <v-col>
        <div class="title px-4 py-4 styleTitle">{{ $t('exoplatform.wallet.label.createWalletInvitation') }}</div>
        <div class="d-flex flex-column pl-4">
          <div class="subtitle walletInformation">{{ $t('exoplatform.wallet.label.createWalletInvitation.description') }}</div>
          <div class="subtitle walletInformation pt-1">{{ $t('exoplatform.wallet.label.createWalletInvitation.spendDescription') }}</div>
        </div>
        <div>
          <div class="subtitle  px-4 pt-4 styleTitle">{{ $t('exoplatform.wallet.label.createWalletInvitation.secure') }}</div>
          <ul class="ml-8 px-4 py-4">
            <li class="subtitle walletInformation">{{ $t('exoplatform.wallet.label.createWalletInvitation.firstDescription') }}</li>
            <li class="subtitle walletInformation">{{ $t('exoplatform.wallet.label.createWalletInvitation.lastDescription') }}</li>
          </ul>
        </div>
      </v-col>
    </v-row>
    <v-row class="my-12 justify-space-between">
      <v-col class="text-start">
        <v-btn
          outlined
          @click="$emit('create-internal-wallet')"
          class="ignore-vuetify-classes mx-2"
          :title="$t('exoplatform.wallet.label.meedsBtnTitle')">
          {{ $t('exoplatform.wallet.label.internalWallet') }}
          <img
            class="ml-2"
            :src="`/wallet/images/meeds.svg`"
            alt="Meeds"
            width="16">
        </v-btn>
        <div class="subtitle px-4 py-4 walletInformation">{{ $t('exoplatform.wallet.label.createInternalWallet') }}</div>
      </v-col>
      <v-col class="text-start">
        <v-btn
          :disaled="!isMetamaskInstalled"
          @click="connectToMetamask"
          outlined
          class="ignore-vuetify-classes mx-2"
          :class="metamaskBtnClass"
          :title="metamaskBtnTitle">
          {{ $t('exoplatform.wallet.button.metamask') }}
          <img
            class="ml-2"
            :src="`/wallet/images/metamask.svg`"
            alt="Metamask"
            width="25">
        </v-btn>
        <div class="subtitle px-4 py-4 walletInformation">
          {{ $t('exoplatform.wallet.label.createMetamaskWallet') }}
          <div class="subtitle walletInformation" v-if="!isMetamaskInstalled">
            {{ $t('exoplatform.wallet.label.createMetamaskWalletLearnMore') }}
            <a
              :href="metamaskInstallLink"
              target="_blank"
              rel="noopener nofollow">{{ metamaskInstallLink }}</a>
          </div>
        </div>
      </v-col>
    </v-row>
  </v-flex>
</template>

<script>
export default {
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
    metamaskInstallLink() {
      return this.isMobile
        && `https://metamask.app.link/dapp/${this.currentSiteLink}`
        || 'https://metamask.io/';
    },
    generatedToken() {
      return this.$root.generatedToken;
    },
    metamaskBtnClass(){
      return this.isMetamaskInstalled && ' ' || 'disabledButton';
    },
    metamaskBtnTitle(){
      return this.isMetamaskInstalled && this.$t('exoplatform.wallet.label.metamask.buttonTitle') || this.$t('exoplatform.wallet.label.metamask.disabledButton');
    }
  },
  methods: {
    connectToMetamask() {
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
        })
        .catch(() => {
          this.savingMetamaskAddress = false;
        });
    },
    retrieveAddress() {
      return window.ethereum.request({ method: 'eth_requestAccounts' })
        .then(retrievedAddress => {
          return retrievedAddress[0];
        });
    },
    signMessage(address) {
      let rawMessage = this.$t('exoplatform.wallet.label.metamask.welcomeMessage', {0: address, 1: this.generatedToken});
      rawMessage = rawMessage.split(/\\n/g).join('\u000A');
      return window.ethereum.request({
        method: 'personal_sign',
        params: [rawMessage, address, ''],
      })
        .then(signedMessage => this.saveProvider('METAMASK', address, rawMessage, signedMessage));
    },
    saveProvider(provider, address, rawMessage, signedMessage) {
      return this.addressRegistry.switchProvider(provider, address, rawMessage, signedMessage)
        .then(() => {
          window.walletSettings.wallet.address = address;
          window.walletSettings.wallet.provider = 'METAMASK';
          this.$emit('configured');
          this.$root.$emit('wallet-notification-alert', {
            type: 'success',
            message: this.$t('exoplatform.wallet.metamask.message.connectedSuccess')
          });
        });
    },  }
};
</script>
