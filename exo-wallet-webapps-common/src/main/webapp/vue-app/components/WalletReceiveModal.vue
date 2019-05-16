<template>
  <v-dialog
    v-model="dialog"
    attach="#walletDialogsParent"
    content-class="uiPopup with-overflow"
    class="walletReceiveModal"
    width="500px"
    max-width="100vw"
    @keydown.esc="dialog = false">
    <v-btn
      v-if="icon"
      slot="activator"
      :disabled="disabled"
      class="bottomNavigationItem"
      title="Receive funds"
      flat
      value="receive">
      <span>
        Receive
      </span>
      <v-icon>
        fa-hand-holding-usd
      </v-icon>
    </v-btn>
    <button
      v-else
      slot="activator"
      class="btn ml-1 mt-2">
      Receive
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
            Receive funds
          </span>
      </div>
      <v-card-text class="text-xs-center">
        <qr-code
          ref="qrCode"
          :to="walletAddress"
          title="Address QR Code"
          information="You can send this Wallet address or QR code to other users to send you ether or tokens" />
        <wallet-address
          ref="walletAddress"
          :value="walletAddress"
          :allow-edit="false" />
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button class="btn" @click="dialog = false">
          Close
        </button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import WalletAddress from './WalletAddress.vue';
import QrCode from './QRCode.vue';

import {setDraggable} from '../js/WalletUtils.js';

export default {
  components: {
    WalletAddress,
    QrCode,
  },
  props: {
    icon: {
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
      dialog: false,
    };
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.$refs.qrCode.computeCanvas();
        this.$nextTick(() => {
          setDraggable();
        });
      }
    },
  },
};
</script>
