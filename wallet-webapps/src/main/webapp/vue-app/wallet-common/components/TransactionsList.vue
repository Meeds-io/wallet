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
  <v-flex class="transactionsList text-start">
    <v-card class="card--flex-toolbar" flat>
      <div v-if="error && !loading" class="alert alert-error">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <div v-if="loading" class="grey--text">
        {{ $t('exoplatform.wallet.message.loadingRecentTransactions') }}...
      </div>
      <v-progress-linear
        v-if="loading"
        indeterminate
        color="primary"
        class="mb-0 mt-0" />

      <v-divider />
      <v-expansion-panels
        v-if="Object.keys(sortedTransactions).length"
        accordion
        focusable>
        <v-expansion-panel
          v-for="(item, index) in sortedTransactions"
          :id="`transaction-${item.hash}`"
          :key="index"
          :value="item.selected">
          <v-expansion-panel-header class="border-box-sizing px-0 py-0">
            <v-list
              :class="item.selected && 'blue lighten-5'"
              two-line
              ripple
              class="px-2 py-0 transactions">
              <v-list-item
                :key="item.hash"
                class="transactionDetailItem autoHeight"
                ripple>
                <v-progress-circular
                  v-if="item.pending"
                  indeterminate
                  color="primary"
                  class="me-4" />
                <v-list-item-avatar v-else>
                  <v-icon :color="item.succeeded ? 'primary' : 'red'">
                    {{ item.adminIcon ? 'fa-cog' : item.isReceiver ? 'fa-arrow-down' : 'fa-arrow-up' }}
                  </v-icon>
                </v-list-item-avatar>
  
                <v-list-item-content class="transactionDetailContent">
                  <v-list-item-title v-if="item.isContractCreation">
                    <span>
                      {{ $t('exoplatform.wallet.label.createdContract') }}
                    </span>
                    <wallet-reward-wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.type === 'ether'" class="text-truncated">
                    <wallet-reward-profile-chip
                      v-if="displayFullTransaction && item.isReceiver"
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <wallet-reward-profile-chip
                      v-else-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
  
                    <span v-if="item.isReceiver">
                      {{ (item.pending || item.succeeded) ? $t('exoplatform.wallet.label.receivedFrom') : $t('exoplatform.wallet.label.errorReceivedFrom') }}
                    </span>
                    <span v-else>
                      {{ (item.pending || item.succeeded) ? $t('exoplatform.wallet.label.sentTo') : $t('exoplatform.wallet.label.errorSentTo') }}
                    </span>
  
                    <wallet-reward-profile-chip
                      v-if="item.isReceiver"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
  
                    <wallet-reward-profile-chip
                      v-else
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'transferFrom'">
                    <span>
                      {{ $t('exoplatform.wallet.label.sentTo') }}
                    </span>
                    <wallet-reward-profile-chip
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.by') }}
                    </span>
                    <wallet-reward-profile-chip
                      :address="item.byAddress"
                      :profile-id="item.byUsername"
                      :profile-technical-id="item.byTechnicalId"
                      :space-id="item.bySpaceId"
                      :profile-type="item.byType"
                      :display-name="item.byDisplayName"
                      :avatar="item.byAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.onBehalfOf') }}
                    </span>
                    <wallet-reward-profile-chip
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="(item.contractMethodName === 'transfer' || item.contractMethodName === 'approve')">
                    <wallet-reward-profile-chip
                      v-if="displayFullTransaction && item.isReceiver"
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <wallet-reward-profile-chip
                      v-else-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />

                    <span v-if="item.contractMethodName === 'transfer' && item.isReceiver">
                      {{ $t('exoplatform.wallet.label.receivedFrom') }}
                    </span>
                    <span v-else-if="item.contractMethodName === 'transfer' && !item.isReceiver">
                      {{ $t('exoplatform.wallet.label.sentTo') }}
                    </span>
                    <span v-else>
                      {{ $t('exoplatform.wallet.label.delegatedTo') }}
                    </span>
  
                    <wallet-reward-profile-chip
                      v-if="item.isReceiver"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <wallet-reward-profile-chip
                      v-else
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.value && Number(item.value) && item.amountFiat">
                    <wallet-reward-profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.sentEtherToContract') }}
                    </span>
                  </v-list-item-title>
  
                  <v-list-item-title v-else>
                    <span>
                      {{ $t('exoplatform.wallet.label.contractTransaction') }}
                    </span>
                    <wallet-reward-wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
  
                  <v-list-item-subtitle>
                    <v-icon
                      v-if="!item.pending && !item.succeeded"
                      :title="$t('exoplatform.wallet.label.transactionFailed')"
                      color="orange">
                      warning
                    </v-icon>
                    <v-list-item-action-text v-if="item.dateFormatted">
                      {{ item.dateFormatted }}
                    </v-list-item-action-text>
                  </v-list-item-subtitle>
                </v-list-item-content>
  
                <v-list-item-content v-if="item.type === 'ether' && item.value && Number(item.value)" class="transactionDetailActions">
                  <v-list-item-title :class="item.adminIcon ? '' : (item.pending || item.succeeded) ? 'primary--text' : 'red--text'">
                    <span>
                      {{ toFixed(item.value) }} {{ $t('wallet.mainCurrency') }}
                    </span>
                  </v-list-item-title>
                  <v-list-item-subtitle v-if="item.amountFiat">
                    <v-list-item-action-text>
                      {{ toFixed(item.amountFiat) }} {{ fiatSymbol }}
                    </v-list-item-action-text>
                  </v-list-item-subtitle>
                </v-list-item-content>
  
                <v-list-item-content v-else class="transactionDetailActions">
                  <v-list-item-title
                    v-if="item.contractAmount"
                    :class="(item.pending || item.succeeded) ? 'primary--text' : 'red--text'">
                    <span>
                      {{ toFixed(item.contractAmount) }} {{ item.contractSymbol }}
                    </span>
                  </v-list-item-title>
                  <v-list-item-title
                    v-else-if="item.value && Number(item.value)"
                    :class="(item.pending || item.succeeded) ? 'primary--text' : 'red--text'">
                    <span>
                      {{ toFixed(item.value) }} {{ $t('wallet.mainCurrency') }}
                    </span>
                  </v-list-item-title>
                  <v-list-item-subtitle
                    v-if="item.contractMethodName === 'reward'">
                    <v-list-item-action-text>
                      {{ toFixed(item.value) }} {{ item.contractSymbol }}
                    </v-list-item-action-text>
                  </v-list-item-subtitle>
                  <v-list-item-subtitle
                    v-else-if="item.amountFiat">
                    <v-list-item-action-text>
                      {{ toFixed(item.amountFiat) }} {{ fiatSymbol }}
                    </v-list-item-action-text>
                  </v-list-item-subtitle>
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-list class="px-0" dense>
              <v-list-item v-if="!item.pending && !item.succeeded">
                <v-list-item-content>
                  <div class="alert alert-warning ignore-vuetify-classes">
                    <i class="uiIconWarning"></i>
                    {{ item.adminIcon ? $t('exoplatform.wallet.label.transactionFailed') : item.isReceiver ? $t('exoplatform.wallet.label.transactionReceptionFailed', {0: contractName}) : $t('exoplatform.wallet.label.transactionSendingFailed', {0: contractName}) }}
                  </div>
                </v-list-item-content>
              </v-list-item>

              <v-list-item v-if="item.pending && item.contractAddress && item.sentTimestamp && !item.boost">
                <v-list-item-content>
                  <wallet-reward-send-tokens-modal
                    ref="boostTransactionModal"
                    :wallet="wallet"
                    :transaction="item"
                    :contract-details="contractDetails"
                    @sent="transactionSent" />
                </v-list-item-content>
              </v-list-item>

              <v-list-item v-if="administration && !item.refreshed && item.pending && !item.succeeded">
                <v-list-item-content>
                  <v-btn
                    class="btn ignore-vuetify-classes"
                    :loading="item.refreshing"
                    @click="refreshFromBlockchain(item)">
                    {{ $t('exoplatform.wallet.button.refresh') }}
                  </v-btn>
                </v-list-item-content>
              </v-list-item>

              <v-list-item v-if="!item.succeeded && !item.pending && !item.resent">
                <v-list-item-content>
                  <v-btn
                    class="btn ignore-vuetify-classes"
                    :loading="item.resending"
                    @click="resendToBlockchain(item)">
                    {{ $t('exoplatform.wallet.button.resend') }}
                  </v-btn>
                </v-list-item-content>
              </v-list-item>

              <v-list-item v-if="administration && item.issuer">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.issuer') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-end">
                  <wallet-reward-profile-chip
                    :address="item.issuer.address"
                    :profile-id="item.issuer.id"
                    :profile-technical-id="item.issuer.technicalId"
                    :space-id="item.issuer.spaceId"
                    :profile-type="item.issuer.type"
                    :display-name="item.issuer.name"
                    :avatar="item.issuer.avatar" />
                </v-list-item-content>
              </v-list-item>

              <v-list-item v-if="item.label" class="dynamic-height">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.transactionLabel') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-end paragraph text-truncate">
                  {{ item.label }}
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.message" class="dynamic-height">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.transactionMessage') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-start paragraph">
                  <span v-sanitized-html="item.message"></span>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="Number(item.contractAmount)">
                <v-list-item-content>
                  {{ item.contractAmountLabel }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-end">
                  <div class="no-wrap">
                    {{ toFixed(item.contractAmount) }} {{ item.contractSymbol }}
                  </div>
                </v-list-item-content>
              </v-list-item>
              <template v-else>
                <v-list-item v-if="Number(item.amountFiat)">
                  <v-list-item-content>
                    {{ $t('exoplatform.wallet.label.fiatAmount') }}
                  </v-list-item-content>
                  <v-list-item-content class="align-end text-end">
                    <div class="no-wrap">
                      {{ toFixed(item.amountFiat) }} {{ fiatSymbol }}
                    </div>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item v-if="Number(item.value)">
                  <v-list-item-content>
                    {{ $t('exoplatform.wallet.label.etherAmount') }}
                  </v-list-item-content>
                  <v-list-item-content class="align-end text-end">
                    <div class="no-wrap">
                      {{ toFixed(item.value) }} {{ $t('wallet.mainCurrency') }}
                    </div>
                  </v-list-item-content>
                </v-list-item>
              </template>
  
              <v-list-item v-if="item.fromAddress">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.fromAddress') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-end text-truncate">
                  <a
                    v-if="addressEtherscanLink"
                    :href="`${addressEtherscanLink}${item.fromAddress}`"
                    :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                    target="_blank">
                    {{ item.fromAddress }}
                  </a>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.toAddress">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.toAddress') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-end text-truncate">
                  <a
                    v-if="addressEtherscanLink"
                    :href="`${addressEtherscanLink}${item.toAddress}`"
                    :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                    target="_blank">
                    {{ item.toAddress }}
                  </a>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.contractName">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.contractName') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-end">
                  <div class="no-wrap">
                    {{ item.contractName }}
                  </div>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.contractAddress">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.contractAddress') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-end text-truncate">
                  <a
                    v-if="tokenEtherscanLink"
                    :href="`${tokenEtherscanLink}${item.contractAddress}`"
                    :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                    target="_blank">
                    {{ item.contractAddress }}
                  </a>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.fee">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.transactionFee') }}
                </v-list-item-content>
                <v-list-item-content v-if="item.tokenFee" class="align-end text-end">
                  <div class="no-wrap">
                    {{ toFixed(item.tokenFee) }} {{ item.contractSymbol }}
                  </div>
                </v-list-item-content>
                <v-list-item-content v-else class="align-end text-end">
                  <div class="no-wrap">
                    {{ toFixed(item.feeFiat) }} {{ fiatSymbol }}
                    <v-icon
                      v-if="item.noContractFunds"
                      color="orange">
                      warning
                    </v-icon>
                  </div>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item>
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.transactionHash') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-end text-truncate">
                  <a
                    v-if="transactionEtherscanLink"
                    :href="`${transactionEtherscanLink}${item.hash}`"
                    :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                    target="_blank">
                    {{ item.hash }}
                  </a>
                </v-list-item-content>
              </v-list-item>
              <template v-if="administration">
                <v-list-item v-if="item.nonce">
                  <v-list-item-content>
                    Nonce
                  </v-list-item-content>
                  <v-list-item-content class="align-end text-end">
                    <strong>{{ item.nonce }}</strong>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item v-if="item.sentDateFormatted">
                  <v-list-item-content>
                    {{ $t('exoplatform.wallet.label.sentDate') }}
                  </v-list-item-content>
                  <v-list-item-content class="align-end text-end">
                    <strong>{{ item.sentDateFormatted }}</strong>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item v-if="item.sendingAttemptCount">
                  <v-list-item-content>
                    {{ $t('exoplatform.wallet.label.sendingAttemptCount') }}
                  </v-list-item-content>
                  <v-list-item-content class="align-end text-end">
                    <strong>{{ item.sendingAttemptCount === -200 ? 1 : item.sendingAttemptCount }}</strong>
                  </v-list-item-content>
                </v-list-item>
              </template>
            </v-list>
          </v-expansion-panel-content>
        </v-expansion-panel>
        <div v-if="!limitReached" class="loadMoreTransaction">
          <v-btn
            :loading="loading"
            color="primary"
            block
            class="mt-3"
            text
            @click="transactionsLimit += transactionsPerPage">
            {{ $t('exoplatform.wallet.button.loadMore') }}
          </v-btn>
        </div>
      </v-expansion-panels>
      <v-flex v-else-if="!loading" class="text-center">
        <span>
          {{ $t('exoplatform.wallet.label.noRecentTransactions') }}
        </span>
      </v-flex>
    </v-card>
  </v-flex>
</template>

<script>
import {watchTransactionStatus, getTransactionEtherscanlink, getAddressEtherscanlink, getTokenEtherscanlink, toFixed} from '../js/WalletUtils.js';
import {loadTransactions, refreshTransactionDetail, saveTransactionDetails} from '../js/TransactionUtils.js';

export default {
  props: {
    wallet: {
      type: Object,
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
    error: {
      type: String,
      default: function() {
        return null;
      },
    },
    displayFullTransaction: {
      type: Boolean,
      default: function() {
        return false;
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
    administration: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      // A trick to force update computed list
      // since the attribute this.transactions is modified outside the component
      refreshIndex: 1,
      currentUser: eXo.env.portal.userName,
      loading: false,
      settings: null,
      transactionsLimit: 10,
      transactionsPerPage: 10,
      limitReached: false,
      transactions: {},
    };
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    isSpaceWallet() {
      return this.wallet && this.wallet.type === 'space';
    },
    contractName() {
      return (this.contractDetails && this.contractDetails.name) || (this.settings && this.settings.contractDetail && this.settings.contractDetail.name);
    },
    sortedTransactions() {
      // A trick to force update computed list
      // since the attribute this.transactions is modified outside the component
      if (!this.refreshIndex) {
        return {};
      }
      const transactions = this.transactions;
      const sortedTransactions = {};
      Object.values(transactions)
        .filter((transaction) => transaction && !transaction.ignore)
        .sort((transaction1, transaction2) => (transaction2.timestamp || 0) - (transaction1.timestamp || 0))
        .forEach((transaction) => {
          sortedTransactions[transaction.hash] = transaction;
        });
      return sortedTransactions;
    },
  },
  watch: {
    transactionsLimit() {
      if (this.transactionsLimit !== this.transactionsPerPage && !this.loading) {
        this.init(true).catch((error) => {
          this.loading = false;
          this.$emit('error', `Account loading error: ${error}`);
        });
      }
    },
    contractDetails() {
      if (this.contractDetails) {
        this.limitReached = false;
        this.transactions = {};
        this.transactionsLimit = this.transactionsPerPage;
        this.init().catch((error) => {
          console.error('account field change event - error', error);
          this.loading = false;
          this.$emit('error', `${this.$t('exoplatform.wallet.error.accountLoadingError')}: ${error}`);
        });
      }
    },
  },
  created() {
    this.transactionEtherscanLink = getTransactionEtherscanlink();
    this.addressEtherscanLink = getAddressEtherscanlink();
    this.tokenEtherscanLink = getTokenEtherscanlink();

    this.init().catch((error) => {
      console.error('init method - error', error);
      this.loading = false;
      this.$emit('error', `${this.$t('exoplatform.wallet.error.accountInitializationError')}: ${error}`);
    });
  },
  methods: {
    transactionSent() {
      this.$emit('refresh');
      this.init();
    },
    init(ignoreSelected) {
      this.loading = true;
      this.error = null;
      this.settings = window.walletSettings;

      // Get transactions to latest block with maxBlocks to load
      return this.loadRecentTransaction(this.transactionsLimit)
        .then(() => {
          if (!ignoreSelected && this.selectedTransactionHash) {
            const selectedTransaction = this.transactions[this.selectedTransactionHash] || this.transactions[this.selectedTransactionHash.toLowerCase()];
            if (selectedTransaction) {
              this.$set(selectedTransaction, 'selected', true);
              this.$nextTick(() => {
                setTimeout(() => {
                  const selectedTransactionElement = document.getElementById(`transaction-${selectedTransaction.hash}`);
                  if (selectedTransactionElement) {
                    selectedTransactionElement.scrollIntoView();
                  }
                }, 200);
              });
            }
          }
          this.loading = false;
          this.forceUpdateList();
        })
        .catch((e) => {
          console.error('loadRecentTransaction - method error', e);

          this.loading = false;
          this.$emit('error', `${e}`);
        })
        .finally(() => {
          this.$emit('loaded', this.sortedTransactions, this.transactions ? Object.keys(this.transactions).length : 0);
        });
    },
    loadRecentTransaction(limit) {
      const filterObject = {
        hash: this.selectedTransactionHash,
        contractMethodName: this.selectedContractMethodName,
      };
      return loadTransactions(this.walletAddress, this.contractDetails, this.transactions, false, limit, filterObject, this.administration)
        .then(() => {
          const totalTransactionsCount = Object.keys(this.transactions).length;
          this.limitReached = (totalTransactionsCount <= this.transactionsLimit && (totalTransactionsCount % this.transactionsPerPage) > 0) || (totalTransactionsCount < this.transactionsLimit);

          Object.values(this.transactions).filter(tx => tx.pending).forEach(transaction => {
            watchTransactionStatus(transaction.hash, (transactionDetails) => {
              Object.assign(transaction, transactionDetails);
              this.forceUpdateList();
            });
          });
        })
        .catch((e) => {
          console.error('loadTransactions - method error', e);
          this.$emit('error', `${e}`);
        });
    },
    refreshFromBlockchain(transactionDetail) {
      if (!transactionDetail || !transactionDetail.hash) {
        return;
      }
      this.$set(transactionDetail, 'refreshing', true);
      this.$forceUpdate();

      refreshTransactionDetail(transactionDetail.hash)
        .then(refreshedTransactionDetail => {
          if (refreshedTransactionDetail) {
            Object.assign(transactionDetail, refreshedTransactionDetail);
            this.$set(transactionDetail, 'refreshed', true);
          }
        })
        .finally(() => {
          this.$set(transactionDetail, 'refreshing', false);
          this.$forceUpdate();
        });
    },
    resendToBlockchain(transactionDetail) {
      if (!transactionDetail || !transactionDetail.hash) {
        return;
      }
      this.$set(transactionDetail, 'resending', true);
      this.$forceUpdate();
      this.error = null;
      this.warning = null;
      transactionDetail.sendingAttemptCount=0;
      transactionDetail.id=0;
      transactionDetail.pending=true;
      transactionDetail.sentTimestamp=0;
      transactionDetail.boost=true;
      saveTransactionDetails(transactionDetail)
        .then(boostedTransactionDetail => {
          if (boostedTransactionDetail) {
            Object.assign(transactionDetail, boostedTransactionDetail);
            this.$set(transactionDetail, 'boost', true);
          }
        })
        .finally(() => {
          this.$set(transactionDetail, 'resending', false);
          this.$set(transactionDetail, 'resent', true);
          this.$forceUpdate();
        });
    },
    forceUpdateList() {
      try {
        // A trick to force update computed list
        // since the attribute this.transactions is modified outside the component
        this.refreshIndex++;
        this.$forceUpdate();
      } catch (e) {
        console.error('forceUpdateList - method error', e);
      }
    },
    toFixed(args) {
      return toFixed(args);
    },
  },
};
</script>
