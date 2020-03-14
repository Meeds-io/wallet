<template>
  <v-dialog
    id="walletBackupModal" 
    v-model="dialog"
    :disabled="disabled"
    class="fixLinkHeight"
    content-class="uiPopup with-overflow walletDialog"
    width="500px"
    max-width="100vw"
    hide-overlay
    @keydown.esc="dialog = false">
    <template v-if="!noButton" v-slot:activator="{ on }">
      <a href="javascript:void(0);" v-on="on">
        {{ $t('exoplatform.wallet.title.backupWalletModal') }}
      </a>
    </template>
    <v-card class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix" draggable="true">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="ignore-vuetify-classes PopupTitle popupTitle">
            {{ $t('exoplatform.wallet.button.backupWallet') }}
          </span>
      </div>
      <v-card-text>
        <div v-if="error" class="alert alert-error">
          <i class="uiIconError"></i> {{ error }}
        </div>

        <div
          id="walletBackupDetailedWarning"
          class="alert alert-warning v-content">
          <p>
            <i class="uiIconWarning"></i>
            {{ $t('exoplatform.wallet.warning.backupWalletPart1') }} <br>
            {{ $t('exoplatform.wallet.warning.backupWalletPart2') }} <br>
            {{ $t('exoplatform.wallet.warning.backupWalletPart3') }} <br>
          </p>
        </div>

        <v-form
          ref="form"
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <v-text-field
            v-if="dialog && !walletPrivateKey"
            ref="walletPassword"
            v-model="walletPassword"
            :append-icon="walletPasswordShow ? 'mdi-eye' : 'mdi-eye-off'"
            :rules="[rules.min]"
            :type="walletPasswordShow ? 'text' : 'password'"
            name="walletPassword"
            label="Current wallet password"
            placeholder="Enter your current wallet password"
            autocomplete="current-passord"
            autofocus
            validate-on-blur
            @click:append="walletPasswordShow = !walletPasswordShow" />
        </v-form>
      </v-card-text>
      <v-card-text v-if="walletPrivateKey" class="text-center">
        <code v-text="walletPrivateKey"></code>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button
          v-if="walletPrivateKey"
          class="ignore-vuetify-classes btn btn-primary mr-1"
          @click="walletBackedUp">
          {{ $t('exoplatform.wallet.button.backedUp') }}!
        </button>
        <button
          v-if="!walletPrivateKey"
          :disabled="loading"
          class="ignore-vuetify-classes btn btn-primary mr-1"
          @click="showPrivateKey">
          {{ $t('exoplatform.wallet.button.displayPrivateKey') }}!
        </button>
        <button
          v-if="!walletPrivateKey"
          :disabled="loading"
          class="ignore-vuetify-classes btn"
          @click="dialog = false">
          {{ $t('exoplatform.wallet.button.close') }}!
        </button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>

import {unlockBrowserWallet, lockBrowserWallet, getCurrentBrowserWallet, hashCode, setWalletBackedUp} from '../js/WalletUtils.js';

export default {
  props: {
    noButton: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      dialog: false,
      error: null,
      walletPassword: '',
      walletPasswordShow: false,
      walletPrivateKey: null,
      loading: false,
      rules: {
        min: (v) => (v && v.length >= 8) || this.$t('exoplatform.wallet.warning.atLeast8Chars'),
      },
    };
  },
  watch: {
    dialog() {
      if (this.dialog) {
        if (this.$refs.form) {
          this.$refs.form.reset();
        }
        this.init();
      }
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.error = null;
      this.walletPrivateKey = null;
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.loading = false;
    },
    walletBackedUp() {
      this.error = null;
      setWalletBackedUp()
        .then(wallet => {
          this.$emit('backed-up');
          this.dialog = false;
        }).catch((error) => {
          this.error = String(error);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    showPrivateKey() {
      this.error = null;
      if (!this.$refs.form.validate()) {
        return;
      }
      this.loading = true;
      window.setTimeout(this.unlockBrowserWallet, 200);
    },
    unlockBrowserWallet() {
      const unlocked = unlockBrowserWallet(hashCode(this.walletPassword));
      try {
        if (unlocked) {
          const wallet = getCurrentBrowserWallet();
          if (!wallet || !wallet.privateKey) {
            this.error = this.$t('exoplatform.wallet.error.walletNotFound');
            return;
          }
          this.loading = false;
          this.walletPrivateKey = wallet.privateKey;
        } else {
          this.error = this.$t('exoplatform.wallet.warning.wrongPassword');
        }
      } finally {
        this.loading = false;
        if (unlocked) {
          lockBrowserWallet();
        }
      }
    },
  },
};
</script>
