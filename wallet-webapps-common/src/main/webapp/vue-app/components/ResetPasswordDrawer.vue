<template>
  <exo-drawer
    ref="ResetPasswordDrawer"
    right
    @closed="closeDrawer">
    <template slot="title">
      <span class="ignore-vuetify-classes PopupTitle popupTitle">
        {{ $t('exoplatform.wallet.resetPassword') }}
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
          {{ $t('exoplatform.wallet.button.cancel') }}
        </button>
        <button
          :disabled="loading"
          class="ignore-vuetify-classes btn btn-primary me-1 mx-1"
          @click="resetPassword()">
          {{ $t('exoplatform.wallet.button.save') }}
        </button>
      </div>
    </template>
  </exo-drawer>
</template>

<script>
import {
  hashCode,
  rememberPassword,
  saveBrowserWallet,
  sendPrivateKeyToServer
} from '../js/WalletUtils';

export default {
  props: {
    rememberPassword: {
      type: Boolean,
      default: true
    },
    hasKeyOnServerSide: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      error: null,
      walletPassword: null,
      walletPasswordShow: false,
      newWalletPassword: null,
      confirmNewWalletPassword: null,
      newWalletPasswordShow: false,
      rememberPasswordToChange: false,
      rules: {
        min: (v) => (v && v.length >= 8) || this.$t('exoplatform.wallet.warning.atLeast8Chars'),
        passwordMatching: (v) => (v && v === this.newWalletPassword) || this.$t('exoplatform.wallet.warning.passwordNotMatching'),
      },
    };
  },
  methods: {
    openDrawer(){
      this.$refs.ResetPasswordDrawer.open();
    },
    closeDrawer(){
      this.$refs.ResetPasswordDrawer.close();
    },
    resetPassword() {
      this.error = null;
      if (!this.$refs.form.validate()) {
        return;
      }
      this.loading = true;
      const thiss = this;
      // Check if original password is ok
      window.setTimeout(() => {
        try {
          saveBrowserWallet(thiss.newWalletPassword, null, null, thiss.rememberPassword, true);
          if (this.hasKeyOnServerSide) {
            sendPrivateKeyToServer(null, null, this.newWalletPassword)
              .then((result, error) => {
                if (error) {
                  throw error;
                }
                if (this.rememberPassword) {
                  rememberPassword(true, hashCode(this.newWalletPassword));
                }
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
          thiss.closeDrawer();
          thiss.loading = false;
        } catch (e) {
          thiss.loading = false;
          console.error('saveWallet method error', e);
          thiss.error = String(e);
          return;
        }
      }, 200);
    },
  }
};
</script>
