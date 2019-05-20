<template>
  <v-dialog
    id="sendTokensModal"
    v-model="dialog"
    :disabled="disabled"
    attach="#walletDialogsParent"
    content-class="uiPopup with-overflow"
    class="sendTokensModal"
    width="600px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <v-bottom-nav
      v-if="useNavigation"
      slot="activator"
      :disabled="disabled"
      :value="true"
      color="white"
      class="elevation-0 buttomNavigation">
      <v-btn
        :disabled="disabled"
        flat
        value="send">
        <span>
          Send tokens
        </span>
        <v-icon>
          send
        </v-icon>
      </v-btn>
    </v-bottom-nav>
    <button
      v-else-if="!noButton"
      slot="activator"
      :disabled="disabled"
      class="btn btn-primary mt-1 mb-1">
      Send tokens
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
            Send Tokens
          </span>
      </div>
      <send-tokens-form
        ref="sendTokensForm"
        :account="account"
        :contract-details="contractDetails"
        class="pt-4"
        @sent="$emit('sent', $event, contractDetails)"
        @close="dialog = false"
        @error="$emit('error', $event)" />
    </v-card>
  </v-dialog>
</template>

<script>
import SendTokensForm from './SendTokensForm.vue';

import {setDraggable} from '../js/WalletUtils.js';

export default {
  components: {
    SendTokensForm,
  },
  props: {
    account: {
      type: String,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
    noButton: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    useNavigation: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isReadonly: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    open: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    contract: {
      type: Object,
      default: function() {
        return {};
      },
    },
  },
  data() {
    return {
      dialog: null,
    };
  },
  computed: {
    balance() {
      return this.contractDetails && this.contractDetails.balance;
    },
    etherBalance() {
      return this.contractDetails && this.contractDetails.etherBalance;
    },
    disabled() {
      return this.isReadonly || !this.balance || this.balance === 0 || (typeof this.balance === 'string' && (!this.balance.length || this.balance.trim() === '0')) || !this.etherBalance || this.etherBalance === 0 || (typeof this.etherBalance === 'string' && (!this.etherBalance.length || this.etherBalance.trim() === '0'));
    },
  },
  watch: {
    open() {
      this.dialog = this.open;
      if (this.open) {
        this.$nextTick(() => {
          setDraggable();
        });
      }
    },
    dialog() {
      if (this.dialog) {
        this.$refs.sendTokensForm.init();
      } else {
        this.$emit('close');
      }
    },
  },
};
</script>
