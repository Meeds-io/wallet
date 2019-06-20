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
    <button
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
            ignore-current-user
            @item-selected="recipient = $event" />
  
          <v-text-field
            v-model.number="amount"
            :disabled="loading"
            :rules="amoutRules"
            name="amount"
            label="Amount"
            placeholder="Select a suggested amount to request funds"
            class="mt-4" />
  
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
        </button>
        <button
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
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      recipient: null,
      amount: null,
      error: null,
      requestMessage: '',
      loading: false,
      dialog: false,
      amoutRules: [(v) => !!v || 'Field is required', (v) => (!isNaN(parseFloat(v)) && isFinite(v) && v > 0) || 'Invalid amount'],
    };
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
        this.$nextTick(() => {
          setDraggable();
        });
      }
    },
  },
  methods: {
    requestFunds() {
      if (!this.$refs.form.validate()) {
        return;
      }
      if (!this.contractDetails) {
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
          contract: this.contractDetails && this.contractDetails.address,
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
