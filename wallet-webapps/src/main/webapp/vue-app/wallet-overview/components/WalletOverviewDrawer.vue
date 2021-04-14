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
    ref="rewardsOverviewDrawer"
    :right="!$vuetify.rtl"
    class="rewardsOverviewDrawer">
    <template slot="title">
      {{ title }}
    </template>
    <template slot="content">
      <v-list v-if="rewardsList">
        <wallet-overview-reward-item
          v-for="reward in rewardsList"
          :key="reward.technicalId"
          :reward-item="reward"
          :symbol="symbol"
          class="border-color border-radius ma-4" />
      </v-list>
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
  props: {
    symbol: {
      type: String,
      default: 'sent',
    },
  },
  data: () => ({
    pageSize: 20,
    limit: 20,
    rewardsList: [],
  }),
  computed: {
    hasMore() {
      return this.rewardsList.length >= this.limit;
    },
  },
  methods: {
    reset() {
      this.limit = 20;
      this.size = 0;
      this.rewardsList = [];
    },
    open(title) {
      this.title = title;
      this.reset();
      this.retrieveList();
      this.$refs.rewardsOverviewDrawer.open();
    },
    loadMore() {
      this.limit += this.pageSize;
      this.retrieveList();
    },
    retrieveList() {
      this.$refs.rewardsOverviewDrawer.startLoading();
      return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/reward/list?limit=${this.limit}`, {
        method: 'GET',
        credentials: 'include',
      })
        .then((resp) => resp && resp.ok && resp.json())
        .then(data => {
          if (data && data.length) {
            this.rewardsList = data.filter(reward => reward.transaction && reward.status === 'success');
          } else {
            this.rewardsList = [];
          }
        })
        .finally(() => {
          this.$refs.rewardsOverviewDrawer.endLoading();
        });
    },
  }
};
</script>
