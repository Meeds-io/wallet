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
  <v-dialog
    v-model="dialog"
    content-class="uiPopup with-overflow not-draggable walletDialog"
    class="walletSettingsModal"
    width="700px"
    max-width="100vw"
    persistent>
    <v-card class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="ignore-vuetify-classes PopupTitle popupTitle">
            {{ $t('exoplatform.wallet.title.security') }}
          </span>
      </div>
      <v-card-text v-if="loading || appLoading" class="text-center pt-0">
        <v-progress-circular
          color="primary"
          class="mb-2"
          indeterminate />
      </v-card-text>
      <v-card-text v-show="!loading && !appLoading" class="pt-0">
        <div v-if="error && !loading" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div>
        <v-flex>
          <v-tabs
            v-if="selectedTab !== 'security'"
            ref="settingsTabs"
            v-model="selectedTab"
            class="ps-3 pe-3">
            <v-tabs-slider />
            <v-tab
              key="security"
              href="#security">
              {{ $t('exoplatform.wallet.title.security') }}
            </v-tab>
            <v-tab
              v-if="selectedTab === 'keys'"
              key="keys"
              href="#keys">
              {{ $t('exoplatform.wallet.title.manageKeys') }}
            </v-tab>
          </v-tabs>
          <v-tabs-items v-model="selectedTab">
            <v-tab-item
              id="security"
              value="security"
              eager>
              <settings-security-tab
                ref="securityTab"
                :wallet="wallet"
                :is-space="isSpace"
                @settings-changed="$emit('settings-changed')"
                @manage-keys="selectedTab = 'keys'" />
            </v-tab-item>
            <v-tab-item
              id="keys"
              value="keys"
              eager>
              <div>
                <div>
                  <qr-code
                    ref="qrCode"
                    :to="walletAddress"
                    :title="$t('exoplatform.wallet.title.addressQRCode')"
                    :information="$t('exoplatform.wallet.info.addressQRCode')" />
                  <div class="text-center">
                    <wallet-address :value="walletAddress" :allow-edit="false" />
                  </div>
                </div>
                <div v-if="browserWalletExists" class="text-center pt-3">
                  <backup-modal
                    ref="walletBackupModal"
                    :display-complete-message="false"
                    @backed-up="
                      $emit('backed-up');
                      refreshFromSettings();
                    " />
                </div>
                <div class="text-center pt-3">
                  <import-key-modal
                    ref="walletImportKeyModal"
                    :is-space="isSpace"
                    :wallet-address="walletAddress"
                    @configured="
                      $emit('settings-changed');
                      refreshFromSettings();
                    " />
                </div>
              </div>
            </v-tab-item>
          </v-tabs-items>
        </v-flex>
      </v-card-text>
      <v-divider />
      <v-card-actions>
        <v-spacer />
        <button
          :disabled="loading"
          class="ignore-vuetify-classes btn"
          @click="dialog = false">
          {{ $t('exoplatform.wallet.button.close') }}
        </button>
        <v-spacer />
      </v-card-actions>
    </v-card>
    <div id="walletSecurityDialogsParent">
    </div>
  </v-dialog>
</template>

<script>
export default {
  props: {
    title: {
      type: String,
      default: function() {
        return null;
      },
    },
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    appLoading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    open: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    displayResetOption: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      settings: false,
      browserWalletExists: false,
      loading: false,
      dialog: false,
      error: null,
      selectedTab: null,
      accountType: 0,
    };
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address;
    }
  },
  watch: {
    walletAddress() {
      if (this.walletAddress) {
        this.$nextTick(() => {
          this.refreshFromSettings();
          if (this.$refs && this.$refs.qrCode) {
            this.$refs.qrCode.computeCanvas();
          }
        });
      }
    },
    appLoading() {
      if (!this.appLoading) {
        this.refreshFromSettings();
      }
    },
    open() {
      if (this.open) {
        this.error = null;
        this.settings = window.walletSettings || {wallet: {}, userPreferences: {}};
        this.browserWalletExists = this.settings && this.settings.browserWalletExists;

        // Workaround to display slider on first popin open
        if (this.$refs.settingsTabs) {
          this.$refs.settingsTabs.callSlider();
        }

        this.dialog = true;
        this.$nextTick(() => {
          this.refreshFromSettings();
        });
      }
    },
    dialog() {
      if (!this.dialog) {
        this.$emit('close');
      }
    },
  },
  methods: {
    refreshFromSettings() {
      this.settings = window.walletSettings || {wallet: {}, userPreferences: {}};
      if (this.$refs.securityTab) {
        this.$refs.securityTab.init();
      }
    },
  },
};
</script>
