<template>
  <v-card
    class="text-xs-center pr-3 pl-3 pt-2"
    flat>
    <v-card-title>
      The following settings manages the funds amounts to send to wallets newly created.
      The initial ether amount is preconfigured to allow users to use their wallets using {{ contractDetails && contractDetails.name }}.
    </v-card-title>
    <v-card-text>
      <v-text-field
        v-model="etherAmountLabel"
        name="etherAmount"
        label="Ether initial fund"
        required
        readonly
        placeholder="Select an amount of tokens to send"
        title="Minimum ether amount to send to wallets to enable it"
        @input="$emit('amount-selected', amount)" />

      <v-text-field
        v-model.number="tokenAmount"
        :label="`${contractDetails && contractDetails.name} initial fund`"
        :disabled="loading"
        name="tokenAmount"
        required
        placeholder="Select an amount of tokens to send"
        @input="$emit('amount-selected', amount)" />

      <v-textarea
        id="requestMessage"
        v-model="requestMessage"
        name="requestMessage"
        label="Initial funds default message"
        placeholder="You can enter a default message to send with initial funds"
        class="mt-4 mb-0"
        rows="7"
        flat
        no-resize />
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <button class="btn btn-primary mb-3" @click="save">
        Save
      </button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>
<script>
export default {
  props: {
    loading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      etherAmount: 0,
      tokenAmount: 0,
      requestMessage: null,
      isLoadingSuggestions: false,
    };
  },
  computed: {
    etherAmountLabel() {
      return this.etherAmountInFiat ? `${this.etherAmount} (${this.etherAmountInFiat} ${window.walletSettings.fiatSymbol})` : this.etherAmount;
    },
    etherAmountInFiat() {
      return (this.etherAmount && this.walletUtils.etherToFiat(this.etherAmount)) || 0;
    },
  },
  watch: {
    contractDetails() {
      this.reloadInitialFunds();
    },
  },
  methods: {
    init() {
      const initialFunds = window.walletSettings.initialFunds || {};
      this.requestMessage = initialFunds.requestMessage;
      this.reloadInitialFunds();
    },
    reloadInitialFunds() {
      const initialFunds = window.walletSettings.initialFunds || {};

      this.etherAmount = initialFunds.etherAmount || 0;
      this.tokenAmount = initialFunds.tokenAmount || 0;
    },
    save() {
      const initialFunds = {
        requestMessage: this.requestMessage,
        etherAmount: this.etherAmount,
        tokenAmount: this.tokenAmount,
      };

      this.loading = true;
      return fetch('/portal/rest/wallet/api/settings/saveInitialFunds', {
        method: 'POST',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(initialFunds),
      })
        .then((resp) => {
          if (resp && resp.ok) {
            return resp.text();
          } else {
            throw new Error('Error saving global settings');
          }
        })
        .then(() => this.$emit('saved', initialFunds))
        .catch((e) => {
          this.loading = false;
          console.debug('fetch settings - error', e);
          this.error = 'Error saving global settings';
        });
    },
  },
};
</script>
