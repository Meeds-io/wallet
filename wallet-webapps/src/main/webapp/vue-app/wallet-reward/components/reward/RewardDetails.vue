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
  <v-card class="application-body position-static pb-5" flat>
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
            color="tertiary"
            left>
            fas fa-calendar
          </v-icon>
          <span class="font-weight-bold">{{ rangeDateTimeTitle }}</span>
        </v-card-title>
      </v-card>
      <v-spacer />
      <span v-if="sendingInProgress" class="text-subtitle pe-2"> {{ $t('wallet.administration.rewardDetails.sendingProgress') }}... </span>
      <template v-if="!completelyProcessed">
        <div v-if="hasErrorTransactions" class="text-subtitle pe-2 align-self-center">
          <v-icon
            color="warning"
            class="pe-2"
            size="16">
            fas fa-exclamation-triangle
          </v-icon>
          {{ $t('wallet.administration.rewardDetails.sendingError') }}
        </div>
        <div v-else-if="tokensToSend > 0 && !sendingInProgress" class="text-subtitle pe-2">{{ rewardsToSend }}</div>
        <v-tooltip
          bottom
          :disabled="!disabledSendButton">
          <template #activator="{ on }">
            <div v-on="on" class="d-inline-block">
              <v-btn
                :loading="loadingSending"
                :disabled="disabledSendButton || sendingInProgress"
                class="btn btn-primary"
                @click="sendRewards">
                {{ $t('wallet.administration.rewardDetails.label.reward') }}
              </v-btn>
            </div>
          </template>
          <span> {{ disabledSendButtonLabel }} </span>
        </v-tooltip>
      </template>
      <extension-registry-components
        v-else
        :params="{rewardReport}"
        name="WalletRewarding"
        type="wallet-reward-send-header-extensions"
        parent-element="div"
        element="div" />
    </v-toolbar>
    <application-toolbar
      :left-button="{
        icon: 'fa-file-excel',
        text: 'Export',
      }"
      :right-select-box="{
        selected: status,
        items: walletRewardStatus,
      }"
      @filter-select-change="status = $event" />
    <v-data-table
      :headers="identitiesHeaders"
      :items="filteredIdentitiesList"
      :items-per-page="1000"
      :loading="loading"
      :sort-desc.sync="sortDescending"
      :sort-by.sync="sortBy"
      item-key="identityId"
      disable-pagination
      hide-default-footer
      must-sort>
      <template #item="{item}">
        <wallet-reward-details-item
          :key="item.wallet.id"
          :reward="item"
          :contract-details="contractDetails"
          :token-symbol="tokenSymbol"
          :completely-processed="completelyProcessed"
          :transaction-ether-scan-link="transactionEtherScanLink"
          @open-contribution-details="openContributionDetails"
          @open-rewards-details="openRewardsDetails" />
      </template>
    </v-data-table>
    <v-toolbar
      v-if="hasMore"
      color="transparent"
      flat>
      <v-col class="fill-width border-box-sizing">
        <v-btn
          class="btn"
          :loading="loading"
          :disabled="loading"
          block
          @click="loadMore">
          <span class="ms-2 d-inline">
            {{ $t("realization.label.loadMore") }}
          </span>
        </v-btn>
      </v-col>
    </v-toolbar>
    <users-leaderboard-profile-achievements-drawer
      ref="profileStatsDrawer"
      :from-date-in-second="startDateInSeconds"
      :to-date-in-second="endDateInSeconds"
      go-back-button
      relative />
    <wallet-rewards-details-drawer
      ref="rewardsDetailsDrawer"
      :wallet-reward="selectedWalletReward"
      :token-symbol="tokenSymbol" />
  </v-card>
</template>

