<template>
  <v-dialog
    v-model="dialog"
    attach="#walletDialogsParent"
    content-class="uiPopup with-overflow"
    width="500px"
    max-width="100vw"
    @keydown.esc="dialog = false">
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
            {{ title }}
          </span>
      </div>
      <v-card-text>
        <qr-code
          ref="qrCode"
          :net-id="netId"
          :from="from"
          :to="to"
          :is-contract="isContract"
          :function-payable="functionPayable"
          :function-name="functionName"
          :args-names="argsNames"
          :args-types="argsTypes"
          :args-values="argsValues"
          :amount="amount"
          :open="open"
          :information="information" />
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script>
import QrCode from './QRCode.vue';

import {setDraggable} from '../js/WalletUtils.js';

export default {
  components: {
    QrCode,
  },
  props: {
    title: {
      type: String,
      default: function() {
        return null;
      },
    },
    information: {
      type: String,
      default: function() {
        return null;
      },
    },
    open: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    amount: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    from: {
      type: String,
      default: function() {
        return null;
      },
    },
    to: {
      type: String,
      default: function() {
        return null;
      },
    },
    isContract: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    functionName: {
      type: String,
      default: function() {
        return null;
      },
    },
    functionPayable: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    argsNames: {
      type: Array,
      default: function() {
        return [];
      },
    },
    argsTypes: {
      type: Array,
      default: function() {
        return [];
      },
    },
    argsValues: {
      type: Array,
      default: function() {
        return [];
      },
    },
  },
  data() {
    return {
      dialog: false,
      netId: null,
    };
  },
  watch: {
    open() {
      if (this.open) {
        this.dialog = true;
        this.computeCanvas();
        this.$nextTick(() => {
          setDraggable();
        });
      }
    },
    dialog() {
      if (!this.dialog) {
        this.netId = null;
        this.$emit('close');
      }
    },
  },
  methods: {
    computeCanvas() {
      this.$refs.qrCode.computeCanvas();
    },
  },
};
</script>
