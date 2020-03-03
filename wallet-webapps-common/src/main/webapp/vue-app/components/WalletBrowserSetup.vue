<template>
  <v-flex class="text-center white">
    <div
      v-show="!loading"
      id="walletBrowserSetup"
      class="mt-3 mb-3">
      <template v-if="walletAddress">
        <wallet-import-key-modal
          ref="walletImportKeyModal"
          :is-space="isSpace"
          :wallet-address="walletAddress"
          @configured="$emit('configured')" />
      </template>
      <template v-else>
        <v-form
          ref="form"
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <v-text-field
            v-if="!loading"
            v-model="walletPassword"
            :append-icon="walletPasswordShow ? 'mdi-eye' : 'mdi-eye-off'"
            :rules="[rules.min]"
            :type="walletPasswordShow ? 'text' : 'password'"
            :disabled="loading || loadingWalletBrowser"
            :label="$t('exoplatform.wallet.label.walletPassword')"
            :placeholder="$t('exoplatform.wallet.label.setWalletPasswordPlaceholder')"
            name="walletPassword"
            required
            autocomplete="new-passord"
            @click:append="walletPasswordShow = !walletPasswordShow" />
        </v-form>
        <button
          :disabled="loadingWalletBrowser"
          class="ignore-vuetify-classes btn btn-primary"
          @click="createWallet()">
          {{ $t('exoplatform.wallet.button.createNewWallet') }}
        </button>
      </template>
    </div>
  </v-flex>
</template>

<script>
import WalletImportKeyModal from './WalletImportKeyModal.vue';

import {initEmptyWeb3Instance, saveBrowserWalletInstance} from '../js/WalletUtils.js';

export default {
  components: {
    WalletImportKeyModal,
  },
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
      walletPasswordShow: null,
      loadingWalletBrowser: false,
      rules: {
        min: (v) => (v && v.length >= 8) || this.$t('exoplatform.wallet.warning.atLeast8Chars'),
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
        console.debug('saveBrowserWalletInstance method - error: no address in created wallet', wallet);
        this.$emit('error', this.$t('exoplatform.wallet.error.errorCreatingWallet'));
        return;
      }
      return saveBrowserWalletInstance(wallet[0], this.walletPassword, this.isSpace)
        .then(() => {
          this.$emit('configured');
          this.loadingWalletBrowser = false;
        })
        .catch((e) => {
          console.debug('saveBrowserWalletInstance method - error', e);
          this.$emit('error', this.$t('exoplatform.wallet.error.errorSavingWallet'));
          this.loadingWalletBrowser = false;
        });
    },
  },
};
</script>
