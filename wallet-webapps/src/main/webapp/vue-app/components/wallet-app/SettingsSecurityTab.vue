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
        <reset-modal
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
export default {
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
      settings: false,
      hasKeyOnServerSide: false,
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
      this.settings = window.walletSettings || {userPreferences: {}};
      this.hasKeyOnServerSide = this.settings.userPreferences.hasKeyOnServerSide;
      this.browserWalletExists = this.settings.browserWalletExists;
      if (this.$refs.walletResetModal) {
        this.$refs.walletResetModal.init();
      }
    },
    saveKeysOnServer() {
      if (this.hasKeyOnServerSide) {
        return this.walletUtils.sendPrivateKeyToServer(this.walletAddress)
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
        return this.walletUtils.removeServerSideBackup(this.walletAddress);
      }
    },
  }
}
</script>