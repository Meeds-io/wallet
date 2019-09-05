<template>
  <v-flex
    v-if="contractDetails && contractDetails.icon"
    id="accountDetail"
    class="text-center white layout column">
    <v-card-title class="align-start accountDetailSummary">
      <v-layout row>
        <v-flex class="xs10 offset-xs1 headline title">
          <span v-if="wallet">
            <template v-if="isAdministration">
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
            </template>
            <template v-else-if="selectedContractMethodName === 'reward'">
              {{ $t('exoplatform.wallet.label.rewardTransactionsList') }}
            </template>
            <template v-else>
              {{ $t('exoplatform.wallet.label.transactionsList') }}
            </template>
          </span>
          <span v-else>
            {{ contractDetails.title }}
          </span>
        </v-flex>
        <v-flex class="xs1">
          <v-btn
            icon
            class="rightIcon"
            @click="$emit('back')">
            <v-icon>
              close
            </v-icon>
          </v-btn>
        </v-flex>
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
  watch: {
    contractDetails() {
      this.error = null;
    },
  },
};
</script>
