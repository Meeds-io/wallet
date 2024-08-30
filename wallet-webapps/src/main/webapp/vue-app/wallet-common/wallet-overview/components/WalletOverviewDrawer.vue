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
    ref="drawer"
    v-model="drawer"
    :right="!$vuetify.rtl"
    :loading="loading"
    class="rewardsOverviewDrawer">
    <template slot="title">
      {{ $t('wallet.overview.rewards.title') }}
    </template>
    <template v-if="drawer" slot="content">
      <v-tabs
        v-model="tabName"
        slider-size="4"
        class="px-4">
        <v-tab
          tab-value="rewards"
          href="#rewards">
          {{ $t('wallet.overview.rewards') }}
        </v-tab>
        <v-tab
          tab-value="transactions"
          href="#transactions">
          {{ $t('wallet.overview.transactions') }}
        </v-tab>
      </v-tabs>
      <v-tabs-items
        v-model="tabName"
        class="px-4">
        <v-tab-item value="rewards">
          <v-list v-if="rewardsList">
            <wallet-overview-reward-item
              v-for="reward in rewardsList"
              :key="reward.technicalId"
              :reward-item="reward"
              :symbol="symbol"
              class="border-color border-radius" />
          </v-list>
        </v-tab-item>
        <v-tab-item value="transactions">
          <wallet-reward-transactions-list
            class="lastTransactionsList overflow-x-hidden"
            :wallet="wallet"
            :contract-details="contract" />
        </v-tab-item>
      </v-tabs-items>
    </template>
    <template v-if="hasMore" slot="footer">
      <v-spacer />
      <v-btn
        :loading="loading"
        :disabled="loading"
        class="loadMoreButton ma-auto btn"
        block
        @click="loadNextPage">
        {{ $t('exoplatform.wallet.button.loadMore') }}
      </v-btn>
      <v-spacer />
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    drawer: false,
    pageSize: 20,
    limit: 20,
    rewardsList: [],
    loading: false,
    contract: null,
    wallet: null,
    tabName: 'rewards',
  }),
  computed: {
    hasMore() {
      return this.rewardsList.length >= this.limit;
    },
    symbol() {
      return this.contract?.symbol;
    },
    identity() {
      return {
        id: this.wallet?.technicalId,
        enabled: !this.wallet?.disabledUser,
        deleted: this.wallet?.deletedUser,       
        fullName: this.wallet?.name,
        avatar: this.wallet?.avatar,
        external: false,
      };
    },
    balanceToDisplay() {
      return Number.isFinite(Number(this.wallet?.tokenBalance))
        ? new Intl.NumberFormat(eXo.env.portal.language, {
          style: 'decimal',
          minimumFractionDigits: 0,
          maximumFractionDigits: 0,
        }).format(Math.trunc(this.wallet?.tokenBalance))
        : 0;
    },
  },
  created() {
    this.$root.$on('wallet-overview-drawer', this.open);
  },
  methods: {
    open(tabName) {
      this.tabName = tabName || 'rewards';
      this.reset();
      this.retrieveList();
      this.$refs.drawer.open();
    },
    reset() {
      this.limit = 20;
      this.size = 0;
      this.wallet = window.walletSettings.wallet
        && {...window.walletSettings.wallet}
        || {};
      this.contract = window.walletSettings.contractDetail
        && {...window.walletSettings.contractDetail}
        || {};
      this.rewardsList = [];
    },
    loadMore() {
      this.limit += this.pageSize;
      this.retrieveList();
    },
    retrieveList() {
      this.loading = true;
      return this.$rewardService.getRewardsByUser(this.limit)
        .then(data => {
          if (data && data.length) {
            this.rewardsList = data.filter(reward => reward.transaction && reward.status === 'success');
          } else {
            this.rewardsList = [];
          }
        })
        .finally(() => this.loading = false);
    },
  }
};
</script>
