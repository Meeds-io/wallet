<template>
  <v-flex
    v-if="contractDetails && contractDetails.icon"
    id="accountDetail"
    class="text-xs-center white layout column">
    <v-card-title class="align-start accountDetailSummary">
      <v-layout column>
        <v-flex id="accountDetailTitle">
          <div class="headline title align-start">
            <v-icon class="primary--text accountDetailIcon">
              {{ contractDetails.icon }}
            </v-icon>
            <span v-if="wallet">
              {{ $t('exoplatform.wallet.label.transactionsOfWallet') }}:
              <profile-chip
                ref="profileChip"
                :profile-technical-id="wallet.technicalId"
                :profile-id="wallet.id"
                :profile-type="wallet.type"
                :space-id="wallet.spaceId"
                :avatar="wallet.avatar"
                :display-name="wallet.name"
                :address="wallet.address" />
            </span>
            <span v-else>
              {{ contractDetails.title }}
            </span>
          </div>
          <h3 v-if="!contractDetails.isContract" class="font-weight-light">
            {{ fiatBalance }}
          </h3>
          <h4 v-if="!contractDetails.isContract" class="grey--text font-weight-light">
            {{ tokenBalance }}
          </h4>
          <h3 v-else class="font-weight-light">
            <template v-if="selectedContractMethodName === 'reward'">
              {{ $t('exoplatform.wallet.label.rewardBalance') }}: {{ rewardBalance }}
            </template>
            <template v-else>
              {{ $t('exoplatform.wallet.label.balance') }}: {{ balance }}
            </template>
          </h3>
        </v-flex>
        <v-btn
          icon
          class="rightIcon"
          @click="$emit('back')">
          <v-icon>
            close
          </v-icon>
        </v-btn>
      </v-layout>
    </v-card-title>

    <transactions-list
      id="transactionsList"
      ref="transactionsList"
      :account="wallet && wallet.address"
      :contract-details="contractDetails"
      :fiat-symbol="fiatSymbol"
      :administration="isAdministration"
      :selected-transaction-hash="selectedTransactionHash"
      :selected-contract-method-name="selectedContractMethodName"
      :error="error"
      @error="error = $event"
      @refresh-balance="refreshBalance" />
  </v-flex>
</template>

<script>
import TransactionsList from './TransactionsList.vue';
import ProfileChip from './ProfileChip.vue';

import {etherToFiat, toFixed} from '../js/WalletUtils.js';

export default {
  components: {
    TransactionsList,
    ProfileChip,
  },
  props: {
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
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
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
    isAdministration: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      error: null,
    };
  },
  computed: {
    fiatBalance() {
      return this.wallet && `${toFixed(etherToFiat(this.wallet.etherBalance))} ${this.fiatSymbol}`;
    },
    rewardBalance() {
      return this.wallet && `${toFixed(this.wallet.rewardBalance)} ${this.tokenSymbol}`;
    },
    tokenBalance() {
      return this.wallet && `${toFixed(this.wallet.tokenBalance)} ${this.tokenSymbol}`;
    },
    tokenSymbol() {
      return this.contractDetails && this.contractDetails.symbol;
    },
  },
  watch: {
    contractDetails() {
      this.error = null;
    },
  },
};
</script>
