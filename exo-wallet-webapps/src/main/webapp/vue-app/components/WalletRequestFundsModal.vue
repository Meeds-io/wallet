<template>
  <v-dialog
    v-model="dialog"
    attach="#walletDialogsParent"
    content-class="uiPopup with-overflow"
    class="walletRequestFundsModal"
    width="500px"
    max-width="100vw"
    draggable="true"
    @keydown.esc="dialog = false">
    <v-btn
      v-if="icon"
      slot="activator"
      :disabled="disabled"
      class="bottomNavigationItem"
      title="Request funds"
      flat
      value="request">
      <span>
        Request
      </span>
      <v-icon>
        fa-comment-dollar
      </v-icon>
    </v-btn>
    <button
      v-else
      slot="activator"
      class="btn ml-1 mt-2">
      Request
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
            Request funds
          </span>
      </div> <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <v-card-text>
        <v-form ref="form">
          <address-auto-complete
            ref="autocomplete"
            :disabled="loading"
            :autofocus="dialog"
            input-label="Recipient"
            input-placeholder="Select a recipient for your funds request"
            required
            @item-selected="recipient = $event" />
  
          <v-container
            flat
            fluid
            grid-list-lg
            class="mt-4 pl-2">
            <v-layout row wrap>
              <v-text-field
                v-model.number="amount"
                :disabled="loading"
                name="amount"
                label="Amount"
                placeholder="Select a suggested amount to request funds" />
  
              <div id="requestFundsAccount" class="selectBoxVuetifyParent ml-1">
                <v-combobox
                  v-model="selectedOption"
                  :items="accountsList"
                  attach="#requestFundsAccount"
                  label="Select currency"
                  placeholder="Select a currency to use for requesting funds"
                  cache-items />
              </div>
            </v-layout>
          </v-container>
  
          <v-textarea
            id="requestMessage"
            v-model="requestMessage"
            :disabled="loading"
            name="requestMessage"
            label="Request message (Optional)"
            placeholder="You can enter a custom message to send with your request"
            class="mt-4"
            rows="7"
            flat
            no-resize />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button
          :disabled="loading"
          class="btn btn-primary"
          @click="requestFunds">
          Send request
        </button> <button
          :disabled="loading"
          class="btn ml-2"
          @click="dialog = false">
          Close
        </button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import AddressAutoComplete from './AddressAutoComplete.vue';

import {setDraggable} from '../js/WalletUtils.js';

export default {
  components: {
    AddressAutoComplete,
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
        return 0;
      },
    },
  },
  data() {
    return {
      selectedOption: null,
      recipient: null,
      amount: null,
      error: null,
      requestMessage: '',
      loading: false,
      dialog: false,
    };
  },
  computed: {
    selectedAccount() {
      return this.selectedOption && this.selectedOption.value;
    },
    accountsList() {
      const accountsList = [];
      if (this.accountsDetails && this.refreshIndex > 0) {
        Object.keys(this.accountsDetails).forEach((key) => {
          // Check list of accounts to display switch user preferences
          const isContractOption = this.overviewAccounts.indexOf(key) > -1;
          // Always allow to display ether option
          // const isEtherOption = isContractOption || (key === this.walletAddress && (this.overviewAccounts.indexOf('ether') > -1 || this.overviewAccounts.indexOf('fiat') > -1));
          const isEtherOption = isContractOption || key === this.walletAddress;
          if (isContractOption || isEtherOption) {
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
        this.requestMessage = '';
        this.recipient = null;
        this.amount = null;
        if (this.$refs && this.$refs.autocomplete) {
          this.$refs.autocomplete.clear();
        }
        const contractAddress = this.principalAccount === 'ether' || this.principalAccount === 'fiat' ? null : this.principalAccount;
        this.selectedOption = this.accountsList.find((account) => account.value && account.value.address && ((account.value.isContract && account.value.address === contractAddress) || (!account.value.isContract && !contractAddress)));
        this.$nextTick(() => {
          setDraggable();
        });
      }
    },
  },
  methods: {
    requestFunds() {
      if (!this.selectedAccount) {
        this.error = 'Please select a valid account';
        return;
      }

      if (!this.recipient) {
        this.error = 'Please select a receipient to your request';
        return;
      }

      this.loading = true;
      fetch('/portal/rest/wallet/api/account/requestFunds', {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          address: this.walletAddress,
          receipient: this.recipient.id,
          receipientType: this.recipient.type,
          contract: this.selectedAccount.isContract ? this.selectedAccount.address : null,
          amount: this.amount,
          message: this.requestMessage,
        }),
      })
        .then((resp) => {
          if (resp && resp.ok) {
            this.dialog = false;
          } else {
            this.error = 'Error requesting funds';
          }
          this.loading = false;
        })
        .catch((e) => {
          console.debug('requestFunds method - error', e);
          this.error = `Error while proceeding: ${e}`;
          this.loading = false;
        });
    },
  },
};
</script>
