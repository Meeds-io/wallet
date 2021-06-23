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
  <v-card class="walletSummaryBalance headline elevation-3  pa-1 mt-2">
    <v-card-title class="title subtitle-1 text-truncate pb-1">
      {{ $t('exoplatform.wallet.label.lastTransaction') }}
    </v-card-title>
    <v-card-title class="lastTransactionBalance headline">
      <template v-if="loadingTransaction">
        <v-progress-circular
          color="primary"
          class="mb-2"
          indeterminate />
      </template>
      <v-container
        v-else
        fluid
        grid-list-sm>
        <v-layout row>
          <v-flex grow class="amount">
            <span class="symbol"> {{ contractDetails.symbol }} </span> {{ lastTransaction && walletUtils.toFixed(lastTransaction.contractAmount) }}
          </v-flex>
        </v-layout>
      </v-container>
    </v-card-title>
  </v-card>
</template>

<script>
export default {
  props: {
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
    pendingTransactionsCount: {
      type: Number,
      default: function() {
        return 0;
      },
    },
  },
  data() {
    return {
      loadingTransaction: false,
      lastTransaction: null,
      lastTransactionSent: null,
    };
  },
  computed: {
    lastTransactionSign() {
      return (this.lastTransaction && this.lastTransaction.contractAmount && ((this.lastTransactionSent && '-') || '+')) || '';
    }
  },
  watch: {
    pendingTransactionsCount(newValue, oldValue) {
      if (newValue !== oldValue) {
        this.refreshLastTransaction();
      }
    },
    contractDetails() {
      this.refreshLastTransaction();
    },
    lastTransaction() {
      if (this.lastTransaction && this.lastTransaction.pending) {
        const thiss = this;
        this.walletUtils.watchTransactionStatus(this.lastTransaction.hash, (transactionDetails) => {
          if (transactionDetails && thiss.lastTransaction && thiss.lastTransaction.hash === transactionDetails.hash) {
            Object.assign(thiss.lastTransaction, transactionDetails);
            thiss.$forceUpdate();
          }
        });
      }
    }
  },
  created() {
    this.refreshLastTransaction();
  },
  methods: {
    displayTransactionList() {
      this.$emit('display-transactions');
    },
    refreshLastTransaction() {
      if (!this.contractDetails || !this.contractDetails.address || !this.walletAddress) {
        return;
      }
      this.loadingTransaction = true;
      this.transactionUtils.getStoredTransactions(this.walletAddress, this.contractDetails.address, 10)
        .then(transactions => {
          this.lastTransaction = transactions && transactions.length && transactions[0];
          this.lastTransactionSent = (this.lastTransaction && this.lastTransaction.contractAmount && this.lastTransaction.from && (this.lastTransaction.from.toLowerCase() === this.walletAddress.toLowerCase()));
        })
        .catch(e => {
          this.$emit('error', e);
        })
        .finally(() => this.loadingTransaction = false);
    }
  }
};
</script>
