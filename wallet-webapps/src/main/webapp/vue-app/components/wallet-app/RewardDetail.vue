<template>
  <v-flex class="transactionsList">
    <v-card class="rewardDetailTop">
      <v-layout row>
        <v-flex class="xs11 headline text-left">
          <v-icon color="purple" class="px-2">fa-trophy</v-icon>
          <span>
            {{ $t('exoplatform.wallet.label.myRewards') }}
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
    </v-card>
    <v-card class="card--flex-toolbar" flat>
      <div v-if="error && !loading" class="alert alert-error">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <v-layout class="rewardDetailSummary">
        <v-flex>
          <v-row>
            <v-col class="title text-left my-auto">{{ $t('exoplatform.wallet.label.rewardBalance') }}</v-col>
            <v-col class="headline text-right my-auto primary--text">{{ wallet.rewardBalance }} {{ symbol }}</v-col>
          </v-row>
          <v-row>
            <v-col class="title text-left my-auto">{{ $t('exoplatform.wallet.label.nextPayEstimation') }}</v-col>
            <v-col class="headline text-right my-auto primary--text">{{ nextPayEstimation }} {{ symbol }}</v-col>
          </v-row>
          <v-row v-if="actualPool">
            <v-col class="title text-left my-auto">{{ $t('exoplatform.wallet.label.actualPool') }}</v-col>
            <v-col class="headline text-right my-auto primary--text">{{ actualPool }}</v-col>
          </v-row>
        </v-flex>
      </v-layout>

      <div v-if="loading" class="grey--text">
        {{ $t('exoplatform.wallet.message.loadingRecentRewards') }}...
      </div>
      <v-progress-linear
        v-if="loading"
        indeterminate
        color="primary"
        class="mb-0 mt-0" />

      <v-divider />
      <v-expansion-panels
        v-if="pastWalletRewards && pastWalletRewards.length"
        accordion
        focusable>
        <v-expansion-panel
          v-for="(item, index) in pastWalletRewards"
          :id="`reward-${item.transaction.hash}`"
          :key="index"
          :value="item.selected">
          <v-expansion-panel-header class="border-box-sizing px-0 py-0">
            <v-list
              :class="item.selected && 'blue lighten-5'"
              two-line
              ripple
              class="px-2 py-0">
              <v-list-item
                :key="item.hash"
                class="transactionDetailItem autoHeight"
                ripple>
                <v-list-item-content class="transactionDetailContent">
                  <v-list-item-title>
                    {{ $t('exoplatform.wallet.label.rewardedForPeriod', {0: formatDate(item.period.startDateInSeconds), 1: formatDate(item.period.endDateInSeconds)}) }}
                  </v-list-item-title>
                  <v-list-item-subtitle>
                    <v-list-item-action-text v-if="item.transaction.timestamp">
                      {{ formatDate(item.transaction.timestamp) }}
                    </v-list-item-action-text>
                  </v-list-item-subtitle>
                </v-list-item-content>
                <v-list-item-content class="transactionDetailActions">
                  <v-list-item-title class="primary--text no-wrap">
                    {{ walletUtils.toFixed(item.tokensSent) }} {{ symbol }}
                  </v-list-item-title>
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-list class="px-0 ml-2" dense>
              <v-list-item
                v-for="(plugin, indexReward) in item.rewards"
                :id="`plugin-${plugin.pluginId}`"
                :key="indexReward">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.rewardedFor', {0 : plugin.points, 1 : plugin.pluginId}) }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-left">
                  {{ walletUtils.toFixed(plugin.amount) }} {{ symbol }}
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-expansion-panel-content>
        </v-expansion-panel>
        <div v-if="!limitReached">
          <v-btn
            :loading="loading"
            color="primary"
            text
            @click="limit += 10">
            {{ $t('exoplatform.wallet.button.loadMore') }}
          </v-btn>
        </div>
      </v-expansion-panels>
      <v-flex v-else-if="!loading" class="text-center">
        <span>
          {{ $t('exoplatform.wallet.label.noRewardsYet') }}
        </span>
      </v-flex>
    </v-card>
  </v-flex>
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
    contractDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      walletRewards: [],
      error: null,
      loading: false,
      limit: 10,
      currentDateInSeconds: Date.now() / 1000,
    };
  },
  computed: {
    symbol() {
      return this.contractDetails && this.contractDetails.symbol;
    },
    limitReached() {
      return !this.walletRewards || !this.walletRewards.length < this.limit;
    },
    futureWalletReward() {
      return this.walletRewards && this.walletRewards.find(wr => wr.status !== 'success');
    },
    pastWalletRewards() {
      return this.walletRewards && this.walletRewards.filter(wr => wr.status === 'success');
    },
    nextPayEstimation() {
      return (this.futureWalletReward && this.futureWalletReward.tokensToSend) || 0;
    },
    actualPool() {
      return (this.futureWalletReward && this.futureWalletReward.poolName) || '';
    },
  },
  watch: {
    limit() {
      this.init();
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.loading = true;
      this.listRewards(this.limit)
        .then(rewards => this.walletRewards = rewards)
        .catch(e => this.error = String(e))
        .finally(() => this.loading = false);
    },
    listRewards(limit) {
      return fetch(`/portal/rest/wallet/api/reward/list?limit=${limit || 10}`, {
        method: 'GET',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
      }).then((resp) => {
        if (resp && resp.ok) {
          return resp.json();
        } else {
          throw new Error('Error listing rewards');
        }
      }).catch((error) => {
        console.error(error);
        this.error = this.$t('exoplatform.wallet.error.errorListingRewards');
      });
    },
    formatDate(timeInSeconds) {
      if (!timeInSeconds) {
        return '';
      }
      return new Date(timeInSeconds * 1000).toLocaleDateString(eXo.env.portal.language);
    },
  },
};
</script>