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
  <v-app>
    <template v-if="displayed">
      <v-card
        class="ma-4 walletManagePassword"
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
              {{ $t('exoplatform.wallet.label.mangePassword') }}
            </v-toolbar-title>
            <v-spacer />
          </v-toolbar>
        </v-card-title>
        <v-list class="mx-8 mt-n5">
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title class="title text-color">
                {{ $t('exoplatform.wallet.label.rememberPasswordInBrowser') }}
              </v-list-item-title>
              <v-list-item-subtitle>
                {{ $t('exoplatform.wallet.message.rememberMyPassword') }}
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-list-item-action>
              <v-switch
                v-model="rememberPasswordStored"
                @change="changeRememberMe" />
            </v-list-item-action>
          </v-list-item>
          <v-divider />
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title class="title text-color">
                {{ $t('exoplatform.wallet.button.changeWalletPassword') }}
              </v-list-item-title>
              <v-list-item-subtitle>
                {{ $t('exoplatform.wallet.message.managePassword') }}
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-list-item-action>
              <v-btn
                small
                icon
                @click="openDrawer">
                <i class="uiIconEdit uiIconLightBlue pb-2"></i>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
        </v-list>
      </v-card>


      <exo-drawer
        ref="ManagePasswordDrawer"
        right
        @closed="closeDrawer">
        <template slot="title">
          <span class="ignore-vuetify-classes PopupTitle popupTitle">
            {{ rememberPasswordToChange ? $t('exoplatform.wallet.warning.requiredPassword') : $t('exoplatform.wallet.button.changeWalletPassword') }}
          </span>
        </template>
        <template slot="content">
          <v-card-text class="walletManagePasswordDrawer">
            <div v-if="error" class="alert alert-error">
              <i class="uiIconError"></i> {{ error }}
            </div>

            <v-form
              ref="form"
              @submit="
                $event.preventDefault();
                $event.stopPropagation();
              ">
              <div class="managePasswordLabels mb-5">
                <v-text-field
                  v-model="walletPassword"
                  :append-icon="walletPasswordShow ? 'mdi-eye' : 'mdi-eye-off'"
                  :rules="[rules.min]"
                  :type="walletPasswordShow ? 'text' : 'password'"
                  :disabled="loading"
                  :label="rememberPasswordToChange ? $t('exoplatform.wallet.label.walletPassword') : $t('exoplatform.wallet.label.currentWalletPassword')"
                  :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
                  name="walletPassword"
                  autocomplete="current-passord"
                  autofocus
                  validate-on-blur
                  @click:append="walletPasswordShow = !walletPasswordShow" />

                <v-text-field
                  v-if="!rememberPasswordToChange "
                  v-model="newWalletPassword"
                  :append-icon="newWalletPasswordShow ? 'mdi-eye' : 'mdi-eye-off'"
                  :rules="[rules.min]"
                  :type="newWalletPasswordShow ? 'text' : 'password'"
                  :disabled="loading"
                  :label="$t('exoplatform.wallet.label.newWalletPassword')"
                  :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
                  name="newWalletPassword"
                  autocomplete="new-passord"
                  @click:append="newWalletPasswordShow = !newWalletPasswordShow" />

                <v-text-field
                  v-if="!rememberPasswordToChange "
                  v-model="confirmNewWalletPassword"
                  :append-icon="newWalletPasswordShow ? 'mdi-eye' : 'mdi-eye-off'"
                  :rules="[rules.passwordMatching]"
                  :type="newWalletPasswordShow ? 'text' : 'password'"
                  :disabled="loading"
                  :label="$t('exoplatform.wallet.label.newWalletPasswordConfirm')"
                  :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholderConfirm')"
                  name="confirmNewWalletPassword"
                  autocomplete="new-passord"
                  @click:append="newWalletPasswordShow = !newWalletPasswordShow" />
              </div>
              <v-switch
                v-model="rememberPassword"
                :label="$t('exoplatform.wallet.message.rememberMyPassword')"
                class="v-input--reverse mt-2" />
            </v-form>
          </v-card-text>
        </template>
        <template slot="footer">
          <div class="d-flex mr-2">
            <v-spacer />
            <button
              :disabled="loading"
              class="ignore-vuetify-classes btn"
              @click="closeDrawer">
              {{ $t('exoplatform.wallet.button.close') }}
            </button>
            <button
              :disabled="loading"
              class="ignore-vuetify-classes btn btn-primary me-1 mx-1"
              @click="resetWallet()">
              {{ $t('exoplatform.wallet.button.confirm') }}
            </button>
          </div>
        </template>
      </exo-drawer>
    </template>
  </v-app>
