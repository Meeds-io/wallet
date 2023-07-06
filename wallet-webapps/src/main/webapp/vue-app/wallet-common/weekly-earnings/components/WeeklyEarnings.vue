<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2022 Meeds Association
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
  <div v-show="!loading" class="ma-0">
    <div
      flat
      @click="openDrawer"
      class="clickable">
      <div class="pa-0 d-flex justify-center flex-nowrap text-color display-1 font-weight-bold big-number">
        <span class="my-2 tertiary-color">{{ symbol }}</span>
        <span
          :class="typographyClass"
          class="ma-2 text-color font-weight-bold d-flex align-self-center">
          {{ weeklyRewardToDisplay }}
        </span>
      </div>
    </div>
    <wallet-overview-drawer
      ref="walletOverviewDrawer"
      :symbol="symbol" />
  </div>
</template>

<script>
import {computeRewardsByUser} from '../../../wallet-reward/js/RewardService.js';
export default {
  data() {
    return {
      weeklyReward: null,
      wallet: null,
      contractDetails: null,
      loading: true,
    };
  },
  computed: {
    selectedDate() {
      return new Date().toISOString().substring(0, 10);
    },
    symbol() {
      return this.contractDetails?.symbol;
    },
    weeklyRewardToDisplay() {
      return Number.isFinite(Number(this.weeklyReward)) ? Math.trunc(this.weeklyReward) : '';
    }
  },
  created() {
    Promise.all([
      this.init(),
      this.refreshRewards(),
    ]).finally(() => this.loading = false);
  },
  methods: {
    init() {
      return this.walletUtils.initSettings(false, true, true)
        .then(() => {
          if (window.walletSettings) {
            this.wallet = Object.assign({}, window.walletSettings.wallet);    
            this.contractDetails = Object.assign({},window.walletSettings.contractDetail); 
          }
          return this.$nextTick();
        });
    },
    refreshRewards() {
      return computeRewardsByUser(this.selectedDate)
        .then(rewardReport => {
          if (rewardReport) {
            for (const reward of rewardReport.rewards) {
              if (reward.wallet.address.toUpperCase() === this.wallet.address.toUpperCase()) {
                this.weeklyReward = reward.tokensToSend;
              } 
            }
          }
          return this.$nextTick();
        });
    },
    openDrawer() {
      this.$refs.walletOverviewDrawer.open(this.$t('exoplatform.wallet.title.rewardedBalance'));
    }
  },
};
</script>
