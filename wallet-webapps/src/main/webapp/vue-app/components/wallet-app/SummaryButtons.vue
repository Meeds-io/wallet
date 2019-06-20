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
        v-if="!isSpace || isSpaceAdministrator"
        ref="sendTokensModal"
        :account="walletAddress"
        :contract-details="principalAccountDetails"
        @sent="$emit('transaction-sent')"
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
        v-if="!isSpace || isSpaceAdministrator"
        ref="walletRequestFundsModal"
        :wallet-address="walletAddress"
        :contract-details="principalAccountDetails" />
    </v-flex>
  </v-layout>
</template>

<script>
export default {
  props: {
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
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
    isMaximized: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isSpaceAdministrator: {
      type: Boolean,
      default: function() {
        return false;
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
    etherBalance: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      updatePendingTransactionsIndex: 1,
      walletInitializationStatus: null,
      pendingTransactions: {},
      lastTransaction: null,
    };
  },
  computed: {
    disableSendButton() {
      return this.isReadOnly || !this.etherBalance || !Number(this.etherBalance);
    },
    pendingTransactionsCount() {
      return this.updatePendingTransactionsIndex && Object.keys(this.pendingTransactions).length;
    },
    principalAccountDetails() {
      if (this.principalAccount && this.accountsDetails[this.principalAccount]) {
        return this.accountsDetails[this.principalAccount];
      }
      return null;
    },
    overviewAccountsArray() {
      if(!this.overviewAccounts) {
        return [];
      }
      const accountsList = [];
      this.overviewAccounts.forEach((selectedValue) => {
        if (selectedValue !== this.principalAccount) {
          if (selectedValue === 'fiat') {
            const accountDetails = Object.assign({}, this.accountsDetails[this.walletAddress]);
            accountDetails.key = 'fiat';
            accountsList.push(accountDetails);
          } else if (selectedValue === 'ether') {
            const accountDetails = Object.assign({}, this.accountsDetails[this.walletAddress]);
            accountDetails.key = 'ether';
            accountsList.push(accountDetails);
          } else if (this.accountsDetails[selectedValue]) {
            accountsList.push(this.accountsDetails[selectedValue]);
          }
        }
      });

      return accountsList;
    },
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
          if (isReadOnly) {
            throw new Error('Your wallet is in readonly state');
          }
          this.$refs.sendTokensModal.prepareSendForm(parameters.receiver, parameters.receiver_type, parameters.amount, parameters.id);
        }
      }
    },
  },
};
</script>
