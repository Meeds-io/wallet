<template>
  <v-dialog
    id="sendFundsModal"
    v-model="dialog"
    :disabled="disabled"
    attach="#walletDialogsParent"
    content-class="uiPopup with-overflow"
    class="sendFundsModal"
    width="500px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <v-btn
      v-if="icon && !noButton"
      slot="activator"
      :disabled="disabled"
      class="bottomNavigationItem"
      title="Send funds"
      flat
      value="send">
      <span>
        Send
      </span>
      <v-icon>
        send
      </v-icon>
    </v-btn>
    <button
      v-else-if="!noButton"
      slot="activator"
      :disabled="disabled"
      class="btn btn-primary mr-1 mt-2">
      Send
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
            Send Funds
          </span>
      </div>

      <div v-if="error" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <send-funds-form
        ref="sendFundsForm"
        :wallet-address="walletAddress"
        :network-id="networkId"
        :selected-account="selectedOption && selectedOption.value"
        :default-label="defaultLabel"
        :default-message="defaultMessage"
        @success="success"
        @error="$emit('error', $event)"
        @dialog-error="error = $event"
        @sent="addPendingTransaction($event)"
        @pending="$emit('pending', $event)"
        @close="dialog = false">
        <div id="sendFundsAccount" class="selectBoxVuetifyParent">
          <v-combobox
            v-model="selectedOption"
            :items="accountsList"
            attach="#sendFundsAccount"
            label="Select currency"
            placeholder="Select a currency to use for requesting funds"
            absolute
            cache-items />
        </div>
      </send-funds-form>
    </v-card>
  </v-dialog>
</template>

<script>
import SendFundsForm from './SendFundsForm.vue';

import {setDraggable, checkFundRequestStatus} from '../js/WalletUtils.js';

export default {
  components: {
    SendFundsForm,
  },
  props: {
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
    overviewAccounts: {
      type: Array,
      default: function() {
        return [];
      },
    },
    principalAccount: {
      type: String,
      default: function() {
        return null;
      },
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 1;
      },
    },
    networkId: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    icon: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    displayAllAccounts: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    disabled: {
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
    defaultLabel: {
      type: String,
      default: function() {
        return null;
      },
    },
    defaultMessage: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      selectedOption: null,
      error: null,
      dialog: null,
    };
  },
  computed: {
    accountsList() {
      const accountsList = [];
      if (this.accountsDetails && this.refreshIndex > 0) {
        Object.keys(this.accountsDetails).forEach((key) => {
          // Check list of accounts to display switch user preferences
          const isContractOption = this.overviewAccounts && this.overviewAccounts.indexOf(key) > -1;
          // Always allow to display ether option
          const isEtherOption = isContractOption || key === this.walletAddress;
          if (this.displayAllAccounts || isContractOption || isEtherOption) {
            accountsList.push({
              text: this.accountsDetails[key].title,
              value: this.accountsDetails[key],
            });
          }
        });
      }
      return accountsList;
    },
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.error = null;
        this.$nextTick(() => {
          if (!this.selectedOption) {
            const contractAddress = this.principalAccount === 'ether' || this.principalAccount === 'fiat' ? null : this.principalAccount;
            this.prepareSendForm(null, null, null, contractAddress, null, true);
          }
          this.$nextTick(() => {
            setDraggable();
          });
        });
      } else {
        this.selectedOption = null;
      }
    },
  },
  methods: {
    success(...args) {
      this.$emit('success', ...args);
    },
    prepareSendForm(receiver, receiverType, amount, contractAddress, notificationId, keepDialogOpen) {
      if (!this.accountsList || !this.accountsList.length) {
        console.debug('prepareSendForm error - no accounts found');
        return;
      }

      if (receiver && receiverType && notificationId) {
        checkFundRequestStatus(notificationId).then((sent) => {
          this.dialog = !sent;
        });
      } else {
        this.dialog = true;
      }

      this.selectedOption = null;
      let i = 0;
      while (i < this.accountsList.length && !this.selectedOption) {
        const account = this.accountsList[i];
        if (
          account &&
          account.value &&
          // Token account
          ((account.value.isContract && contractAddress && account.value.address.toLowerCase() === contractAddress.toLowerCase()) ||
            // Ether account
            (!account.value.isContract && !contractAddress))
        ) {
          this.selectedOption = this.accountsList[i];
        }
        i++;
      }

      this.$nextTick(() => {
        this.$refs.sendFundsForm.prepareSendForm(receiver, receiverType, amount, contractAddress, notificationId, keepDialogOpen);
      });
    },
  },
};
</script>
