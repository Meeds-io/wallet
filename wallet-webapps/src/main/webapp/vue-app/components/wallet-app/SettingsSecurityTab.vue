<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <v-card flat>
    <v-card-title primary-title class="pt-0">
      <div>
        <h4 class="no-wrap">
          <v-icon small class="mb-1">fa-unlock</v-icon>
          <span>{{ $t('exoplatform.wallet.label.password') }}</span>
          <v-icon
            v-if="!browserWalletExists"
            :title="$t('exoplatform.wallet.warning.noPrivateKey')"
            color="orange">
            warning
          </v-icon>
        </h4>
        <h5>{{ $t('exoplatform.wallet.message.securityPasswordManagement') }}</h5>
        <reset-modal
          ref="walletResetModal"
          :wallet="wallet"
          :button-label="$t('exoplatform.wallet.button.resetWalletPassword')"
          display-remember-me
          class="d-flex"
          @opened="dialogOpened = true"
          @closed="dialogOpened = false"
          @reseted="$emit('settings-changed')" />
      </div>
    </v-card-title>
    <v-card-title class="pt-0 pb-0">
      <div>
        <h4>
          <v-icon small>fa-key</v-icon>
          {{ $t('exoplatform.wallet.label.encryptionKeys') }}
        </h4>
        <h5>{{ $t('exoplatform.wallet.message.encryptionKeysManagement') }}</h5>
        <button
          :disabled="dialogOpened"
          class="ignore-vuetify-classes btn"
          @click="$emit('manage-keys')">
          <v-icon small>fa-qrcode</v-icon>
          {{ $t('exoplatform.wallet.button.manageKeys') }}
        </button>
        <v-switch
          v-model="hasKeyOnServerSide"
          :disabled="!browserWalletExists || dialogOpened"
          :label="$t('exoplatform.wallet.button.keepMyKeysSafeOnServer')"
          @change="$refs.confirmDialog.open()" />
      </div>
    </v-card-title>

    <confirm-dialog
      ref="confirmDialog"
      :loading="loading"
      :message="confirmMessage"
      :title="confirmTitle"
      :ok-label="confirmOkLabel"
      :cancel-label="$t('exoplatform.wallet.button.cancel')"
      @dialog-opened="dialogOpened = true"
      @dialog-closed="dialogOpened = false"
      @ok="saveKeysOnServer"
      @closed="init" />
  </v-card>
</template>

<script>
export default {
  props: {
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      loading: false,
      settings: false,
      dialogOpened: false,
      hasKeyOnServerSide: false,
      browserWalletExists: false,
    };
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    confirmMessage() {
      return this.hasKeyOnServerSide ? this.$t('exoplatform.wallet.message.storePrivateKeyOnServer') : this.$t('exoplatform.wallet.message.deletePrivateKeyFromServer');
    },
    confirmTitle() {
      return this.hasKeyOnServerSide ? this.$t('exoplatform.wallet.label.storePrivateKeyOnServer') : this.$t('exoplatform.wallet.label.deletePrivateKeyFromServer');
    },
    confirmOkLabel() {
      return this.hasKeyOnServerSide ? this.$t('exoplatform.wallet.button.save') : this.$t('exoplatform.wallet.button.delete');
    },
  },
  methods: {
    init() {
      this.settings = window.walletSettings || {userPreferences: {}};
      this.hasKeyOnServerSide = this.settings.userPreferences.hasKeyOnServerSide;
      this.browserWalletExists = this.settings.browserWalletExists;
      if (this.$refs.walletResetModal) {
        this.$refs.walletResetModal.init();
      }
    },
    saveKeysOnServer() {
      if (this.hasKeyOnServerSide) {
        return this.walletUtils.sendPrivateKeyToServer(this.walletAddress)
          .then(() => this.settings.userPreferences.hasKeyOnServerSide = true)
          .catch(error => {
            console.error('Error occurred', error);
            this.hasKeyOnServerSide = this.settings.userPreferences.hasKeyOnServerSide;
          })
          .finally(() => this.dialogOpened = false);
      } else {
        return this.walletUtils.removeServerSideBackup(this.walletAddress)
          .then(() => this.settings.userPreferences.hasKeyOnServerSide = false)
          .catch(error => {
            console.error('Error occurred', error);
            this.hasKeyOnServerSide = this.settings.userPreferences.hasKeyOnServerSide;
          })
          .finally(() => this.dialogOpened = false);
      }
    },
  }
};
</script>