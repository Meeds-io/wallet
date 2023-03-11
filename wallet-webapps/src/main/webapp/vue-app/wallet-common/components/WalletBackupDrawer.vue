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
    ref="walletBackup"
    :right="!$vuetify.rtl"
    @closed="close">
    <template slot="title">
      <span class="pb-2"> {{ $t('exoplatform.wallet.title.backupWalletModal') }} </span>
    </template>
    <template slot="content">
      <v-card-text>
        <div
          class="text-sub-title mx-2"
          id="walletBackupDetailedWarning">
          {{ $t('exoplatform.wallet.warning.backupWalletPart1') }}
          {{ $t('exoplatform.wallet.warning.backupWalletPart2') }} <br>
          {{ $t('exoplatform.wallet.warning.backupWalletPart3') }} <br>
        </div>
        <p v-if="!walletPrivateKey" class="backupWalletText mx-2"> {{ $t('exoplatform.wallet.warning.backupWalletPart4') }} <br> </p>
        <v-form
          ref="form"
          class="mx-2"
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <v-text-field
            v-if="!walletPrivateKey"
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
      <v-card-text v-if="walletPrivateKey">
        <wallet-reward-address
          :value="walletPrivateKey"
          :allow-edit="false"
          :is-link="false"
          class="mx-2" />
      </v-card-text>
    </template>
    <template slot="footer">
      <div class="d-flex mr-2">
        <v-spacer />
        <button
          :disabled="loading"
          class="ignore-vuetify-classes btn"
          @click="close">
          {{ $t('exoplatform.wallet.button.close') }}
        </button>
        <button
          v-if="walletPrivateKey"
          class="ignore-vuetify-classes btn btn-primary me-1 mx-1"
          @click="walletBackedUp">
          {{ $t('exoplatform.wallet.button.backedUp') }}
        </button>
        <button
          v-if="!walletPrivateKey"
          :disabled="loading"
          class="ignore-vuetify-classes btn btn-primary me-1 mx-1"
          @click="showPrivateKey">
          {{ $t('exoplatform.wallet.button.displayPrivateKey') }}
        </button>
      </div>
    </template>
  </exo-drawer>
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
      walletPassword: '',
      walletPasswordShow: false,
      walletPrivateKey: null,
      loading: false,
      rules: {
        min: (v) => (v && v.length >= 8) || this.$t('exoplatform.wallet.warning.atLeast8Chars'),
      },
    };
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.walletPrivateKey = null;
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.loading = false;
    },
    walletBackedUp() {
      setWalletBackedUp()
        .then(() => {
          this.$emit('backed-up');
          this.close();
        }).catch((error) => {
          this.$root.$emit('wallet-notification-alert', {
            type: 'error',
            message: String(error)
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },
    showPrivateKey() {
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
            this.$root.$emit('wallet-notification-alert', {
              type: 'error',
              message: this.$t('exoplatform.wallet.error.walletNotFound')
            });
            return;
          }
          this.loading = false;
          this.walletPrivateKey = wallet.privateKey;
        } else {
          this.$root.$emit('wallet-notification-alert', {
            type: 'error',
            message: this.$t('exoplatform.wallet.warning.wrongPassword')
          });
        }
      } finally {
        this.loading = false;
        if (unlocked) {
          lockBrowserWallet();
        }
      }
    },
    open() {
      this.$refs.walletBackup.open();
    },
    close() {
      this.$refs.walletBackup.close();
      this.init();
    },
  },
};
</script>
