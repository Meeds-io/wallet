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
  <exo-drawer
    ref="walletImportKeyModal"
    :right="!$vuetify.rtl"
    @closed="close">
    <template slot="title">
      <span class="pb-2"> {{ $t('exoplatform.wallet.title.restoreWalletModal') }} </span>
    </template>
    <template slot="content" class="walletRequestFundsModal">
      <v-card-text>
        <div v-if="error" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div>

        <v-form
          ref="form"
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <v-text-field
            v-model="walletPrivateKey"
            :append-icon="walletPrivateKeyShow ? 'mdi-eye' : 'mdi-eye-off'"
            :rules="[rules.priv]"
            :type="walletPrivateKeyShow ? 'text' : 'password'"
            :disabled="loading"
            :label="$t('exoplatform.wallet.label.walletPrivateKey')"
            :placeholder="$t('exoplatform.wallet.label.walletPrivateKeyPlaceholder')"
            name="walletPrivateKey"
            autocomplete="off"
            autofocus
            validate-on-blur
            @click:append="walletPrivateKeyShow = !walletPrivateKeyShow" />
          <v-text-field
            v-model="walletPassword"
            :append-icon="walletPasswordShow ? 'mdi-eye' : 'mdi-eye-off'"
            :rules="[rules.min]"
            :type="walletPasswordShow ? 'text' : 'password'"
            :disabled="loading"
            :label="$t('exoplatform.wallet.label.walletPassword')"
            :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
            name="walletPassword"
            autocomplete="current-passord"
            @click:append="walletPasswordShow = !walletPasswordShow" />
        </v-form>
      </v-card-text>
    </template>
    <template slot="footer">
      <div class="d-flex mr-2">
        <v-spacer />
        <button
          :disabled="loading"
          class="ignore-vuetify-classes btn mx-1"
          @click="close">
          {{ $t('exoplatform.wallet.button.cancel') }}
        </button>
        <button
          :disabled="loading"
          class="ignore-vuetify-classes btn btn-primary"
          @click="importWallet">
          {{ $t('exoplatform.wallet.button.import') }}
        </button>
      </div>
    </template>
  </exo-drawer>
</template>

<script>
import {saveBrowserWalletInstance} from '../js/WalletUtils.js';

export default {
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      walletPrivateKey: '',
      walletPrivateKeyShow: false,
      error: null,
      loading: false,
      walletPassword: null,
      walletPasswordShow: false,
      rules: {
        min: (v) => (v && v.length >= 8) || 'At least 8 characters',
        priv: (v) => (v && (v.length === 66 || v.length === 64)) || 'Exactly 64 or 66 (with "0x") characters are required',
      },
    };
  },
  created() {
    this.walletUtils.initSettings(this.isSpace, true, true)
      .then(() => this.walletUtils.initWeb3(this.isSpace, true));
    this.resetForm();
  },
  methods: {
    resetForm() {
      this.walletPrivateKey = null;
      this.walletPrivateKeyShow = false;
      this.walletPassword = null;
      this.walletPasswordShow = false;
      this.error = null;
      this.loading = false;
      if (this.$refs.form) {
        this.$refs.form.reset();
      }
    },
    importWallet() {
      this.error = null;
      if (!this.$refs.form.validate()) {
        return;
      }
      this.loading = true;
      const thiss = this;
      window.setTimeout(() => {
        try {
          if (thiss.walletPrivateKey.indexOf('0x') < 0) {
            thiss.walletPrivateKey = `0x${thiss.walletPrivateKey}`;
          }
          const wallet = window.localWeb3.eth.accounts.wallet.add(thiss.walletPrivateKey);
          if (!thiss.walletAddress || wallet.address.toLowerCase() === thiss.walletAddress.toLowerCase()) {
            saveBrowserWalletInstance(wallet, this.walletPassword, thiss.isSpace, true, true)
              .then(() => {
                thiss.loading = false;
                thiss.close();
                thiss.$nextTick(() => {
                  thiss.$emit('configured');
                });
              })
              .catch((e) => {
                thiss.loading = false;
                console.error('saveBrowserWalletInstance method - error', e);
                thiss.error = 'Error processing new keys';
              });
          } else {
            thiss.loading = false;
            thiss.error = this.$t('exoplatform.wallet.error.wrongPrivateKey', {0: thiss.walletAddress});
          }
        } catch (e) {
          thiss.loading = false;
          console.error('Error importing private key', e);
          thiss.error = this.$t('exoplatform.wallet.error.errorImportingPrivateKey');
        }
      }, 200);
    },
    open() {
      this.$refs.walletImportKeyModal.open();
    },
    close() {
      this.$refs.walletImportKeyModal.close();
    }
  },
};
</script>
