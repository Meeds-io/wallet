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
    :loading="loadingForecast"
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
    distributionForecast: {
      type: Object,
      default: null
    },
    loadingForecast: {
      type: Boolean,
      default: false
    },
  },
  data: () => ({

  }),
  computed: {
    forecast() {
      return [
        { title: this.$t('wallet.administration.distributionForecast.participants'), value: this.participantsCount },
        { title: this.$t('wallet.administration.distributionForecast.eligibleContributors'), value: this.eligibleContributorsCount },
        { title: this.$t('wallet.administration.distributionForecast.acceptedContributions'), value: `${this.acceptedContributionsToDisplay} ${this.$t('wallet.administration.budgetConfiguration.points')}` },
        { title: this.$t('wallet.administration.distributionForecast.budget'), value: this.budgetToDisplay , class: 'font-weight-bold' , unit: this.tokenSymbol, unitClass: 'symbol fundsLabels'},
      ];
    },
    eligibleContributorsCount() {
      return this.distributionForecast?.eligibleContributorsCount;
    },
    participantsCount() {
      return this.distributionForecast?.participantsCount;
    },
    tokenSymbol() {
      return window.walletSettings?.contractDetail?.symbol;
    },
    budget() {
      return this.distributionForecast?.budget;
    },
    budgetToDisplay() {
      return this.valueFormatted(this.budget);
    },
    acceptedContributions() {
      return this.distributionForecast?.acceptedContributions;
    },
    acceptedContributionsToDisplay() {
      return this.valueFormatted(this.acceptedContributions);
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