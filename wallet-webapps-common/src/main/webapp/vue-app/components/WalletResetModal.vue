<template>
  <v-dialog
    v-model="dialog"
    :disabled="disabled"
    attach="#walletSecurityDialogsParent"
    content-class="uiPopup with-overflow"
    class="walletResetModal"
    width="500px"
    max-width="100vw"
    hide-overlay
    @keydown.esc="close">
    <template v-if="displayRememberMe" v-slot:activator="{ on }">
      <div
        @click="
          $event.preventDefault();
          $event.stopPropagation();
        ">
        <template>
          <button
            :disabled="!browserWalletExists || dialog"
            class="ignore-vuetify-classes btn"
            v-on="on">
            {{ $t('exoplatform.wallet.button.changeWalletPassword') }}
          </button>
          <v-switch
            v-model="rememberPasswordStored"
            :disabled="!browserWalletExists || dialog"
            :label="$t('exoplatform.wallet.label.rememberPasswordInBrowser')"
            @change="changeRememberMe" />
        </template>
      </div>
    </template>
    <template v-else v-slot:activator="{ on }">
      <v-btn
        text
        v-on="on">
        {{ buttonLabel }}
      </v-btn>
    </template>

    <v-card class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="close"></a> <span class="ignore-vuetify-classes PopupTitle popupTitle">
            {{ rememberPasswordToChange ? $t('exoplatform.wallet.warning.requiredPassword') : $t('exoplatform.wallet.button.resetWalletPassword') }}
          </span>
      </div>
      <v-card-title v-show="loading" class="pb-0">
        <v-spacer />
        <v-progress-circular
          color="primary"
          indeterminate
          size="20" />
        <v-spacer />
      </v-card-title>
      <v-card-text>
        <div v-if="error" class="alert alert-error">
          <i class="uiIconError"></i> {{ error }}
        </div>

        <div v-if="!backedUp && !rememberPasswordToChange" class="alert alert-warning v-content">
          <i id="backupWarningWhenSetPassword" class="uiIconWarning"></i> {{ $t('exoplatform.wallet.info.backupWalletRecommendation') }}
        </div>

        <v-form
          ref="form"
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <v-text-field
            v-if="dialog"
            v-model="walletPassword"
            :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
            :rules="[rules.min]"
            :type="walletPasswordShow ? 'text' : 'password'"
            :disabled="loading"
            :label="rememberPasswordToChange ? $t('exoplatform.wallet.label.walletPassword') : $t('exoplatform.wallet.label.currentWalletPassword')"
            :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
            name="walletPassword"
            counter
            autocomplete="current-passord"
            autofocus
            validate-on-blur
            @click:append="walletPasswordShow = !walletPasswordShow" />

          <v-text-field
            v-if="!rememberPasswordToChange && dialog"
            v-model="newWalletPassword"
            :append-icon="newWalletPasswordShow ? 'visibility_off' : 'visibility'"
            :rules="[rules.min]"
            :type="newWalletPasswordShow ? 'text' : 'password'"
            :disabled="loading"
            :label="$t('exoplatform.wallet.label.newWalletPassword')"
            :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
            name="newWalletPassword"
            counter
            autocomplete="new-passord"
            @click:append="newWalletPasswordShow = !newWalletPasswordShow" />

          <v-switch
            v-if="!rememberPasswordToChange"
            v-model="rememberPassword"
            :label="$t('exoplatform.wallet.label.rememberPasswordInBrowser')"
            class="mt-1" />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button
          :disabled="loading"
          class="ignore-vuetify-classes btn btn-primary mr-1"
          @click="resetWallet()">
          {{ $t('exoplatform.wallet.button.confirm') }}
        </button>
        <button
          :disabled="loading"
          class="ignore-vuetify-classes btn"
          @click="close">
          {{ $t('exoplatform.wallet.button.close') }}
        </button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
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
  },
  data() {
    return {
      dialog: false,
      loading: false,
      browserWalletExists: false,
      hasKeyOnServerSide: false,
      error: null,
      walletPassword: null,
      walletPasswordShow: false,
      newWalletPassword: null,
      newWalletPasswordShow: false,
      browserWalletDecrypted: false,
      rememberPasswordStored: false,
      rememberPasswordToChange: false,
      rememberPassword: false,
      rules: {
        min: (v) => (v && v.length >= 8) || this.$t('exoplatform.wallet.warning.atLeast8Chars'),
      },
    };
  },
  computed: {
    backedUp () {
      return this.wallet && this.wallet.backedUp;
    }
  },
  watch: {
    dialog() {
      this.loading = true;
      if (this.dialog) {
        this.$emit('opened');
        if (this.$refs.form) {
          this.$refs.form.reset();
        }
        if (!this.browserWalletExists) {
          this.error = this.$t('exoplatform.wallet.error.cantResetWallet');
        }
      } else{
        this.$emit('closed');
        if (this.rememberPasswordToChange) {
          this.init();
        }
      }
      this.loading = false;
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.error = null;
      this.walletPassword = null;
      this.walletPasswordShow = false;
      this.newWalletPassword = null;
      this.newWalletPasswordShow = false;
      this.rememberPassword = false;
      this.rememberPasswordToChange = false;
      this.hasKeyOnServerSide = window.walletSettings && window.walletSettings.userPreferences && window.walletSettings.userPreferences.hasKeyOnServerSide;
      this.browserWalletExists = window.walletSettings.browserWalletExists;
      this.rememberPasswordStored = this.browserWalletExists && window.walletSettings.storedPassword === true;
    },
    changeRememberMe() {
      if (this.loading || this.dialog) {
        return;
      }

      if (this.rememberPasswordStored) {
        this.rememberPasswordToChange = true;
        this.dialog = true;
      } else {
        rememberPassword(false);
      }
    },
    close() {
      this.dialog = false;
      this.init();
      this.$forceUpdate();
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
                    this.dialog = false;
                  })
                  .catch(e => {
                    console.debug('Error saving private key on server', e);
                    this.error = String(e)
                  })
                  .finally(() => {
                    this.loading = false;
                  });
              }
              thiss.$emit('reseted');
            }
            thiss.dialog = false;
            thiss.loading = false;
          } catch (e) {
            thiss.loading = false;
            console.debug('saveWallet method error', e);
            thiss.error = String(e);
            return;
          }
        } else {
          thiss.loading = false;
          thiss.error = this.$t('exoplatform.wallet.warning.wrongPassword');
        }
      }, 200);
    },
  },
};
</script>
