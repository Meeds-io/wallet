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
  <div class="ma-0">
    <div
      flat
      @click="$refs.accountDetail.open()"
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
    <wallet-reward-account-detail
      ref="accountDetail"
      :fiat-symbol="symbol"
      :wallet="wallet"
      :contract-details="contractDetails" />
  </div>
</template>

<script>
import {computeRewardsByUser} from '../../../../../../../wallet-webapps-reward/src/main/webapp/vue-app/js/RewardService.js';
export default {
  data() {
    return {
      weeklyReward: null,
      wallet: null,
      contractDetails: null
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
    this.init();
    this.refreshRewards();
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
      computeRewardsByUser(this.selectedDate)
        .then(rewardReport => {
          if (rewardReport) {
            for (let i=0; i<rewardReport.rewards.length; i++) {
              if (rewardReport.rewards[i].wallet.address.toUpperCase() === this.wallet.address.toUpperCase()) {
                this.weeklyReward = rewardReport.rewards[i].tokensToSend;
              } 
            }
          }
        });
    }
  },
};
</script>