</template>

<script>
import {unlockBrowserWallet, saveBrowserWallet, hashCode, rememberPassword} from '../js/WalletUtils.js';

import {sendPrivateKeyToServer} from '../js/WalletUtils.js';

export default {
  props: {
    displayRememberMe: {
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
    buttonLabel: {
      type: String,
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
      id: `WalletManagePassword${parseInt(Math.random() * 10000)}`,
      displayed: true,
      loading: false,
      browserWalletExists: false,
      hasKeyOnServerSide: false,
      error: null,
      walletPassword: null,
      walletPasswordShow: false,
      newWalletPassword: null,
      confirmNewWalletPassword: null,
      newWalletPasswordShow: false,
      browserWalletDecrypted: false,
      rememberPasswordStored: false,
      rememberPasswordToChange: false,
      rememberPassword: true,
      rules: {
        min: (v) => (v && v.length >= 8) || this.$t('exoplatform.wallet.warning.atLeast8Chars'),
        passwordMatching: (v) => (v && v === this.newWalletPassword) || this.$t('exoplatform.wallet.warning.passwordNotMatching'),
      },
    };
  },
  computed: {
    backedUp () {
      return this.wallet && this.wallet.backedUp;
    }
  },
  created() {
    this.init();},
  methods: {
    init() {
      this.error = null;
      this.walletPassword = null;
      this.walletPasswordShow = false;
      this.newWalletPassword = null;
      this.newWalletPasswordShow = false;
      this.rememberPassword = true;
      this.rememberPasswordToChange = false;
      return this.walletUtils.initSettings(this.isSpace, true, true)
        .then(() => {
          if (!window.walletSettings) {
            this.forceUpdate();
            throw new Error(this.$t('exoplatform.wallet.error.emptySettings'));
          }
          this.hasKeyOnServerSide = window.walletSettings && window.walletSettings.userPreferences && window.walletSettings.userPreferences.hasKeyOnServerSide;
          this.browserWalletExists = window.walletSettings.browserWalletExists;
          this.rememberPasswordStored = this.browserWalletExists && window.walletSettings.storedPassword === true;
        })
        .then(() => this.walletUtils.initWeb3(this.isSpace, true));
    },
    changeRememberMe() {
      if (this.loading) {
        return;
      }

      if (this.rememberPasswordStored) {
        this.rememberPasswordToChange = true;
        this.openDrawer();
      } else {
        rememberPassword(false);
      }
    },
    resetWallet() {
      this.error = null;
      if (!this.$refs.form.validate()) {
        return;
      }
      this.loading = true;
      const thiss = this;
      // Check if original password is ok
      window.setTimeout(() => {
        const unlocked = unlockBrowserWallet(hashCode(thiss.walletPassword));
        if (unlocked) {
          try {
            if (thiss.rememberPasswordToChange) {
              rememberPassword(true, hashCode(thiss.walletPassword));
            } else {
              saveBrowserWallet(thiss.newWalletPassword, null, null, thiss.rememberPassword);
              if (this.hasKeyOnServerSide) {
                sendPrivateKeyToServer(null, this.walletPassword, this.newWalletPassword)
                  .then((result, error) => {
                    if (error) {
                      throw error;
                    }
                    if (this.rememberPassword) {
                      rememberPassword(true, hashCode(this.newWalletPassword));
                    }
                    this.$emit('reseted');
                    this.closeDrawer();
                  })
                  .catch(e => {
                    console.error('Error saving private key on server', e);
                    this.error = String(e);
                  })
                  .finally(() => {
                    this.loading = false;
                  });
              }
              thiss.$emit('reseted');
            }
            thiss.closeDrawer();
            thiss.loading = false;
          } catch (e) {
            thiss.loading = false;
            console.error('saveWallet method error', e);
            thiss.error = String(e);
            return;
          }
        } else {
          thiss.loading = false;
          thiss.error = this.$t('exoplatform.wallet.warning.wrongPassword');
        }
      }, 200);
    },
    openDrawer(){
      this.$refs.ManagePasswordDrawer.open();
    },
    closeDrawer(){
      this.init();
      this.$forceUpdate();
      this.$refs.ManagePasswordDrawer.close();
    },
  },
};
</script>
