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
  <exo-drawer
    ref="accountDetail"
    allow-expand
    :right="!$vuetify.rtl">
    <template slot="title">
      <span v-if="wallet" class="ps-4">
        <template v-if="isAdministration">
          {{ $t('exoplatform.wallet.label.transactionsOfWallet') }}:
          <wallet-reward-profile-chip
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
          {{ $t('exoplatform.wallet.label.lastTransaction') }}
        </template>
      </span>
      <span v-else class="ps-4">
        {{ contractTitle }}
      </span>
    </template>
    <template slot="content">
      <wallet-reward-transactions-list
        id="transactionsList"
        ref="transactionsList"
        class="lastTransactionsList overflow-x-hidden"
        :wallet="wallet"
        :contract-details="contractDetails"
        :fiat-symbol="fiatSymbol"
        :administration="isAdministration"
        :selected-transaction-hash="selectedTransactionHash"
        :selected-contract-method-name="selectedContractMethodName"
        :display-full-transaction="isAdministration"
        :error="error"
        @error="error = $event" />
    </template>
  </exo-drawer>
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
  computed: {
    contractTitle(){
      return this.contractDetails && this.contractDetails.title;
    }
  },
  methods: {
    open(){
      this.$refs.accountDetail.open();
    }
  }
};
</script>
