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
          <span class="text-header font-weight-light">{{ rangeDateTimeTitle }}</span>
        </v-card-title>
      </v-card>
      <v-spacer />
      <v-tooltip
        v-if="!completelyProceeded"
        bottom
        :disabled="!isNotPastPeriod">
        <template #activator="{ on }">
          <div v-on="on" class="d-inline-block">
            <v-btn
              class="btn btn-primary"
              :disabled="isNotPastPeriod"
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
        placeholder: 'Filter by name',
      }"
      @filter-text-input-end-typing="term = $event" />

    <v-data-table
      :headers="identitiesHeaders"
      :items="filteredIdentitiesList"
      :items-per-page="1000"
      :loading="loading"
      item-key="identityId"
      hide-default-footer
      sortable>
      <template slot="item" slot-scope="props">
        <tr :active="props.selected">
          <td class="text-start">
            <v-avatar size="36px">
              <img
                :src="props.item.wallet.avatar"
                onerror="this.src = '/platform-ui/skin/images/avatar/DefaultSpaceAvatar.png'"
                alt="">
            </v-avatar>
            <wallet-reward-profile-chip
              :address="props.item.wallet.address"
              :profile-id="props.item.wallet.id"
              :profile-technical-id="props.item.wallet.technicalId"
              :space-id="props.item.wallet.spaceId"
              :profile-type="props.item.wallet.type"
              :display-name="props.item.wallet.name"
              :enabled="props.item.wallet.enabled"
              :disabled-in-reward-pool="props.item.disabledPool"
              :deleted-user="props.item.wallet.deletedUser"
              :disabled-user="props.item.wallet.disabledUser"
              :avatar="props.item.wallet.avatar"
              :initialization-state="props.item.wallet.initializationState"
              display-no-address />
          </td>
          <td class="text-center">
            <span>
              {{ props.item.points }}
            </span>
          </td>
          <td class="text-center">
            <template v-if="!props.item.status">
              <v-icon
                v-if="!props.item.wallet.address"
                :title="$t('exoplatform.wallet.label.noAddress')"
                color="warning">
                warning
              </v-icon>
              <v-icon
                v-else-if="!props.item.amount"
                :title="$t('exoplatform.wallet.label.noEnoughEarnedPoints')"
                color="warning">
                warning
              </v-icon>
              <div v-else>
                -
              </div>
            </template>
            <v-progress-circular
              v-else-if="props.item.status === 'pending'"
              color="primary"
              indeterminate
              size="20" />
            <v-icon
              v-else
              :color="props.item.status === 'success' ? 'success' : 'error'"
              :title="props.item.status === 'success' ? 'Successfully proceeded' : props.item.status === 'pending' ? 'Transaction in progress' : 'Transaction error'"
              v-text="props.item.status === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'" />
          </td>
          <td class="text-center">
            <span
              v-if="props.item.amount"
              :title="$t('exoplatform.wallet.label.amountSent')"
              class="grey--text text--darken-1">
              <span class="symbol fundsLabels"> {{ tokenSymbol }} </span>{{ walletUtils.toFixed(props.item.amount) }}
            </span>
            <span
              v-else
              :title="$t('exoplatform.wallet.label.noRewardsForPeriod')"
              class="grey--text text--darken-1">
              <span class="symbol fundsLabels"> {{ tokenSymbol }} </span> 0
            </span>
          </td>
        </tr>
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
    term: null,
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
          text: this.$t('exoplatform.wallet.label.name'),
          align: 'start',
          sortable: true,
          value: 'wallet.name',
        },
        {
          text: 'Points',
          align: 'center',
          sortable: true,
          value: 'points',
        },
        {
          text: this.$t('exoplatform.wallet.label.status'),
          align: 'center',
          sortable: true,
          value: 'status',
        },
        {
          text: 'Rewards',
          align: 'center',
          sortable: true,
          value: 'tokensToSend',
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
  },
  methods: {
    openDetails() {
      this.$emit('openDetails');
    },
    filterItemFromList(walletReward, searchText) {
      if (!searchText || !searchText.length) {
        return true;
      }
      searchText = searchText.trim().toLowerCase();
      const name = walletReward && walletReward.wallet && walletReward.wallet.name && walletReward.wallet.name.toLowerCase();
      if (name.indexOf(searchText) > -1) {
        return true;
      }
      const address = walletReward && walletReward.wallet && walletReward.wallet.address && walletReward.wallet.address.toLowerCase();
      if (address.indexOf(searchText) > -1) {
        return true;
      }
      const poolName = walletReward && walletReward.poolName && walletReward.poolName.toLowerCase();
      return searchText === '-' || (poolName.indexOf(searchText) > -1);

    },
    sendRewards() {
      this.error = null;
      return this.$rewardService.sendRewards(this.period)
        .catch(e => {
          this.error = String(e);
        })
        .finally(() => {
          this.$emit('refresh');
        });
    },
  },
};
</script>