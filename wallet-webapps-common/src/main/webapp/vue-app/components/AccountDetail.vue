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
  <v-flex
    v-if="contractDetails && contractDetails.icon"
    id="accountDetail"
    class="text-center white layout column">
    <div class="rewardDetailTop">
      <v-layout row class="ma-0">
        <v-flex class="xs11 title text-start">
          <span v-if="wallet" class="ps-4">
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
          <span v-else class="ps-4">
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
