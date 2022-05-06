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
  <v-flex id="walletSetup" class="text-center">
    <wallet-reward-backup-modal
      ref="walletBackupModal"
      class="me-3"
      display-complete-message
      no-button
      @backed-up="$emit('backed-up')" />
    <div v-if="displayWalletCreationToolbar" class="walletBackupDetailedWarning alert alert-info">
      <i class="uiIconInfo"></i> <span v-if="isSpace">
        {{ $t('exoplatform.wallet.warning.notPrivateKey') }}.
        <strong>
          {{ $t('exoplatform.wallet.title.spaceWallet') }}
        </strong> {{ $t('exoplatform.wallet.warning.isReadOnlyMode') }}.
      </span> <span v-else>
        {{ $t('exoplatform.wallet.warning.notPrivateKey') }}.
        <strong>
          {{ $t('exoplatform.wallet.title.yourWallet') }}
        </strong> {{ $t('exoplatform.wallet.warning.isReadOnlyMode') }}.
      </span>
      <a
        v-if="!displayWalletSetup"
        href="javascript:void(0);"
        @click="displayWalletSetupActions()">
        {{ $t('exoplatform.wallet.label.moreOptions') }}
      </a>
    </div>

    <div v-if="displayWalletNotExistingYet" class="alert alert-info">
      <i class="uiIconInfo"></i> {{ $t('exoplatform.wallet.info.spaceWalletNotCreatedYet') }}
    </div>
    <wallet-welcome-screen
      v-if="displayWelcomeScreen && !displayWalletBrowserSetup && !isSpace"
      @create-internal-wallet="displayWalletBrowserSetup = true"
      @configured="refresh()" />
    <wallet-reward-browser-setup
      v-if="displayWalletBrowserSetup || isSpace"
      ref="walletBrowserSetup"
      :is-space="isSpace"
      :is-space-administrator="isSpaceAdministrator"
      :wallet="wallet"
      :refresh-index="refreshIndex"
      :is-administration="isAdministration"
      :loading="loading"
      :initialization-state="initializationState"
      @configured="refresh()"
      @loading="$emit('loading')"
      @end-loading="$emit('end-loading')"
      @error="$emit('error', $event)" />
  </v-flex>
</template>

<script>
export default {
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    loading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isAdministration: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    initializationState: {
      type: String,
      default: function() {
        return null;
      },
    },
    isMinimized: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      isReadOnly: false,
      browserWalletExists: false,
      displayWalletSetup: false,
      displayWalletBrowserSetup: false,
    };
  },
  computed: {
    backedUp () {
      return this.wallet && this.wallet.backedUp;
    },
    displayWalletNotExistingYet() {
      return !this.loading && !this.isAdministration && this.isSpace && !this.isSpaceAdministrator && !this.walletAddress;
    },
    isSpaceAdministrator() {
      return this.wallet && this.wallet.spaceAdministrator;
    },
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    displayWalletCreationToolbar() {
      return this.wallet && this.wallet.provider === 'INTERNAL_WALLET' && !this.loading && this.walletAddress && !this.browserWalletExists && this.isReadOnly && (!this.isSpace || this.isSpaceAdministrator);
    },
    displayWalletBackup() {
      return !this.loading && !this.isAdministration && this.walletAddress && this.browserWalletExists && !this.backedUp && this.initializationState !== 'DELETED' && (this.wallet && this.wallet.provider === 'INTERNAL_WALLET');
    },
    displayWelcomeScreen() {
      return this.displayWalletSetup && (this.wallet && !this.wallet.address ||  this.initializationState === 'DELETED');
    },
  },
  watch: {
    refreshIndex() {
      this.init();
    },
    displayWalletBackup() {
      if (this.displayWalletBackup && this.$refs && this.$refs.walletBackupModal) {
        this.$refs.walletBackupModal.dialog = true;
      }
    },
  },
  methods: {
    refresh() {
      this.$emit('refresh');
    },
    init() {
      if (!window.walletSettings) {
        return;
      }
      this.isReadOnly = window.walletSettings.isReadOnly;
      this.browserWalletExists = window.walletSettings.browserWalletExists;
      this.displayWalletSetup = (!this.walletAddress && (!this.isSpace || this.isSpaceAdministrator)) || this.initializationState === 'DELETED';

      this.$nextTick(() => {
        if (this.$refs.walletBrowserSetup) {
          this.$refs.walletBrowserSetup.init();
        }
      });
    },
    displayWalletSetupActions() {
      this.displayWalletSetup = true;
    },
  },
};
</script>
