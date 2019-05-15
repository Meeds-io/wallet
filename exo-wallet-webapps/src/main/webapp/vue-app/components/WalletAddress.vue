<template>
  <v-chip outline class="walletAddressCmp">
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
      @click="ignoreDefaultActions"
      @keydown.esc="reset"
      @keyup.enter="save">
    <a
      v-else
      :href="addressEtherscanLink && `${addressEtherscanLink}${value}` || '#'"
      :title="addressEtherscanLink && 'Open on etherscan' || ''"
      :class="!allowCopy && 'mr-4'"
      target="_blank"
      class="walletAddressLabel ellipsis mr-2">
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
    <v-slide-x-reverse-transition v-if="allowEdit && displayLabel && isAdmin" mode="out-in">
      <v-icon
        id="walletAddressEdit"
        :key="`icon-${isEditing}`"
        :color="isEditing ? 'success' : 'info'"
        class="walletAddressEdit"
        title="Edit label"
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
      isAdmin: null,
      addressEtherscanLink: null,
      labelDetail: {},
      labelDetailToEdit: {},
    };
  },
  created() {
    this.init();
    this.isAdmin = window.walletSettings && window.walletSettings.isAdmin;
    document.addEventListener('exo-wallet-label-changed', this.refresh);
  },
  methods: {
    init() {
      this.refresh();
    },
    refresh() {
      if(!this.value) {
        return;
      }
      this.addressEtherscanLink = getAddressEtherscanlink(window.walletSettings && window.walletSettings.defaultNetworkId);

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
      if(!this.allowEdit) {
        return;
      }
      if(this.isEditing) {
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
      if(!this.allowEdit) {
        return;
      }

      this.loading = true;
      return saveAddressLabel(this.labelDetail)
        .then((labelDetail) => {
          this.labelDetail = labelDetail;

          if(!window.walletSettings.userPreferences.addresesLabels) {
            window.walletSettings.userPreferences.addresesLabels = [];
          }
          const walletAddress = this.value.toLowerCase();
          const labelDetailToChange = window.walletSettings.userPreferences.addresesLabels.find(label => label && label.address && label.address.toLowerCase() === walletAddress);
          if(labelDetailToChange) {
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
        } catch(e) {
          console.debug('Error executing document.execCommand', e);
        }
      }
    },
  },
};
</script>
