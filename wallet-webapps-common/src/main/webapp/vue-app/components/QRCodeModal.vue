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
  <v-dialog
    v-model="dialog"
    content-class="uiPopup with-overflow walletDialog"
    width="500px"
    max-width="100vw"
    @keydown.esc="dialog = false">
    <v-card class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="ignore-vuetify-classes PopupTitle popupTitle">
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
export default {
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
      if (this.$refs && this.$refs.qrCode) {
        this.$refs.qrCode.computeCanvas();
      }
    },
  },
};
</script>
