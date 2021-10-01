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
  <v-flex class="text-center white">
    <div
      v-show="!loading"
      id="walletBrowserSetup"
      class="mt-3 mb-3">
      <template v-if="walletAddress">
        <wallet-reward-import-key-modal
          ref="walletImportKeyModal"
          :is-space="isSpace"
          :wallet-address="walletAddress"
          @configured="$emit('configured')" />
      </template>
      <template v-else>
        <v-row class="justify-center">
          <div class="title px-4 py-4">{{ $t('exoplatform.wallet.label.createWalletInvitation') }}</div>
          <div class="subtitle px-4 py-4">{{ $t('exoplatform.wallet.label.exoWalletDescription') }}</div>
        </v-row>
        <v-form
          ref="form"
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <v-text-field
            v-if="!loading"
            v-model="walletPassword"
            :rules="[rules.min]"
            type="password"
            :disabled="loading || loadingWalletBrowser"
            :placeholder="$t('exoplatform.wallet.label.setWalletPasswordPlaceholder')"
            name="walletPassword"
            required
            outlined
            autocomplete="new-password" />
          <v-text-field
            v-if="!loading"
            v-model="confirmWalletPassword"
            :rules="[rules.passwordMatching]"
            type="password"
            :disabled="loading || loadingWalletBrowser"
            :placeholder="$t('exoplatform.wallet.label.confirmWalletPasswordPlaceholder')"
            name="confirmWalletPassword"
            required
            outlined
            autocomplete="new-password" />
        </v-form>
        <v-btn
          block
          :disabled="loadingWalletBrowser"
          class="ignore-vuetify-classes btn btn-primary justify-center px-4 py-4 mt-4"
          @click="createWallet()">
          {{ $t('exoplatform.wallet.button.createNewWallet') }}
        </v-btn>
      </template>
    </div>
  </v-flex>
</template>

<script>
import {initEmptyWeb3Instance, saveBrowserWalletInstance} from '../js/WalletUtils.js';

export default {
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isSpaceAdministrator: {
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
    loading: {
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
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      },
    },
  },
  data() {
    return {
      walletPassword: null,
      confirmWalletPassword: null,
      walletPasswordShow: null,
      loadingWalletBrowser: false,
      rules: {
        min: (v) => (v && v.length >= 8) || this.$t('exoplatform.wallet.warning.atLeast8Chars'),
        passwordMatching: (v) => (v && v === this.walletPassword) || this.$t('exoplatform.wallet.warning.passwordNotMatching'),
      },
    };
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address;
    }
  },
  watch: {
    refreshIndex(newValue, oldValue) {
      if (newValue > oldValue) {
        this.$nextTick(this.init);
      }
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.walletPassword = null;
      this.loadingWalletBrowser = false;
      if (!window.localWeb3) {
        initEmptyWeb3Instance();
      }
    },
    createWallet() {
      if (!this.$refs.form.validate()) {
        return;
      }
      this.loadingWalletBrowser = true;

      // Use set timeout to allow refresh UI by disabling button before processing
      window.setTimeout(this.createWalletInstance, 200);
    },
    createWalletInstance() {
      const entropy = this.walletPassword + Math.random();
      const wallet = window.localWeb3.eth.accounts.wallet.create(1, entropy);
      const walletAddress = wallet && wallet[0] && wallet[0].address;
      if (!walletAddress) {
        console.error('saveBrowserWalletInstance method - error: no address in created wallet', wallet);
        this.$emit('error', this.$t('exoplatform.wallet.error.errorCreatingWallet'));
        return;
      }
      return saveBrowserWalletInstance(wallet[0], this.walletPassword, this.isSpace)
        .then(() => {
          this.$emit('configured');
          this.loadingWalletBrowser = false;
        })
        .catch((e) => {
          console.error('saveBrowserWalletInstance method - error', e);
          this.$emit('error', this.$t('exoplatform.wallet.error.errorSavingWallet'));
          this.loadingWalletBrowser = false;
        });
    },
  },
};
</script>
