<template>
  <v-flex
    v-if="contractDetails && contractDetails.icon"
    id="accountDetail"
    class="text-center white layout column">
    <div class="rewardDetailTop">
      <v-layout row class="ma-0">
        <v-flex class="xs11 title text-left">
          <span v-if="wallet" class="pl-4">
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
          <span v-else class="pl-4">
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
    </div>
    <v-divider />

    <transactions-list
      id="transactionsList"
      ref="transactionsList"
      :wallet="wallet"
      :contract-details="contractDetails"
      :fiat-symbol="fiatSymbol"
      :administration="isAdministration"
      :selected-transaction-hash="selectedTransactionHash"
      :selected-contract-method-name="selectedContractMethodName"
      :display-full-transaction="isAdministration"
      :error="error"
      @error="error = $event" />
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
