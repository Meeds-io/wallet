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
  <v-app
    id="RewardApp"
    flat>
    <div class="d-flex flex-row">
      <v-card
        class="d-flex flex-column justify-space-between"
        width="40%"
        flat>
        <wallet-current-balance />
      </v-card>
      <wallet-budget-configuration
        :loading="loading"
        :reward-settings="rewardSettings"
        :reward-report="rewardReport"
        @openConfiguration="openBudgetConfigurationDrawer" />
    </div>
    <wallet-reward-management />
    <wallet-budget-configuration-drawer
      :reward-settings="rewardSettings"
      @setting-updated="refreshRewardSettings"
      ref="budgetConfiguration" />
  </v-app>
</template>

<script>

export default {
  data: () => ({
    rewardSettings: {},
    loading: false,
    rewardReport: null,
  }),
  computed: {
    rewardPeriod() {
      return this.rewardReport?.period;
    },
    selectedDate() {
      return this.rewardPeriod?.startDate.substring(0, 10) || new Date().toISOString().substring(0, 10);
    },
  },
  created() {
    this.refreshRewardSettings();
  },
  methods: {
    openBudgetConfigurationDrawer() {
      this.$refs.budgetConfiguration.open();
    },
    refreshRewardSettings() {
      this.loading = true;
      return this.$rewardService.getRewardSettings()
        .then(settings => {
          this.rewardSettings = settings || {};
        })
        .then(() => this.refreshRewards())
        .finally(() => this.loading = false);
    },
    refreshRewards(period) {
      if (period) {
        this.period = period;
      }
      this.loading = true;
      return this.$rewardService.computeRewards(this.selectedDate)
        .then(rewardReport => {
          this.rewardReport = rewardReport;
          return this.$nextTick();
        })
        .finally(() => this.loading = false);
    },
  },
};
</script>
