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
  <exo-drawer
    ref="drawer"
    v-model="drawer"
    :loading="loading"
    fixed
    right
    @closed="close">
    <template #title>
      {{ $t('wallet.administration.rewardDetails.label') }}
    </template>
    <template v-if="drawer" #content>
      <div class="pa-5">
        <v-list-item
          :id="id"
          class="px-0"
          dense>
          <div v-if="rank" class="me-3">
            <v-avatar
              color="secondary white--text"
              class="border-color my-auto"
              size="32">
              {{ rank }}
            </v-avatar>
          </div>
          <v-list-item-avatar size="32" class="me-4 my-auto">
            <v-img
              :lazy-src="userAvatar"
              :src="userAvatar"
              transition="none"
              eager />
          </v-list-item-avatar>
          <v-list-item-content>
            <v-list-item-title>
              <user-avatar
                v-if="identity"
                :identity="identity"
                :size="25"
                fullname
                extra-class="me-0 pa-0 my-0 text-truncate-2"
                popover-left-position
                offset-x />
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action v-if="amount" class="justify-end">
            <span class="primary--text font-weight-bold"><span class="fundsLabels"> {{ tokenSymbol }} </span>{{ walletUtils.toFixed(amount) }}</span>
          </v-list-item-action>
        </v-list-item>
        <template>
          <div class="d-flex align-center mb-2 mt-5">
            <div class="text-header me-2">
              {{ rangeDateTimeTitle }}
            </div>
            <v-divider />
          </div>
        </template>
        <v-list-item
          v-for="program in programs"
          :key="program.id"
          class="px-0">
          <v-list-item-avatar class="border-radius">
            <v-img
              :lazy-src="program.avatarUrl"
              :src="program.avatarUrl"
              transition="none"
              eager />
          </v-list-item-avatar>
          <v-list-item-content>
            <v-list-item-title v-text="program.label" />
            <v-list-item-subtitle> {{ program.value }}  {{ $t('wallet.administration.rewardDetails.label.points') }}</v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action class="me-2">
            {{ tokenSymbol }} {{ program.amount }}
          </v-list-item-action>
        </v-list-item>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  props: {
    walletReward: {
      type: Object,
      default: null,
    },
    tokenSymbol: {
      type: String,
      default: ''
    },
  },
  data: () => ({
    drawer: false,
    loading: false,
    identity: null,
    programs: [],
    lang: eXo.env.portal.language,
    dateFormat: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    },
  }),
  computed: {
    rangeDateTimeTitle() {
      return `${this.startDateFormat} ${this.$t('exoplatform.wallet.label.to')} ${this.endDateFormat}`;
    },
    period() {
      return this.walletReward?.period;
    },
    periodMedianDateInSeconds() {
      return this.period?.periodMedianDateInSeconds;
    },
    rewardPeriodType() {
      return this.period?.rewardPeriodType;
    },
    endDateInSeconds() {
      return this.period?.endDateInSeconds;
    },
    startDateInSeconds() {
      return this.period?.startDateInSeconds;
    },
    startDateFormat() {
      return new window.Intl.DateTimeFormat(this.lang, this.dateFormat).format(new Date(this.startDateInSeconds * 1000 - new Date().getTimezoneOffset() * 60 * 1000));
    },
    endDateFormat() {
      return new window.Intl.DateTimeFormat(this.lang, this.dateFormat)
        .format(new Date(this.endDateInSeconds * 1000 - 86400 * 1000 - new Date().getTimezoneOffset() * 60 * 1000));
    },
    rank() {
      return this.walletReward?.rank;
    },
    identityId() {
      return this.walletReward?.identityId;
    },
    amount() {
      return this.walletReward?.amount;
    },
    remoteId() {
      return this.identity?.remoteId;
    },
    userAvatar() {
      return this.identity?.avatar;
    },
  },
  methods: {
    async open() {
      this.loading = true;
      this.drawer = true;
      await this.$nextTick();
      try {
        await Promise.all([this.retrieveUserInformations(), this.retrieveStats()]);
      } finally {
        this.loading = false;
      }
    },
    retrieveUserInformations() {
      return this.$identityService.getIdentityById(this.identityId)
        .then(data => {
          this.identity = {
            id: data?.profile?.id,
            deleted: data?.profile?.deleted,
            username: data?.profile?.username,
            fullname: data?.profile?.fullName,
            remoteId: data?.profile?.remoteId,
            avatar: data?.profile?.avatar,
          };
          return this.$nextTick();
        });
    },
    async retrieveStats() {
      const stats = await this.fetchStats();
      this.populatePrograms(stats);
      await this.fetchAvatarsForPrograms();
      this.divideAmountAcrossPrograms();
    },
    async fetchStats() {
      return await this.$leaderboardService.getStats(
        this.identityId,
        this.rewardPeriodType,
        this.periodMedianDateInSeconds
      );
    },
    populatePrograms(stats) {
      this.programs = stats.map(stat => ({
        id: stat.programId,
        label: stat.label,
        value: stat.value
      }));
    },
    async fetchAvatarsForPrograms() {
      const avatarPromises = this.programs.map(program => this.fetchAvatarForProgram(program));
      await Promise.all(avatarPromises);
    },
    async fetchAvatarForProgram(program) {
      const data = await this.$programService.getProgramById(program.id, {
        lang: eXo.env.portal.language
      });
      program.avatarUrl = data?.avatarUrl || '';
    },
    divideAmountAcrossPrograms() {
      const totalProgramValue = this.programs.reduce((sum, program) => sum + program.value, 0);
      if (totalProgramValue === 0) {
        return;
      }
      this.programs = this.programs.map(program => {
        const proportion = program.value / totalProgramValue;
        const programShare = this.amount * proportion;
        return {
          ...program,
          amount: programShare.toFixed(3)
        };
      });
    },
    close() {
      this.drawer = false;
      this.identity = null;
      this.programs = [];
    },
  },
};
</script>