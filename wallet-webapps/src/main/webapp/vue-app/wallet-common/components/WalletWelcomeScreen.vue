<template>
  <v-flex
    id="walletWelcomeScreen"
    class="text-start mx-4 pa-0">
    <v-row no-gutters>
      <v-col>
        <div class="text-title mb-4">{{ $t('exoplatform.wallet.label.createWalletInvitation') }}</div>
        <div class="d-flex flex-column">
          <div class="walletInformation">{{ $t('exoplatform.wallet.label.createWalletInvitation.description') }}</div>
          <div class="walletInformation pt-1">{{ $t('exoplatform.wallet.label.createWalletInvitation.spendDescription') }}</div>
        </div>
        <div>
          <div class="font-weight-bold my-4">{{ $t('exoplatform.wallet.label.createWalletInvitation.secure') }}</div>
          <ul class="ps-2">
            <li class="walletInformation">- {{ $t('exoplatform.wallet.label.createWalletInvitation.firstDescription') }}</li>
            <li class="walletInformation">- {{ $t('exoplatform.wallet.label.createWalletInvitation.lastDescription') }}</li>
          </ul>
        </div>
      </v-col>
    </v-row>
    <v-row class="justify-space-between mt-4" no-gutters>
      <v-col class="text-start">
        <v-btn
          outlined
          @click="$emit('create-internal-wallet')"
          class="ignore-vuetify-classes font-weight-bold"
          :title="$t('exoplatform.wallet.label.meedsBtnTitle')">
          {{ $t('exoplatform.wallet.label.internalWallet') }}
          <img
            class="ms-2"
            :src="`/wallet/images/meeds.svg`"
            alt="Meeds"
            width="16">
        </v-btn>
        <div class="py-4 walletInformation">{{ $t('exoplatform.wallet.label.createInternalWallet') }}</div>
      </v-col>
      <v-col cols="auto" class="mx-2" />
      <v-col class="text-start">
        <v-btn
          :disaled="!isMetamaskInstalled"
          @click="connectToMetamask"
          outlined
          class="ignore-vuetify-classes font-weight-bold"
          :class="metamaskBtnClass"
          :title="metamaskBtnTitle">
          {{ $t('exoplatform.wallet.button.metamask') }}
          <img
            class="ms-2"
            :src="`/wallet/images/metamask.svg`"
            alt="Metamask"
            width="25">
        </v-btn>
        <div class="py-4 walletInformation">
          {{ $t('exoplatform.wallet.label.createMetamaskWallet') }}
          <div class="walletInformation" v-if="!isMetamaskInstalled">
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
        .catch(e => {
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
