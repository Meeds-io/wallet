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
  <div v-if="rewards && rewards.length">
    <v-list-item>
      <v-list-item-content class="align-end text-start">
        <v-list-item-title :title="period">
          <i class="fa fa-trophy px-2 tertiary--text"></i>
          {{ periodShort }}
          <i class="uiIconInformation text-sub-title my-auto ms-3"></i>
        </v-list-item-title>
        <v-list-item-subtitle class="ps-10">
          {{ $t('exoplatform.wallet.label.sentDate') }}: {{ sentDate }}
        </v-list-item-subtitle>
      </v-list-item-content>
      <v-list-item-icon class="my-auto">
        <span class="primary--text">
          {{ rewardAmount }}
        </span>
      </v-list-item-icon>
    </v-list-item>

    <wallet-overview-reward-plugin-item
      v-for="(plugin, i) in rewards"
      :id="`plugin-${plugin.pluginId}`"
      :key="i"
      :reward-item="rewardItem"
      :plugin="plugin"
      :symbol="symbol" />
  </div>
</template>
<script>
export default {
  props: {
    rewardItem: {
      type: Object,
      default: () => ({}),
    },
    symbol: {
      type: String,
      default: 'sent',
    },
  },
  data: () => ({
    lang: eXo.env.portal.language,
    dateFormat: {
      dateStyle: 'long',
    },
  }),
  computed: {
    rewards() {
      return this.rewardItem && this.rewardItem.rewards && this.rewardItem.rewards.length && this.rewardItem.rewards.filter(plugin => plugin.amount);
    },
    rewardEarnedAmount() {
      return parseInt(this.rewardItem.tokensSent * 100) / 100;
    },
    rewardAmount() {
      return `${this.rewardEarnedAmount} ${this.symbol}`;
    },
    sentDate() {
      return new window.Intl.DateTimeFormat(this.lang, this.dateFormat).format(new Date(this.rewardItem.transaction.sentTimestamp));
    },
    periodStartDate() {
      return new window.Intl.DateTimeFormat(this.lang, this.dateFormat).format(new Date(this.rewardItem.period.startDateInSeconds * 1000 - new Date().getTimezoneOffset() * 60 * 1000));
    },
    periodEndDate() {
      return new window.Intl.DateTimeFormat(this.lang, this.dateFormat).format(new Date((this.rewardItem.period.endDateInSeconds - new Date().getTimezoneOffset() * 60 * 1000 - 1) * 1000));
    },
    periodLabelKey() {
      return `exoplatform.wallet.label.${this.rewardItem.period.rewardPeriodType.toLowerCase()}ShortPeriod`;
    },
    periodShort() {
      const startDate = new Date(this.rewardItem.period.startDateInSeconds * 1000 + 86400000);
      const year = startDate.getFullYear();

      switch(this.rewardItem.period.rewardPeriodType) {
      case 'WEEK':
      case 'week': {
        const weekNumber = this.getWeekNumber(startDate);
        return this.$t(this.periodLabelKey, {
          0: weekNumber,
          1: year,
        });
      }
      case 'MONTH':
      case 'month': {
        const month = new window.Intl.DateTimeFormat(this.lang, {month: 'long'}).format(startDate);
        return this.$t(this.periodLabelKey, {
          0: month,
          1: year,
        });
      }
      case 'QUARTER':
      case 'quarter': {
        const quarterNumber = this.getQuarterNumber(startDate);
        return this.$t(this.periodLabelKey, {
          0: quarterNumber,
          1: year,
        });
      }
      case 'SEMESTER':
      case 'semester': {
        const semesterNumber = this.getSemesterNumber(startDate);
        return this.$t(this.periodLabelKey, {
          0: semesterNumber,
          1: year,
        });
      }
      case 'YEAR':
      case 'year': {
        return this.$t(this.periodLabelKey, {
          0: year,
        });
      }
      }
      return '';
    },
    period() {
      return this.$t(`exoplatform.wallet.label.rewardedForPeriod`, {
        0: this.periodStartDate,
        1: this.periodEndDate,
      });
    },
  },
  methods: {
    getSemesterNumber(startDate) {
      return Math.ceil((startDate.getMonth() + 1) / 6);
    },
    getQuarterNumber(startDate) {
      return Math.ceil((startDate.getMonth() + 1) / 3);
    },
    getWeekNumber(startDate) {
      const date = new Date(startDate);
      date.setHours(0, 0, 0, 0);
      date.setDate(date.getDate() + 3 - (date.getDay() + 6) % 7);

      const week1Date = new Date(date.getFullYear(), 0, 4);
      return 1 + Math.round(((date.getTime() - week1Date.getTime()) / 86400000 - 3 + (week1Date.getDay() + 6) % 7) / 7);
    },
  },
};
</script>