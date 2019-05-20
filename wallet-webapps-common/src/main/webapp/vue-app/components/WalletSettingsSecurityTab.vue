<template>
  <v-card>
    <v-card-title primary-title>
      <div>
        <h4 class="no-wrap">
          <v-icon small class="mb-1">fa-unlock</v-icon>
          <span>Password</span>
          <v-tooltip v-if="!browserWalletExists" bottom>
            <v-icon slot="activator" color="orange">warning</v-icon>
            <span>
              No wallet keys was found, thus you can't manage passwords.
            </span>
          </v-tooltip>
        </h4>
        <h5>Add an extra layer of security by using a dedicated password for your wallet.</h5>
        <wallet-reset-modal
          ref="walletResetModal"
          button-label="Reset wallet password"
          display-remember-me
          @reseted="$emit('settings-changed')" />
      </div>
    </v-card-title>
    <v-card-title class="pt-0 pb-0">
      <div>
        <h4>
          <v-icon small>fa-key</v-icon>
          Encryption keys
        </h4>
        <h5>Keys are used to secure your wallet transactions. We can keep them safe for you on server, or you can manage them yourself.</h5>
        <button class="btn" @click="$emit('manage-keys')">
          <v-icon small>fa-qrcode</v-icon>
          Manage Keys
        </button>
        <v-switch
          v-model="hasKeyOnServerSide"
          :disabled="!browserWalletExists"
          label="Keep my keys safe on server"
          @change="$refs.confirmDialog.open()" />
        <div class="no-wrap">
          <v-switch
            v-model="useMetamask"
            label="Use metamask"
            @change="changeMetamaskOption" />
          <v-tooltip bottom>
            <information-bubble slot="activator">
              <v-btn
                icon
                small
                class="mt-0 mb-2">
                <v-icon small color="primary">fa-info-circle</v-icon>
              </v-btn>
              <template slot="content">
                <a href="https://metamask.io" target="_blank">MetaMask</a>
                is a browser extension to connect to Ethereum Network,
                that let you manage several blockchain accounts.
              </template>
            </information-bubble>
            <span>
              <a href="https://metamask.io" target="_blank">MetaMask</a>
              is a browser extension to connect to Ethereum Network,
              that let you manage several blockchain accounts.
            </span>
          </v-tooltip>
        </div>
      </div>
    </v-card-title>

    <confirm-dialog
      ref="confirmDialog"
      :loading="loading"
      :message="confirmMessage"
      :title="confirmTitle"
      :ok-label="confirmOkLabel"
      cancel-label="Cancel"
      @ok="saveKeysOnServer"
      @closed="init" />
  </v-card>
</template>

<script>
import WalletResetModal from './WalletResetModal.vue';
import InformationBubble from './InformationBubble.vue';
import ConfirmDialog from './ConfirmDialog.vue';

import {enableMetamask, disableMetamask, removeServerSideBackup, sendPrivateKeyToServer} from '../js/WalletUtils.js';

export default {
  components: {
    WalletResetModal,
    InformationBubble,
    ConfirmDialog,
  },
  props: {
    walletAddress: {
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
      loading: false,
      hasKeyOnServerSide: false,
      useMetamask: false,
      browserWalletExists: false,
    };
  },
  computed: {
    confirmMessage() {
      return this.hasKeyOnServerSide ? 'Would you like to store private keys on server?' : 'Would you like to delete stored keys from server ?'
    },
    confirmTitle() {
      return this.hasKeyOnServerSide ? 'Save keys on server?' : 'Remove keys from server?'
    },
    confirmOkLabel() {
      return this.hasKeyOnServerSide ? 'Save' : 'Delete'
    },
  },
  methods: {
    init() {
      this.hasKeyOnServerSide = window.walletSettings && window.walletSettings.userPreferences && window.walletSettings.userPreferences.hasKeyOnServerSide;
      this.useMetamask = window.walletSettings && window.walletSettings.userPreferences && window.walletSettings.userPreferences.useMetamask;
      this.browserWalletExists = window.walletSettings.browserWalletExists;
      this.$refs.walletResetModal.init();
    },
    changeMetamaskOption() {
      if (this.useMetamask) {
        enableMetamask(this.isSpace);
      } else {
        disableMetamask(this.isSpace);
      }
      this.$emit('settings-changed');
    },
    saveKeysOnServer() {
      if (this.hasKeyOnServerSide) {
        return sendPrivateKeyToServer(this.walletAddress)
          .then((result, error) => {
            if (error) {
              throw error;
            }
          })
          .catch(error => {
            console.debug("Error occurred", error);
            this.hasKeyOnServerSide = false;
          });
      } else {
        return removeServerSideBackup(this.walletAddress);
      }
    },
  }
}
</script>