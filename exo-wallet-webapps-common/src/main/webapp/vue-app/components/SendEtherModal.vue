<template>
  <v-dialog
    id="sendEtherModal"
    v-model="dialog"
    :disabled="disabled"
    attach="#walletDialogsParent"
    content-class="uiPopup with-overflow"
    class="sendEtherModal"
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
          Send Ether
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
      :dark="!disabled"
      class="btn btn-primary mt-1 mb-1">
      Send Ether
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
            Send Ether
          </span>
      </div>
      <send-ether-form
        ref="sendEtherForm"
        :account="account"
        :balance="balance"
        class="pt-4"
        @success="$emit('success', $event)"
        @sent="$emit('sent', $event)"
        @close="dialog = false"
        @error="$emit('error', $event)" />
    </v-card>
  </v-dialog>
</template>

<script>
import SendEtherForm from './SendEtherForm.vue';

import {setDraggable} from '../js/WalletUtils.js';

export default {
  components: {
    SendEtherForm,
  },
  props: {
    account: {
      type: String,
      default: function() {
        return null;
      },
    },
    isReadonly: {
      type: Boolean,
      default: function() {
        return false;
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
    open: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    balance: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    recipient: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      dialog: null,
    };
  },
  computed: {
    disabled() {
      return this.isReadonly || !this.balance || Number(this.balance) === 0;
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
        this.$refs.sendEtherForm.init(this.recipient);
      } else {
        this.$emit('close');
      }
    },
  },
};
</script>
