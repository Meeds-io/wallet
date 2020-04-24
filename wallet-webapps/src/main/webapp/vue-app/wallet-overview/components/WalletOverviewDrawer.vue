<template>
  <exo-drawer
    ref="rewardsOverviewDrawer"
    class="rewardsOverviewDrawer"
    right>
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
