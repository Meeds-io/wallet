<template>
  <v-layout
    wrap
    xs12
    class="mt-4 mb-6">
    <v-flex
      class="headline my-auto text-center text-md-right"
      md4
      xs12>
      {{ $t('exoplatform.wallet.title.adminWallet') }}
    </v-flex>

    <v-flex
      class="my-auto"
      md2
      xs6>
      <div class="text-center no-wrap">{{ $t('exoplatform.wallet.label.tokenBalance', {0: tokenName}) }}</div>
      <div class="text-center no-wrap">
        <v-progress-circular
          v-if="loadingBalances"
          color="primary"
          class="mr-4"
          indeterminate
          size="20" />
        <template v-else>
          {{ tokenBalanceLabel }}
        </template>
      </div>
    </v-flex>

    <v-flex
      class="my-auto"
      md2
      xs6>
      <div class="ma-auto no-wrap">{{ $t('exoplatform.wallet.label.etherBalance') }}</div>
      <div class="ma-auto no-wrap">
        <v-progress-circular
          v-if="loadingBalances"
          color="primary"
          class="mr-4"
          indeterminate
          size="20" />
        <template v-else>
          {{ etherBalanceLabel }}
        </template>
      </div>
    </v-flex>

    <v-flex
      class="my-auto text-center text-md-left no-wrap"
      md4
      xs12>
      <v-btn
        icon
        text
        class="mr-4"
        @click="$emit('refresh-balance')">
        <v-icon color="grey">refresh</v-icon>
      </v-btn>
      <a
        :href="requestFundsLink"
        target="_blank"
        rel="noopener noreferrer"
        class="no-wrap">
        {{ $t('exoplatform.wallet.button.requestFunds') }}
      </a>
    </v-flex>
  </v-layout>
</template>

<script>

export default {
  props: {
    contractDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
    adminWallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  computed: {
    requestFundsLink() {
      return (this.adminWalletAddress && `https://www.exoplatform.com/request-rewards-funds?address=${this.adminWalletAddress}`) || '#';
    },
    adminWalletAddress() {
      return (this.adminWallet && this.adminWallet.address) || '';
    },
    tokenName() {
      return (this.contractDetails && this.contractDetails.name) || '';
    },
    tokenSymbol() {
      return (this.contractDetails && this.contractDetails.symbol) || '';
    },
    loadingBalances() {
      return (this.adminWallet && this.adminWallet.loading) || false;
    },
    tokenBalance() {
      return (this.adminWallet && this.adminWallet.tokenBalance) || 0;
    },
    tokenBalanceLabel() {
      return `${this.walletUtils.toFixed(this.tokenBalance)} ${this.tokenSymbol}`;
    },
    etherBalance() {
      return (this.adminWallet && this.adminWallet.etherBalance) || 0;
    },
    etherBalanceLabel() {
      return `${this.walletUtils.toFixed(this.etherBalance)} eth`;
    },
  },
}

</script>