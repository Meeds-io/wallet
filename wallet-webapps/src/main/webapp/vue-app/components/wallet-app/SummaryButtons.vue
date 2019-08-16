<template>
  <v-layout
    row
    wrap
    class="walletSummaryActions mt-1 mb-1">
    <v-flex
      md12
      xs6
      order-md1
      order-xs2
      offset-xs0
      offset-md2
      pl-2
      pr-2
      class="walletSummaryAction mt-2">
      <send-tokens-modal
        ref="sendTokensModal"
        :wallet="wallet"
        :is-read-only="isReadOnly"
        :contract-details="contractDetails"
        @sent="$emit('transaction-sent', $event)"
        @error="$emit('error', $event)" />
    </v-flex>
    <v-flex
      md12
      xs6
      order-md2
      order-xs1
      offset-xs0
      offset-md2
      pl-2
      pr-2
      class="walletSummaryAction mt-2">
      <request-funds-modal
        ref="walletRequestFundsModal"
        :wallet-address="walletAddress"
        :contract-details="contractDetails" />
    </v-flex>
  </v-layout>
</template>

<script>
export default {
  props: {
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Array,
      default: function() {
        return [];
      },
    },
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address;
    }
  },
  methods: {
    init(isReadOnly) {
      if (document.location.search && document.location.search.length) {
        const search = document.location.search.substring(1);
        const parameters = JSON.parse(
          `{"${decodeURI(search)
            .replace(/"/g, '\\"')
            .replace(/&/g, '","')
            .replace(/=/g, '":"')}"}`
        );
        if (parameters && parameters.receiver && parameters.receiver_type) {
          if (this.isReadOnly || isReadOnly) {
            throw new Error(this.$t('exoplatform.wallet.warning.walletReadonly'));
          }
          this.$refs.sendTokensModal.prepareSendForm(parameters.receiver, parameters.receiver_type, parameters.amount, parameters.id);
        }
      }
    },
  },
};
</script>
