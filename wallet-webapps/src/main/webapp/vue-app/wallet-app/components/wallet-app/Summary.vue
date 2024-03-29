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
    <template>
      <v-container
        fluid
        ps-3
        pe-0
        class="mt-3 px-1 py-1">
        <v-layout
          wrap
          color="transparent"
          class="WalletSummary align-center">
          <v-flex class="summaryCard mr-3" v-if="!walletReadonly">
            <div class="border-box-sizing" v-if="walletAddress && contractDetails && this.initializationState !== 'DELETED'">
              <button
                class="btn ignore-vuetify-classes me-1"
                @click="openExchangeDrawer">
                {{ $t('exoplatform.wallet.label.exchanges') }}
              </button>
            </div>
            <wallet-reward-summary-buttons
              ref="walletSummaryActions"
              v-if="walletAddress && contractDetails && this.initializationState !== 'DELETED'"
              :is-space="isSpace"
              :is-space-administrator="isSpaceAdministrator"
              :contract-details="contractDetails"
              :wallet="wallet"
              :is-read-only="isReadOnly"
              @display-transactions="openAccountDetail"
              @transaction-sent="newPendingTransaction"
              @error="error = $event" />
          </v-flex>
          <v-flex class="summaryCard" v-if="!walletReadonly">
            <wallet-reward-summary-transaction
              v-if="walletAddress && contractDetails && this.initializationState !== 'DELETED'"
              :contract-details="contractDetails"
              :wallet-address="walletAddress"
              :fiat-symbol="fiatSymbol"
              :wallet="wallet"
              :selected-transaction-hash="selectedTransactionHash"
              :selected-contract-method-name="selectedContractMethodName"
              :pending-transactions-count="pendingTransactionsCount"
              @display-transactions="$emit('display-transactions')"
              @error="$emit('error', $event)" />
          </v-flex>
          <v-flex class="summaryBalance">
            <wallet-reward-summary-balance
              class="mt-1 mx-3 px-1 py-1"
              v-if="walletAddress && !loading && contractDetails && this.initializationState !== 'DELETED'"
              :wallet="wallet"
              :contract-details="contractDetails"
              @refresh="$emit('refresh')" />
          </v-flex>
          <v-flex class="fixedItems no-wrap" v-if="!walletReadonly">
            <div
              v-if="pendingTransactionsCount"
              primary-title
              class="pb-0 mx-4 mt-2">
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
            </div>
            <div
              v-if="displayWarnings"
              id="etherTooLowWarningParent"
              class="ms-2 mt-2">
              <v-icon :title="$t('exoplatform.wallet.warning.noEnoughFunds')" color="orange">
                warning
              </v-icon>
            </div>
            <wallet-reward-toolbar-menu
              ref="walletAppMenu"
              :is-space="isSpace"
              :is-space-administrator="isSpaceAdministrator"
              :wallet="wallet"
              @refresh="init()" />
          </v-flex>
        </v-layout>
      </v-container>
    </template>
  </v-flex>
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
    displayWarnings: {
      type: Boolean,
      default: false,
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return '$';
      },
    },
    selectedTransactionHash: {
      type: String,
      default: function() {
        return null;
      },
    },
    selectedContractMethodName: {
      type: String,
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
      walletRewards: [],
    };
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    pendingTransactionsCount() {
      return this.updatePendingTransactionsIndex && Object.keys(this.pendingTransactions).length;
    },
    walletReadonly() {
      return this.isSpace && !this.isSpaceAdministrator;
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
        this.contractDetails,
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
    openExchangeDrawer() {
      this.$refs.walletSummaryActions.open();
    },
    prepareSendForm() {
      this.$refs.walletSummaryActions?.init();
    },
  },
};
</script>
