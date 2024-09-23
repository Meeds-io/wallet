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
  <v-card flat>
    <v-toolbar flat>
      <v-card
        class="d-flex align-center"
        flat
        @click="$emit('back')">
        <v-btn
          class="width-auto ms-n3"
          icon>
          <v-icon size="18" class="icon-default-color mx-2">fa-arrow-left</v-icon>
        </v-btn>
        <v-card-title class="px-2 py-1">
          <v-icon
            class="secondary--text"
            left>
            fas fa-calendar
          </v-icon>
          <span class="font-weight-bold">{{ rangeDateTimeTitle }}</span>
        </v-card-title>
      </v-card>
      <v-spacer />
      <span v-if="sendingInProgress" class="text-subtitle pe-2"> {{ $t('wallet.administration.rewardDetails.sendingProgress') }}... </span>
      <v-tooltip
        v-if="!completelyProceeded"
        bottom
        :disabled="!isNotPastPeriod">
        <template #activator="{ on }">
          <div v-on="on" class="d-inline-block">
            <v-btn
              :loading="loadingSending"
              :disabled="isNotPastPeriod || sendingInProgress"
              class="btn btn-primary"
              @click="sendRewards">
              Reward
            </v-btn>
          </div>
        </template>
        <span>{{ $t('wallet.administration.rewardCard.status.inPeriod') }}</span>
      </v-tooltip>
    </v-toolbar>
    <application-toolbar
      :left-button="{
        icon: 'fa-file-excel',
        text: 'Export',
      }"
      :right-text-filter="{
        minCharacters: 3,
        placeholder: this.$t('wallet.administration.rewardDetails.searchLabel'),
        tooltip: this.$t('wallet.administration.rewardDetails.searchLabel')
      }"
      @filter-text-input-end-typing="search = $event" />
    <v-data-table
      :headers="identitiesHeaders"
      :items="filteredIdentitiesList"
      :items-per-page="1000"
      :loading="loading"
      item-key="identityId"
      hide-default-footer
      sortable>
      <template #item="{item}">
        <wallet-reward-details-item
          :key="item.wallet.id"
          :reward="item"
          :token-symbol="tokenSymbol"
          :completely-proceeded="completelyProceeded" />
      </template>
    </v-data-table>
  </v-card>
</template>

<script>
export default {
  props: {
    loading: {
      type: Boolean,
      default: false,
    },
    rewardReport: {
      type: Object,
      default: null
    }
  },
  data: () => ({
    search: null,
    loadingSending: false,
    currentTimeInSeconds: Date.now() / 1000,
    lang: eXo.env.portal.language,
    dateFormat: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    },
  }),
  computed: {
    identitiesHeaders() {
      return [
        {
          text: this.$t('wallet.administration.rewardDetails.label.name'),
          align: 'start',
          sortable: false,
        },
        {
          text: this.$t('wallet.administration.rewardDetails.label.points'),
          align: 'center',
          sortable: false,
        },
        {
          text: this.$t('wallet.administration.rewardDetails.label.status'),
          align: 'center',
          sortable: false,
        },
        {
          text: this.$t('wallet.administration.rewardDetails.label.rewards'),
          align: 'center',
          sortable: false,
        },
        {
          text: this.$t('wallet.administration.rewardDetails.label.actions'),
          align: 'center',
          sortable: false,
        },
      ];
    },
    rangeDateTimeTitle() {
      return `${this.starDateFormat} to ${this.toDateFormat}`;
    },
    period() {
      return this.rewardReport?.period;
    },
    startDate() {
      return new Date(this.period?.startDate);
    },
    endDate() {
      return new Date(this.period?.endDate);
    },
    starDateFormat() {
      return this.startDate?.toLocaleString(this.lang, this.dateFormat);
    },
    toDateFormat() {
      return this.endDate?.toLocaleString(this.lang, this.dateFormat);
    },
    completelyProceeded() {
      return this.rewardReport?.completelyProceeded;
    },
    isNotPastPeriod() {
      return !this.period || this.period.endDateInSeconds > this.currentTimeInSeconds;
    },
    walletRewards() {
      return (this.rewardReport && this.rewardReport.rewards) || [];
    },
    filteredIdentitiesList() {
      return (this.walletRewards && this.walletRewards.filter((wallet) => (wallet.enabled || wallet.tokensSent || wallet.tokensToSend) && this.filterItemFromList(wallet, this.search))) || [];
    },
    tokenSymbol() {
      return window.walletSettings.contractDetail?.symbol;
    },
    sendingInProgress() {
      return this.rewardReport?.rewards?.some(item => item.status === 'pending');
    }
  },
  methods: {
    filterItemFromList(walletReward, searchText) {
      if (!searchText || !searchText.length) {
        return true;
      }
      searchText = searchText.trim().toLowerCase();
      const name = walletReward?.wallet?.name?.toLowerCase();
      if (name.indexOf(searchText) > -1) {
        return true;
      }
      const address = walletReward?.wallet?.address?.toLowerCase();
      if (address.indexOf(searchText) > -1) {
        return true;
      }
    },
    sendRewards() {
      this.loadingSending = true;
      return this.$rewardService.sendRewards(this.period).then(() => {
        this.$rewardService.computeRewardsByPeriod(this.period)
          .then(rewardReport => {
            this.rewardReport = rewardReport;
          }).finally(() => {
            this.loadingSending = false;
          });
      });
    },
  },
};
</script>