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
    class="d-flex flex-column overflow-hidden"
    height="160"
    max-height="160"
    outlined
    hover>
    <div v-if="!isValid">
      <div
        class="d-flex position-absolute full-width full-height mask-color z-index-one">
        <wallet-reward-card-mask-content :range-date="rangeDateTimeTitle" />
      </div>
    </div>
    <v-card
      class="d-flex flex-column full-height"
      width="300"
      flat
      @click="openDetails">
      <v-card-title class="px-2 py-1">
        <v-icon
          color="tertiary"
          left>
          fas fa-calendar
        </v-icon>
        <span class="text-header font-weight-light">{{ rangeDateTimeTitle }}</span>
      </v-card-title>
      <template v-if="isValid">
        <template v-if="hasConfiguredBudget || completelyProceeded">
          <v-card-title class="px-2 py-1">
            <v-icon
              :class="statusIconClass"
              :color="statusIconColor"
              size="32"
              left>
              {{ statusIcon }}
            </v-icon>
            <span class="text-body font-weight-bold">{{ statusMessage }}</span>
          </v-card-title>
        </template>
        <v-card-title v-else class="px-2 py-1">
          <v-icon
            color="error"
            size="30"
            left>
            fas fa-times
          </v-icon>
          <span class="text-body font-weight-bold">{{ $t('wallet.administration.rewardCard.status.setYourBudget') }}</span>
        </v-card-title>
      </template>
      <v-card-actions class="mt-auto">
        <v-list-item class="px-0">
          <div class="d-flex row no-gutters">
            <div class="col-12 col-sm-4 mb-2">
              <v-icon
                color="primary"
                class="mr-1"
                size="20">
                fas fa-users
              </v-icon>
              <span class="subheading mr-2">{{ participants }}</span>
            </div>
            <div class="col-12 col-sm-8 mb-2">
              <v-icon
                color="primary"
                class="mr-1"
                size="20">
                fas fa-trophy
              </v-icon>
              <span class="subheading">{{ participationsCount }} {{ $t('wallet.administration.rewardCard.label.contributions') }}</span>
            </div>
            <div class="col-12 col-sm-4">
              <v-icon
                color="primary"
                class="mr-1"
                size="20">
                fas fa-user-check
              </v-icon>
              <span class="subheading mr-2">{{ eligibleContributorsCount }}</span>
            </div>
            <div class="col-12 col-sm-8">
              <v-icon
                color="primary"
                class="mr-1"
                size="20">
                fas fa-coins
              </v-icon>
              <span class="subheading">{{ budget }}</span>
            </div>
          </div>
        </v-list-item>
      </v-card-actions>
    </v-card>
  </v-card>
</template>

<script>
export default {
  props: {
    loading: {
      type: Boolean,
      default: false,
    },
    rewardSettings: {
      type: Object,
      default: null
    },
    rewardReport: {
      type: Object,
      default: null
    }
  },
  data: () => ({
    currentTimeInSeconds: Date.now() / 1000,
    lang: eXo.env.portal.language,
    dateFormat: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    },
  }),
  computed: {
    completelyProceeded() {
      return this.rewardReport?.completelyProceeded;
    },
    isNotPastPeriod() {
      return !this.period || this.period.endDateInSeconds > this.currentTimeInSeconds;
    },
    isValid() {
      return this.isNotPastPeriod || (!this.isNotPastPeriod && this.eligibleContributorsCount > 0);
    },
    status() {
      if (this.isNotPastPeriod) {
        return {
          icon: 'fas fa-hourglass-half',
          iconClass: 'secondary--text',
          iconColor: null,
          message: this.$t('wallet.administration.rewardCard.status.inPeriod'),
        };
      }
      if (!this.completelyProceeded) {
        return {
          icon: 'fas fa-exclamation-triangle',
          iconClass: null,
          iconColor: 'orange',
          message: this.$t('wallet.administration.rewardCard.status.toReward'),
        };
      }
      if (!this.isValid) {
        return {
          icon: 'fas fa-exclamation-triangle',
          iconClass: null,
          iconColor: 'orange',
          message: this.$t('wallet.administration.rewardCard.status.noParticipant'),
        };
      }
      return {
        icon: 'fas fa-check-circle',
        iconClass: null,
        iconColor: 'success',
        message: this.$t('wallet.administration.rewardCard.status.rewardsSent', {0: this.rewardSentDate}),
      };
    },
    statusIcon() {
      return this.status.icon;
    },
    statusIconClass() {
      return this.status.iconClass;
    },
    statusIconColor() {
      return this.status.iconColor;
    },
    statusMessage() {
      return this.status.message;
    },
    hasConfiguredBudget() {
      return this.rewardSettings?.storedSetting;
    },
    eligibleContributorsCount() {
      return this.hasConfiguredBudget ? this.rewardReport?.validRewards?.length : this.participants;
    },
    walletRewards() {
      return this.rewardReport?.rewards;
    },
    participants() {
      return this.rewardReport?.rewards?.length;
    },
    participationsCount() {
      return this.rewardReport?.participationsCount < 1000 ? this.rewardReport?.participationsCount : `${(this.rewardReport?.participationsCount / 1000).toFixed(1)}K`;
    },
    tokensToSend() {
      return this.rewardReport?.tokensToSend;
    },
    tokensSent() {
      return this.rewardReport?.tokensSent;
    },
    formatedTokensToSend() {
      return Number.isFinite(Number(this.tokensToSend)) ? Math.trunc(this.tokensToSend) : 0;
    },
    formatedTokensSent() {
      return Number.isFinite(Number(this.tokensSent)) ? Math.round(this.tokensSent) : 0;
    },
    budget() {
      return this.completelyProceeded ? `${this.formatedTokensSent} MEED` : (this.hasConfiguredBudget ? `${this.formatedTokensToSend} MEED` : '-');
    },
    period() {
      return this.rewardReport?.period;
    },
    rangeDateTimeTitle() {
      return `${this.starDateFormat} ${this.$t('exoplatform.wallet.label.to')} ${this.toDateFormat}`;
    },
    starDateFormat() {
      return this.startDate?.toLocaleString(this.lang, this.dateFormat);
    },
    toDateFormat() {
      return this.endDate?.toLocaleString(this.lang, this.dateFormat);
    },
    startDate() {
      return new Date(this.period?.startDate);
    },
    endDate() {
      return new Date(this.period?.endDate);
    },
    rewardSentDate() {
      const reward = this.rewardReport?.rewards?.find(reward => reward?.transaction?.succeeded);
      const sentDate = new Date(reward?.transaction?.timestamp);
      return sentDate?.toLocaleString(this.lang, this.dateFormat);
    },
  },
  methods: {
    openDetails() {
      this.$emit('openDetails', this.rewardReport);
    },
  },
};
</script>