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
              Wallet transactions of:
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
            {{ balance }}
          </h4>
          <h3 v-else class="font-weight-light">
            <template v-if="selectedContractMethodName === 'reward'">
              Reward balance: {{ rewardBalance }}
            </template>
            <template v-else>
              Balance: {{ balance }}
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
      :account="walletAddress"
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

import {retrieveContractDetails} from '../js/TokenUtils.js';
import {etherToFiat, toFixed} from '../js/WalletUtils.js';

export default {
  components: {
    TransactionsList,
    ProfileChip,
  },
  props: {
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isDisplayOnly: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
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
      // Avoid refreshing list and balance twice
      refreshing: false,
      error: null,
    };
  },
  computed: {
    fiatBalance() {
      return this.contractDetails && this.contractDetails.balanceFiat ? `${toFixed(this.contractDetails.balanceFiat)} ${this.fiatSymbol}` : `0 ${this.fiatSymbol}`;
    },
    rewardBalance() {
      return this.contractDetails && this.contractDetails.rewardBalance ? `${toFixed(this.contractDetails.rewardBalance)} ${this.contractDetails && this.contractDetails.symbol}` : '';
    },
    balance() {
      return this.contractDetails && this.contractDetails.balance ? `${toFixed(this.contractDetails.balance)} ${this.contractDetails && this.contractDetails.symbol}` : '';
    },
  },
  watch: {
    contractDetails() {
      this.error = null;
    },
  },
  methods: {
    refreshBalance() {
      if (!this.contractDetails) {
        return Promise.resolve(null);
      }
      return window.localWeb3.eth.getBalance(this.walletAddress).then((balance) => {
        if (!this.contractDetails) {
          return Promise.resolve(null);
        }
        balance = window.localWeb3.utils.fromWei(String(balance), 'ether');
        if (this.contractDetails.isContract) {
          this.contractDetails.etherBalance = balance;
          return retrieveContractDetails(this.walletAddress, this.contractDetails, this.isAdministration).then(() => this.$forceUpdate());
        } else {
          this.$set(this.contractDetails, 'balance', balance);
          this.$set(this.contractDetails, 'balanceFiat', etherToFiat(balance));
          if (this.contractDetails.details) {
            this.$set(this.contractDetails.details, 'balance', this.contractDetails.balance);
            this.$set(this.contractDetails.details, 'balanceFiat', this.contractDetails.balanceFiat);
          }
          this.$forceUpdate();
        }
      });
    },
    newTransactionPending(transaction, contractDetails) {
      if (this.$refs.transactionsList) {
        this.$refs.transactionsList.init(true);
      }
      this.$emit('transaction-sent');
    },
  },
};
</script>
