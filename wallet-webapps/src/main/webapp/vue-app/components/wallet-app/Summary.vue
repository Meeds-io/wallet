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
  <v-flex id="walletSummary" class="elevation-0 me-3">
    <template v-if="!isSpace || isSpaceAdministrator">
      <v-card-title
        v-if="initializationState === 'NEW' || initializationState === 'MODIFIED' || initializationState === 'PENDING'"
        primary-title
        class="pb-0">
        <v-spacer />
        <div class="alert alert-info">
          <i class="uiIconInfo"></i>
          {{ $t('exoplatform.wallet.info.pendingInitialization') }}
        </div>
        <v-spacer />
      </v-card-title>
      <v-card-title
        v-else-if="initializationState === 'DENIED'"
        primary-title
        class="pb-0">
        <v-spacer />
        <div class="alert alert-info ignore-vuetify-classes">
          <i class="uiIconInfo"></i>
          {{ $t('exoplatform.wallet.info.initializationAccessDenied') }}
          <button class="ignore-vuetify-classes btn" @click="requestAccessAuthorization()">
            {{ $t('exoplatform.wallet.button.requestAuthorization') }}
          </button>
        </div>
        <v-spacer />
      </v-card-title>
    </template>

    <template v-if="initializationState !== 'DENIED'">
      <v-card-title
        v-if="pendingTransactionsCount"
        primary-title
        class="pb-0">
        <v-spacer />
        <v-badge
          :title="$t('exoplatform.wallet.message.transactionInProgress')"
          color="red"
          :right="!$vuetify.rtl">
          <span slot="badge">
            {{ pendingTransactionsCount }}
          </span>
          <v-progress-circular
            color="primary"
            indeterminate
            size="20" />
        </v-badge>
        <v-spacer />
      </v-card-title>
  
      <v-container
        v-if="contractDetails"
        fluid
        grid-list-md
        ps-3
        pe-0>
        <v-layout
          col
          wrap
          class="px-0">
          <v-flex
            md4
            xs12
            text-center>
            <summary-balance :wallet="wallet" :contract-details="contractDetails" />
          </v-flex>
          <v-flex
            v-if="!isSpace"
            offset-md1
            offset-xs0
            md3
            xs6
            pe-0
            ps-0
            text-center>
            <summary-reward
              :wallet="wallet"
              :contract-details="contractDetails"
              @display-transactions="$emit('display-transactions', 'reward')"
              @error="$emit('error', $event)" />
          </v-flex>
          <v-flex
            :class="isSpace ? 'offset-md5 offset-xs0 md3 xs12': 'offset-md1 offset-xs0 md3 xs6'"
            pe-0
            ps-0
            text-center>
            <summary-transaction
              :contract-details="contractDetails"
              :wallet-address="walletAddress"
              :pending-transactions-count="pendingTransactionsCount"
              @display-transactions="$emit('display-transactions')"
              @error="$emit('error', $event)" />
          </v-flex>
        </v-layout>
      </v-container>
    </template>
  </v-flex>
</template>

<script>
import SummaryBalance from './SummaryBalance.vue';
import SummaryReward from './SummaryReward.vue';
import SummaryTransaction from './SummaryTransaction.vue';

export default {
  components: {
    SummaryBalance,
    SummaryReward,
    SummaryTransaction,
  },
  props: {
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    isSpaceAdministrator: {
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
    initializationState: {
      type: String,
      default: function() {
        return null;
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
      updatePendingTransactionsIndex: 1,
      pendingTransactions: {},
      lastTransaction: null,
    };
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    pendingTransactionsCount() {
      return this.updatePendingTransactionsIndex && Object.keys(this.pendingTransactions).length;
    },
  },
  methods: {
    requestAccessAuthorization() {
      return fetch(`/portal/rest/wallet/api/account/requestAuthorization?address=${this.walletAddress}`, {
        credentials: 'include',
      }).then((resp) => {
        if (!resp || !resp.ok) {
          throw new Error(this.$t('exoplatform.wallet.error.errorRequestingAuthorization'));
        }
        this.$emit('refresh');
      })
        .catch(e => {
          this.$emit('error', String(e));
        });
    },
    loadPendingTransactions() {
      Object.keys(this.pendingTransactions).forEach((key) => delete this.pendingTransactions[key]);

      return this.transactionUtils.loadTransactions(
        this.walletAddress,
        null,
        this.pendingTransactions,
        true,
        100,
      ).then((transactions) => {
        // Refresh counting pending transactions
        this.updatePendingTransactionsIndex++;

        transactions.forEach(transaction => {
          this.walletUtils.watchTransactionStatus(transaction.hash, (transaction) => {
            if (this.pendingTransactions[transaction.hash]) {
              delete this.pendingTransactions[transaction.hash];
            }
            this.updatePendingTransactionsIndex++;
          });
        });
      });
    },
  },
};
</script>
