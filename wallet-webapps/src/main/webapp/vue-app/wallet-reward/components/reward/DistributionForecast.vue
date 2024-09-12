<!--
  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io

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
  <v-card
    class="ma-5"
    flat
    outlined>
    <v-card-title>{{ $t('wallet.administration.distributionForecast') }}</v-card-title>
    <v-card-subtitle class="pb-0">{{ $t('wallet.administration.distributionForecast.subtile') }}</v-card-subtitle>
    <v-list class="transparent">
      <v-list-item
        v-for="item in forecast"
        :class="item.class"
        :key="item.title">
        <v-list-item-title>{{ item.title }}</v-list-item-title>
        <v-list-item-title class="text-right">
          <span v-if="item.unit" :class="item.unitClass">{{ item.unit }}</span> {{ item.value }}
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </v-card>
</template>

<script>

export default {
  props: {
    rewardReport: {
      type: Object,
      default: null
    },
    settingsToSave: {
      type: Object,
      default: null
    },
  },
  data: () => ({

  }),
  computed: {
    forecast() {
      return [
        { title: this.$t('wallet.administration.distributionForecast.participants'), value: this.participants },
        { title: this.$t('wallet.administration.distributionForecast.eligibleContributors'), value: this.eligibleContributorsCount },
        { title: this.$t('wallet.administration.distributionForecast.acceptedContributions'), value: `${this.totalRewardsToDisplay} ${this.$t('wallet.administration.budgetConfiguration.points')}` },
        { title: this.$t('wallet.administration.distributionForecast.budget'), value: this.budgetToDisplay , class: 'font-weight-bold' , unit: this.tokenSymbol, unitClass: 'symbol fundsLabels'},
      ];
    },
    eligibleContributorsCount() {
      return this.rewardReport?.validRewards?.length;
    },
    walletRewards() {
      return this.rewardReport?.rewards;
    },
    participants() {
      return this.rewardReport?.rewards?.length;
    },
    tokenSymbol() {
      return window.walletSettings?.contractDetail?.symbol;
    },
    budgetType() {
      return this.settingsToSave?.budgetType;
    },
    amount() {
      return this.settingsToSave?.amount;
    },
    budget() {
      return this.budgetType === 'FIXED' ? this.amount : this.amount * this.eligibleContributorsCount;
    },
    budgetToDisplay() {
      return this.valueFormatted(this.budget);
    },
    totalRewards() {
      let totalRewards = 0;
      this.walletRewards.forEach(walletReward => {
        if (walletReward && walletReward.rewards) {
          walletReward.rewards.forEach(rewardDetail => totalRewards += rewardDetail.points);
        }
      });
      return totalRewards;
    },
    totalRewardsToDisplay() {
      return this.valueFormatted(this.totalRewards);
    },
  },
  methods: {
    valueFormatted(max) {
      return new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(max);
    },
  }
};
</script>