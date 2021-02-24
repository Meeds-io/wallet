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
  <v-chip class="walletAddressCmp">
    <v-btn
      v-if="allowCopy"
      id="copy"
      title="Copy address"
      class="ml-0 mr-0 mb-0 mt-0"
      icon
      small
      @click="copyToClipboard">
      <v-icon size="12" dark>
        fa-copy
      </v-icon>
    </v-btn>
    <input
      v-if="isEditing && allowEdit"
      ref="labelDetailEditInput"
      v-model="labelDetail.label"
      :disabled="loading"
      name="addressLabel"
      placeholder="Label"
      class="walletAddressLabelInput mr-2"
      autofocus
      validate-on-blur
      @click="ignoreDefaultActions"
      @keydown.esc="reset"
      @keyup.enter="save">
    <a
      v-else
      :href="addressEtherscanLink && `${addressEtherscanLink}${value}` || '#'"
      :title="addressEtherscanLink && $t('exoplatform.wallet.label.openOnEtherscan') || ''"
      :class="!allowCopy && 'mr-4'"
      target="_blank"
      class="walletAddressLabel text-truncate mr-2">
      <template v-if="displayLabel && labelDetail && labelDetail.label">
        {{ labelDetail.label }}
      </template>
      <template v-else-if="name">
        {{ name }}
      </template>
      <template v-else>
        {{ value }}
      </template>
    </a>
    <v-slide-x-reverse-transition v-if="allowEdit && displayLabel" mode="out-in">
      <v-icon
        id="walletAddressEdit"
        :key="`icon-${isEditing}`"
        :color="isEditing ? 'success' : 'info'"
        :title="$t('exoplatform.wallet.label.editAddressLabel')"
        class="walletAddressEdit"
        size="16"
        @click="editOrSave">
        {{ isEditing ? 'fa-check-circle' : 'fa-pencil-alt' }}
      </v-icon>
    </v-slide-x-reverse-transition>

    <input
      v-if="allowCopy"
      ref="clipboardInput"
      v-model="value"
      class="copyToClipboardInput"
      type="text">
  </v-chip>
</template>

<script>
import {saveAddressLabel} from '../js/AddressRegistry.js';
import {getAddressEtherscanlink} from '../js/WalletUtils.js';

export default {
  props: {
    name: {
      type: String,
      default: function() {
        return null;
      },
    },
    value: {
      type: String,
      default: function() {
        return null;
      },
    },
    allowCopy: {
      type: Boolean,
      default: function() {
        return true;
      },
    },
    allowEdit: {
      type: Boolean,
      default: function() {
        return true;
      },
    },
    displayLabel: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      isEditing: false,
      loading: false,
      addressEtherscanLink: null,
      labelDetail: {},
      labelDetailToEdit: {},
    };
  },
  created() {
    this.init();
    document.addEventListener('exo-wallet-label-changed', this.refresh);
  },
  methods: {
    init() {
      this.refresh();
    },
    refresh() {
      if (!this.value) {
        return;
      }
      this.addressEtherscanLink = getAddressEtherscanlink(window.walletSettings && window.walletSettings.network.id);

      if (!window.walletSettings.userPreferences
          || !window.walletSettings.userPreferences.addresesLabels) {
        this.labelDetail = {
          address: this.value.toLowerCase(),
        };
        return;
      }
      const walletAddress = this.value.toLowerCase();

      this.labelDetail = window.walletSettings.userPreferences.addresesLabels.find(label => label && label.address && label.address.toLowerCase() === walletAddress) || {address: walletAddress};
      this.labelDetail = Object.assign({}, this.labelDetail);
    },
    ignoreDefaultActions(event) {
      event.preventDefault();
      event.stopPropagation();
    },
    editOrSave(event) {
      this.ignoreDefaultActions(event);
      if (!this.allowEdit) {
        return;
      }
      if (this.isEditing) {
        return this.save();
      } else {
        this.isEditing = !this.isEditing;
        this.$nextTick(() => this.$refs.labelDetailEditInput && this.$refs.labelDetailEditInput.focus());
      }
    },
    reset() {
      this.refresh();
      this.isEditing = false;
    },
    save() {
      if (!this.allowEdit) {
        return;
      }

      this.loading = true;
      return saveAddressLabel(this.labelDetail)
        .then((labelDetail) => {
          this.labelDetail = labelDetail;

          if (!window.walletSettings.userPreferences.addresesLabels) {
            window.walletSettings.userPreferences.addresesLabels = [];
          }
          const walletAddress = this.value.toLowerCase();
          const labelDetailToChange = window.walletSettings.userPreferences.addresesLabels.find(label => label && label.address && label.address.toLowerCase() === walletAddress);
          if (labelDetailToChange) {
            Object.assign(labelDetailToChange, labelDetail);
          } else {
            window.walletSettings.userPreferences.addresesLabels.push(this.labelDetail);
          }
          this.isEditing = false;

          document.dispatchEvent(new CustomEvent('exo-wallet-label-changed'));
        })
        .finally(() => {
          this.loading = false;
        });
    },
    copyToClipboard(event) {
      this.ignoreDefaultActions(event);
      this.$refs.clipboardInput.select();
      if (document.execCommand) {
        try {
          document.execCommand('copy');
        } catch (e) {
          console.error('Error executing document.execCommand', e);
        }
      }
    },
  },
};
</script>
