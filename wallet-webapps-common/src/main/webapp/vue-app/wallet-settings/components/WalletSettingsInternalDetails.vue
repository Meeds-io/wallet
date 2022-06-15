<template>
  <div>
    <wallet-reward-password-management
      v-if="displayManagePasswordDetails"
      :wallet="wallet"
      :is-space="isSpace"
      :button-label="$t('exoplatform.wallet.button.resetWalletPassword')"
      display-remember-me
      class="d-flex"
      @back="closeManagePasswordDetails" />
    <template v-else>
      <v-list-item v-if="!isDeleted && walletAddress">
        <v-list-item-content>
          <v-list-item-title class="title text-color">
            {{ $t('exoplatform.wallet.label.managePassword') }}
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ $t('exoplatform.wallet.message.managePasswordDescription') }}
          </v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-btn
            small
            icon
            @click="openManagePasswordDetails">
            <v-icon size="24" class="text-sub-title">
              {{ $vuetify.rtl && 'fa-caret-left' || 'fa-caret-right' }}
            </v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
      <v-divider v-if="!isDeleted && walletAddress" />
      <v-list-item v-if="!isDeleted && walletAddress">
        <v-list-item-content>
          <v-list-item-title class="title text-color">
            {{ $t('exoplatform.wallet.title.backupWalletModal') }}
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ $t('exoplatform.wallet.message.backupWallet') }}
          </v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-btn
            small
            icon
            @click="openWalletBackUpDrawer">
            <i class="uiIconEdit uiIconLightBlue pb-2" aria-hidden="true"></i>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
      <v-divider v-if="!isDeleted && walletAddress" />
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="title text-color">
            {{ $t('exoplatform.wallet.title.restoreWalletModal') }}
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ $t('exoplatform.wallet.message.importNewPrivateKeyMessage') }}
          </v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-btn
            small
            icon
            @click="openWalletImportKeyDrawer">
            <i class="uiIconEdit uiIconLightBlue pb-2" aria-hidden="true"></i>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
      <v-divider v-if="!isDeleted && walletAddress" />
      <v-list-item class="deleteWallet" v-if="!isDeleted && walletAddress">
        <v-list-item-content>
          <v-list-item-title class="title deleteText">
            {{ $t('exoplatform.wallet.title.deleteWalletConfirmationModal') }}
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ $t('exoplatform.wallet.message.deleteWallet') }}
          </v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-btn
            small
            icon
            @click="openDeleteConfirmationModal">
            <i class="uiIconTrash uiIconLightBlue pb-2" aria-hidden="true"></i>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
    </template>
    <wallet-reward-import-key-drawer
      ref="walletImportKey"
      :is-space="isSpace"
      :wallet-address="wallet && wallet.address"
      @configured="$emit('settings-changed'); " />
    <wallet-reward-backup-drawer
      ref="walletBackup"
      class="walletBackup"
      display-complete-message
      no-button />
    <wallet-reward-confirm-dialog
      ref="informationModal"
      :loading="loading"
      :title="$t('exoplatform.wallet.title.deleteWalletConfirmationModal')"
      :title-class="deleteText"
      :message="$t('exoplatform.wallet.message.deleteWalletConfirmationModal')"
      :hide-default-footer="false"
      :ok-label="$t('exoplatform.wallet.button.deleteteConfirm')"
      :cancel-label="$t('exoplatform.wallet.button.cancel')"
      width="400px"
      @ok="deleteWallet" />
  </div>
</template>
<script>
export default {
  props: {
    walletSettings: {
      type: Object,
      default: null,
    },
    isSpace: {
      type: Boolean,
      default: false
    },
  },
  data: () => ({
    displayManagePasswordDetails: false,
    alert: false,
    type: '',
    message: '',
    provider: null
  }),
  computed: {
    wallet() {
      return this.walletSettings && this.walletSettings.wallet;
    },
    walletAddress() {
      return this.walletSettings && this.walletSettings.wallet && this.walletSettings.wallet.address;
    },
    initializationState() {
      return this.wallet && this.wallet.address && this.wallet.initializationState ;
    },
    isDeleted() {
      return this.initializationState === 'DELETED';
    }
  },
  watch: {
    walletSettings: {
      immediate: true,
      handler() {
        this.provider = this.walletSettings && this.walletSettings.wallet && this.walletSettings.wallet.provider;
      },
    },
  },
  methods: {
    openManagePasswordDetails() {
      this.$emit('close-details');
      this.displayManagePasswordDetails = true;
    },
    closeManagePasswordDetails() {
      this.$emit('open-details');
      this.displayManagePasswordDetails = false;
    },
    openWalletImportKeyDrawer() {
      this.$refs.walletImportKey.open();
    },
    openWalletBackUpDrawer() {
      this.$refs.walletBackup.open();
    },
    openDeleteConfirmationModal() {
      this.$refs.informationModal.open();
    },
    deleteWallet() {
      return this.walletUtils.deleteWallet(this.walletSettings.wallet.address)
        .then(() => {
          this.$root.$emit('wallet-notification-alert', {type: 'success',message: this.$t('exoplatform.wallet.message.deleteWalletSuccess')});
          if (this.wallet.type === 'user') {
            return window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet`;
          } else {
            return window.location.href =  `${eXo.env.portal.context}/g/:spaces:${eXo.env.portal.spaceGroup}/${eXo.env.portal.spaceName}/SpaceWallet`;
          }
        }).catch(e => {
          this.$root.$emit('wallet-notification-alert', {type: 'error',message: String(e)});
        });
    },
  },
};
</script>
