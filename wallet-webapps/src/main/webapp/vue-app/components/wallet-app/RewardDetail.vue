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
  <v-flex class="transactionsList">
    <div class="rewardDetailTop">
      <v-layout row>
        <v-flex class="xs11 title text-start">
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
    </div>
    <v-divider />
    <div v-if="error && !loading" class="alert alert-error">
      <i class="uiIconError"></i>{{ error }}
    </div>
    <v-layout class="ma-2">
      <v-flex md4 xs12>
        <v-card flat>
          <v-card-title class="title text-wrap text-break">
            {{ $t('exoplatform.wallet.label.rewardBalance') }}
          </v-card-title>
          <v-card-text>
            {{ walletUtils.toFixed(wallet.rewardBalance) }} {{ symbol }}
          </v-card-text>
        </v-card>
      </v-flex>
      <v-flex md4 xs12>
        <v-card flat>
          <v-card-title class="title text-wrap text-break">
            {{ $t('exoplatform.wallet.label.nextPayEstimation') }}
          </v-card-title>
          <v-card-text>
            {{ walletUtils.toFixed(nextPayEstimation) }} {{ symbol }}
          </v-card-text>
        </v-card>
      </v-flex>
      <v-flex md4 xs12>
        <v-card flat>
          <v-card-title class="title text-wrap text-break">
            {{ $t('exoplatform.wallet.label.actualPool') }}
          </v-card-title>
          <v-card-text>
            {{ actualPool }}
          </v-card-text>
        </v-card>
      </v-flex>
    </v-layout>
    <v-card flat>
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
                      {{ formatDate(item.transaction.timestamp, true) }}
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
            <v-list class="px-0 ms-2" dense>
              <v-list-item
                v-for="(plugin, indexReward) in item.rewards"
                :id="`plugin-${plugin.pluginId}`"
                :key="indexReward">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.rewardedFor', {0 : plugin.points, 1 : plugin.pluginId}) }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-start">
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
    formatDate(timeInSeconds, seconds) {
      if (!timeInSeconds) {
        return '';
      }
      return new Date(timeInSeconds * ((seconds && 1) || 1000)).toLocaleDateString(eXo.env.portal.language);
    },
  },
};
</script>