<script>
export default {
  props: {
    rewardReport: {
      type: Object,
      default: null
    },
    adminWallet: {
      type: Object,
      default: null
    },
    contractDetails: {
      type: Object,
      default: null
    },
    configuredBudget: {
      type: Number,
      default: 0
    },
    transactionEtherScanLink: {
      type: String,
      default: function () {
        return null;
      },
    }
  },
  data: () => ({
    loadingSending: false,
    loading: false,
    currentTimeInSeconds: Date.now() / 1000,
    lang: eXo.env.portal.language,
    dateFormat: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    },
    walletRewards: [],
    selectedWalletReward: null,
    status: 'VALID',
    sortBy: 'tokensToSend',
    sortDescending: true,
    walletRewardsCount: 0,
    pageSize: 100,
    page: 0,
  }),
  computed: {
    walletRewardStatus() {
      return [{
        text: 'Eligible Members',
        value: 'VALID',
      },{
        text: 'Non Eligible',
        value: 'INVALID',
      }];
    },
    identitiesHeaders() {
      return [
        {
          text: this.$t('wallet.administration.rewardDetails.label.name'),
          value: 'name',
          align: 'start',
          sortable: false,
        },
        {
          text: this.$t('wallet.administration.rewardDetails.label.points'),
          value: 'points',
          align: 'center',
          sortable: true,
        },
        {
          text: this.$t('wallet.administration.rewardDetails.label.status'),
          value: 'status',
          align: 'center',
          sortable: false,
        },
        {
          text: this.$t('wallet.administration.rewardDetails.label.rewards'),
          value: 'tokensToSend',
          align: 'center',
          sortable: true,
        },
        {
          text: this.$t('wallet.administration.rewardDetails.label.actions'),
          value: 'actions',
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
    starDateFormat() {
      return new window.Intl.DateTimeFormat(this.lang, this.dateFormat).format(new Date(this.startDateInSeconds * 1000 - new Date().getTimezoneOffset() * 60 * 1000));
    },
    toDateFormat() {
      return new window.Intl.DateTimeFormat(this.lang, this.dateFormat)
        .format(new Date(this.endDateInSeconds * 1000 - 86400 * 1000 - new Date().getTimezoneOffset() * 60 * 1000));
    },
    completelyProcessed() {
      return this.rewardReport?.completelyProcessed;
    },
    endDateInSeconds() {
      return this.period?.endDateInSeconds;
    },
    startDateInSeconds() {
      return this.period?.startDateInSeconds;
    },
    rewardPeriodType() {
      return this.period?.rewardPeriodType;
    },
    isNotPastPeriod() {
      return !this.period || this.endDateInSeconds > this.currentTimeInSeconds;
    },
    filteredIdentitiesList() {
      return this.walletRewards?.filter((wallet) => (wallet.enabled || wallet.tokensSent || wallet.tokensToSend)) || [];
    },
    tokenSymbol() {
      return window.walletSettings.contractDetail?.symbol;
    },
    sendingInProgress() {
      return this.walletRewards?.some(item => item.status === 'pending');
    },
    hasErrorTransactions() {
      return this.rewardReport?.failedTransactionCount > 0;
    },
    tokensToSend() {
      return this.rewardReport?.tokensToSend;
    },
    formattedTokensToSend() {
      return this.valueFormatted(this.tokensToSend);
    },
    points() {
      return this.rewardReport?.points;
    },
    rewardsToSend() {
      return this.$t('wallet.administration.rewardDetails.label.rewardsToSend', {
        0: this.formattedTokensToSend,
        1: this.points
      });
    },
    tokenBalance() {
      return this.adminWallet?.tokenBalance || 0;
    },
    etherBalance() {
      return this.adminWallet?.etherBalance || 0;
    },
    lowEtherBalance() {
      return this.etherBalance < 1;
    },
    lowTokenBalance() {
      return this.configuredBudget > this.tokenBalance;
    },
    balanceBelowBudget() {
      return this.lowEtherBalance || this.lowTokenBalance;
    },
    balanceBelowBudgetLabel() {
      return this.lowEtherBalance ? this.$t('wallet.administration.lowEtherBalance') : this.$t('wallet.administration.lowTokenBalance');
    },
    disabledSendButton() {
      return this.isNotPastPeriod || this.balanceBelowBudget;
    },
    disabledSendButtonLabel() {
      return this.isNotPastPeriod ? this.$t('wallet.administration.rewardCard.status.inPeriod') : this.balanceBelowBudgetLabel;
    },
    walletRewardsFilter() {
      return {
        page: this.page,
        size: this.pageSize,
        periodId: this.period.id,
        status: this.status,
        sortField: this.sortBy,
        sortDir: this.sortDescending ? 'desc' : 'asc',
      };
    },
    hasMore() {
      return this.walletRewards.length < this.walletRewardsCount;
    },
  },
  watch: {
    status() {
      this.page = 0;
      this.walletRewards = [];
      this.getWalletRewards();
    },
    sortBy(newVal, oldVal) {
      if (newVal !== oldVal) {
        if (this.sortDescending){
          this.sortDescending = false;
        }
        this.sortUpdated();
      }
    },
    sortDescending(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.sortUpdated();
      }
    },
  },
  created() {
    this.getWalletRewards();
  },
  methods: {
    valueFormatted(max) {
      return new Intl.NumberFormat(this.lang, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(max);
    },
    sendRewards() {
      this.loadingSending = true;
      return this.$rewardService.sendRewards(this.period).then(() => {
        this.$rewardService.computeRewardsByPeriod(this.period)
          .then(rewardReport => {
            this.$emit('reward-report-updated', rewardReport);
          }).then(() => {
            this.status = 'VALID';
            this.walletRewards = [];
            this.getWalletRewards();
          }).finally(() => {
            this.loadingSending = false;
          });
      });
    },
    openContributionDetails(userId) {
      this.$refs?.profileStatsDrawer?.openByIdentityId(userId, this.rewardPeriodType);
    },
    openRewardsDetails(walletReward) {
      this.selectedWalletReward = walletReward;
      this.$refs?.rewardsDetailsDrawer?.open();
    },
    sortUpdated() {
      if (!this.loading) {
        this.loading = true;
        this.page = 0;
        this.walletRewards = [];
        this.getWalletRewards();
      }
    },
    getWalletRewards() {
      this.loading = true;
      return this.$rewardService.getWalletRewards(this.walletRewardsFilter).then(data => {
        const newRewards = data?._embedded?.walletRewardList || [];
        this.walletRewards = [...this.walletRewards, ...newRewards];
        this.walletRewardsCount = data?.page?.totalElements || 0;
      }).finally(() => {
        this.loading = false;
        this.$root.$applicationLoaded();
      });
    },
    loadMore() {
      this.page++;
      this.getWalletRewards();
    },
  },
};
</script>