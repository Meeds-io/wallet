<template>
  <v-app>
    <template v-if="displayed">
      <v-card
        v-if="!displayManagePasswordDetails"
        class="walletSetting"
        flat>
        <v-card-title>
          <v-toolbar
            class="border-box-sizing"
            flat>
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
              {{ $t('exoplatform.wallet.label.settings') }}
            </v-toolbar-title>
            <v-spacer />
          </v-toolbar>
        </v-card-title>
        <v-card-subtitle class="mx-14 mn-5">
          {{ $t('exoplatform.wallet.message.settingsDescription') }}
        </v-card-subtitle>

        <v-list class="mx-8">
          <v-list-item v-if="wallet && wallet.address && wallet.initializationState !== 'DELETED'">
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
          <v-divider v-if="wallet && wallet.address && wallet.initializationState !== 'DELETED'" />
          <v-list-item v-if="wallet && wallet.address && wallet.initializationState !== 'DELETED'">
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
                <i class="uiIconEdit uiIconLightBlue pb-2"></i>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
          <v-divider v-if="wallet && wallet.address && wallet.initializationState !== 'DELETED'" />
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
                <i class="uiIconEdit uiIconLightBlue pb-2"></i>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
          <v-divider v-if="wallet && wallet.address && wallet.initializationState !== 'DELETED'" />
          <v-list-item class="deleteWallet">
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
                <i class="uiIconTrash uiIconLightBlue pb-2"></i>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
          <v-divider v-if="wallet && wallet.address && wallet.initializationState !== 'DELETED'" />
          <v-list-item class="manageKey" v-if="wallet && wallet.address && wallet.initializationState !== 'DELETED'">
            <v-list-item-content>
              <v-list-item-title class="title text-color">
                {{ $t('exoplatform.wallet.label.ethereumAddress') }}
              </v-list-item-title>
              <v-list-item-subtitle>
                {{ $t('exoplatform.wallet.message.manageDigitalKey') }}
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-list-item-action>
              <wallet-reward-qr-code
                ref="qrCode"
                :to="wallet && wallet.address"
                :title="$t('exoplatform.wallet.title.addressQRCode')" />
              <wallet-reward-address :value="wallet && wallet.address" :allow-edit="false" />
            </v-list-item-action>
          </v-list-item>
        </v-list>
      </v-card>
      <wallet-reward-password-management
        v-if="displayManagePasswordDetails"
        :wallet="wallet"
        :is-space="isSpace"
        :button-label="$t('exoplatform.wallet.button.resetWalletPassword')"
        display-remember-me
        class="d-flex"
        @back="closeManagePasswordDetails" />
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
      <v-alert
        v-model="alert"
        :type="type"
        dismissible>
        {{ message }}
      </v-alert>
      <wallet-reward-confirm-dialog
        ref="informationModal"
        :loading="loading"
        :title="$t('exoplatform.wallet.title.deleteWalletConfirmationModal')"
        title-class="deleteText"
        :message="$t('exoplatform.wallet.message.deleteWalletConfirmationModal')"
        :hide-default-footer="false"
        :ok-label="$t('exoplatform.wallet.button.confirm')"
        :cancel-label="$t('exoplatform.wallet.button.cancel')"
        width="400px"
        @ok="deleteWallet" />
    </template>
  </v-app>
</template>
<script>
export default {
  props: {
    walletDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
    isSpace: {
      type: Boolean,
      default: false
    },
  },
  data: () => ({
    id: `WalletSettingsDetails${parseInt(Math.random() * 10000)}`,
    displayed: true,
    displayManagePasswordDetails: false,
    alert: false,
    type: '',
    message: '',
  }),
  computed: {
    wallet () {
      return this.walletDetails;
    }
  },
  created(){
    this.$root.$on('show-alert', message => {
      this.displayMessage(message);
    });
  },
  methods: {
    openManagePasswordDetails() {
      this.displayManagePasswordDetails = true;
    },
    closeManagePasswordDetails() {
      this.displayManagePasswordDetails = false;
    },
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
      return this.walletUtils.deleteWallet(this.walletDetails.address)
        .then(() => {
          this.$root.$emit('show-alert', {type: 'success',message: this.$t('exoplatform.wallet.message.deleteWalletSuccess')});
          return window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet`;
        }).catch(e => {
          this.$root.$emit('show-alert', {type: 'error',message: String(e)});
        });
    },
  },
};
</script>
