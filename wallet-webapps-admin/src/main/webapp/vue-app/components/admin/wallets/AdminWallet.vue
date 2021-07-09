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
  <v-layout
    wrap
    class="mt-8 adminWallet">
    <div class="mx-4">
      <v-btn
        outlined
        :href="requestFundsLink"
        target="_blank">
        {{ $t('exoplatform.wallet.button.requestFunds') }}
      </v-btn>
    </div>
    <div class="text-center no-wrap mx-4 title">
      <span class="labels"> {{ tokenName }}: </span>
      <v-progress-circular
        v-if="loadingBalances"
        color="primary"
        class="me-4"
        indeterminate
        size="20" />
      <template v-else>
        <span class="symbol"> {{ tokenSymbol }} </span> <span class="fundsLabels"> {{ tokenBalance }} </span>
        <v-icon
          v-if="adminBalanceTooLow"
          color="orange"
          :title="$t('exoplatform.wallet.label.adminBalanceTooLow')">
          warning
        </v-icon>
      </template>
    </div>
    <v-divider
      class="mx-4"
      width="100"
      vertical />
    <div class="no-wrap mx-4 title">
      <span class="labels"> {{ $t('exoplatform.wallet.label.ethers') }}: </span>
      <v-progress-circular
        v-if="loadingBalances"
        color="primary"
        class="me-4"
        indeterminate
        size="20" />
      <template v-else>
        <span class="fundsLabels"> {{ etherBalanceLabel }} </span>
      </template>
    </div>
    <v-dialog
      v-model="dialog"
      width="500">
      <template v-slot:activator="{ on, attrs }">
        <a
          target="_blank"
          v-bind="attrs"
          v-on="on"
          class="no-wrap mx-5 title">
          <span class="ethereumAddress"> {{ $t('exoplatform.wallet.label.ethereumAddress') }}</span></a>
      </template>
      <v-card>
        <v-card-title class="ethereumAddressTitle grey lighten-2">
          {{ $t('exoplatform.wallet.label.ethereumAddress') }}
        </v-card-title>
        <v-card-text class="mt-4">
          {{ adminWalletAddress }}
        </v-card-text>
      </v-card>
    </v-dialog>
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
    initialTokenAmount: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    adminWallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  data () {
    return {
      dialog: false,
    };
  },
  computed: {
    adminBalance() {
      return (this.adminWallet && this.walletUtils.toFixed(this.adminWallet.tokenBalance)) || 0;
    },
    adminBalanceTooLow() {
      return this.adminBalance < this.initialTokenAmount;
    },
    requestFundsLink() {
      return (this.adminWalletAddress && `https://www.exoplatform.com/rewarding-program?address=${this.adminWalletAddress}`) || '#';
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
    etherBalance() {
      return (this.adminWallet && this.adminWallet.etherBalance) || 0;
    },
    etherBalanceLabel() {
      return `${this.walletUtils.toFixed(this.etherBalance)} E`;
    },
  },
};

</script>