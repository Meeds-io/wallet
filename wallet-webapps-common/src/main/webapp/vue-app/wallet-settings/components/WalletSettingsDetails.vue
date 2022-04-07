<template>
  <v-card
    v-if="displayed"
    class="walletSetting"
    flat>
    <template v-if="!hideContent">
      <v-card-title>
        <v-toolbar class="border-box-sizing" flat>
          <v-btn
            class="mx-1"
            icon
            height="36"
            width="36"
            @click="$emit('back')">
            <v-icon size="20">
              {{ $vuetify.rtl && 'mdi-arrow-right' || 'mdi-arrow-left' }}
            </v-icon>
          </v-btn>
          <v-toolbar-title class="ps-0">
            {{ title }}
          </v-toolbar-title>
          <v-spacer />
        </v-toolbar>
      </v-card-title>
      <v-card-subtitle class="mx-14">
        {{ description }}
      </v-card-subtitle>
    </template>

    <v-list class="mx-8">
      <wallet-settings-internal-details
        v-if="isInternalWallet"
        :is-space="isSpace"
        :wallet-settings="walletSettings"
        @close-details="hideContent = true"
        @open-details="hideContent = false" />
      <v-list-item v-if="!isDeleted && !hideContent" class="manageKey">
        <v-list-item-content>
          <v-list-item-title class="title text-color">
            {{ $t('exoplatform.wallet.label.ethereumAddress') }}
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ $t('exoplatform.wallet.message.manageDigitalKey') }}
          </v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action v-if="walletAddress">
          <wallet-reward-qr-code
            ref="qrCode"
            :to="walletAddress"
            :title="$t('exoplatform.wallet.title.addressQRCode')" />
          <wallet-reward-address :value="walletAddress" :allow-edit="false" />
        </v-list-item-action>
      </v-list-item>
    </v-list>
  </v-card>
</template>
<script>
export default {
  props: {
    walletSettings: {
      type: Object,
      default: null,
    },
    title: {
      type: String,
      default: null,
    },
    description: {
      type: String,
      default: null,
    },
    isSpace: {
      type: Boolean,
      default: false
    },
  },
  data: () => ({
    id: `WalletSettingsDetails${parseInt(Math.random() * 10000)}`,
    displayed: true,
    hideContent: false,
    alert: false,
    type: '',
    message: '',
  }),
  computed: {
    wallet () {
      return this.walletSettings.wallet;
    },
    provider () {
      return this.wallet && this.wallet.provider;
    },
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    initializationState() {
      return this.walletAddress && this.wallet.initializationState ;
    },
    isDeleted() {
      return this.initializationState === 'DELETED';
    },
    isInternalWallet() {
      return this.provider === 'INTERNAL_WALLET';
    },
  },
  created(){
    this.$root.$on('show-alert', message => {
      this.displayMessage(message);
    });
  },
  methods: {
    openWalletImportKeyDrawer() {
      this.$refs.walletImportKey.open();
    },
    openWalletBackUpDrawer() {
      this.$refs.walletBackup.open();
    },
    displayMessage(message) {
      this.message=message.message;
      this.type=message.type;
      this.alert = true;
      window.setTimeout(() => this.alert = false, 5000);
    },
    openDeleteConfirmationModal() {
      this.$refs.informationModal.open();
    },
    deleteWallet() {
      return this.walletUtils.deleteWallet(this.walletSettings.wallet.address)
        .then(() => {
          this.$root.$emit('show-alert', {type: 'success',message: this.$t('exoplatform.wallet.message.deleteWalletSuccess')});
          if (this.wallet.type === 'user') {
            return window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet`;
          } else {
            return window.location.href =  `${eXo.env.portal.context}/g/:spaces:${eXo.env.portal.spaceGroup}/${eXo.env.portal.spaceName}/SpaceWallet`;
          }
        }).catch(e => {
          this.$root.$emit('show-alert', {type: 'error',message: String(e)});
        });
    },
  },
};
</script>
