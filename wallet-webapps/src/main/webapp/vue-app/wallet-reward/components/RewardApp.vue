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
    class="application-body position-static"
    flat>
    <template v-if="!showRewardDetails">
      <div class="d-flex flex-row">
        <v-card
          class="d-flex flex-column justify-space-between"
          width="40%"
          flat>
          <wallet-current-balance
            :loading="loading"
            :reward-settings="rewardSettings"
            :configured-budget="configuredBudget"
            :admin-wallet="adminWallet"
            :contract-details="contractDetails" />
          <extension-registry-components
            name="WalletRewardingCard"
            type="wallet-reward-cards-extensions" />
        </v-card>
        <wallet-budget-configuration
          :loading="loading"
          :reward-settings="rewardSettings"
          :settings-updated="settingsUpdated"
          @openConfiguration="openBudgetConfigurationDrawer"
          @deleteSetting="deleteRewardSettings" />
      </div>
      <wallet-reward-management
        :loading="loadingRewards"
        :reward-reports="rewardReports"
        :reward-settings="rewardSettings"
        @openDetails="openDetails"
        @loadMore="loadMore" />
      <wallet-budget-configuration-drawer
        :reward-settings="rewardSettings"
        :reward-report="rewardReport"
        @setting-updated="settingUpdated"
        ref="budgetConfiguration" />
    </template>
    <template v-else>
      <wallet-reward-details
        :reward-report="selectedRewardReport"
        :transaction-ether-scan-link="transactionEtherScanLink"
        :contract-details="contractDetails"
        :admin-wallet="adminWallet"
        :configured-budget="configuredBudget"
        @reward-report-updated="rewardReportUpdated"
        @back="showRewardDetails = false" />
    </template>
  </v-app>
</template>

<script>

export default {
  data: () => ({
    rewardSettings: {},
    loading: false,
    loadingRewards: false,
    loadingRewardSettings: false,
    rewardReports: [],
    selectedRewardReport: null,
    showRewardDetails: false,
    rewardsPage: 0,
    rewardsPageSize: 12,
    transactionEtherScanLink: null,
    contractDetails: null,
    distributionForecast: null,
    adminWallet: null,
    settingsUpdated: false,
  }),
  computed: {
    rewardReport() {
      return this.rewardReports[0];
    },
    rewardPeriod() {
      return this.rewardReport?.period;
    },
    selectedDate() {
      return this.rewardPeriod?.startDate.substring(0, 10) || new Date().toISOString().substring(0, 10);
    },
    configuredBudget() {
      return this.distributionForecast?.budget;
    },
  },
  created() {
    this.init()
      .then(() => {
        this.transactionEtherScanLink = this.walletUtils.getTransactionEtherscanlink();
      }).then(this.refreshRewardSettings());
  },
  methods: {
    openBudgetConfigurationDrawer() {
      this.$refs.budgetConfiguration.open();
    },
    init() {
      this.loading = true;
      this.error = null;
      return this.walletUtils.initSettings(false, true, true)
        .then(() => {
          this.contractDetails = window.walletSettings.contractDetail;
          return this.addressRegistry.searchWalletByTypeAndId('admin', 'admin');
        })
        .then((adminWallet) => this.adminWallet = adminWallet)
        .then(() => this.walletUtils.initWeb3(false, true))
        .then(() => {
          if (this.contractDetails) {
            return this.tokenUtils.reloadContractDetails(this.walletAddress).then(result => this.contractDetails = result);
          } else {
            this.contractDetails = this.tokenUtils.getContractDetails(this.walletAddress);
          }
        })
        .catch((e) => {
          this.error = e ? String(e) : this.$t('exoplatform.wallet.error.unknownError');
        })
        .finally(() => {
          this.loading = false;
        });
    },
    refreshRewardSettings() {
      return this.$rewardService.getRewardSettings()
        .then(settings => {
          this.rewardSettings = settings || {};
        })
        .then(() => this.refreshRewards())
        .then(() => this.computeDistributionForecast());
    },
    refreshRewards(period) {
      if (period) {
        this.period = period;
      }
      this.loadingRewards = true;
      return this.$rewardService.computeRewards(this.rewardsPage, this.rewardsPageSize)
        .then(rewardReports => {
          this.rewardReports.push(...rewardReports);
          return this.$nextTick();
        }).finally(() => this.loadingRewards = false);
    },
    deleteRewardSettings() {
      return this.$rewardService.deleteRewardSettings()
        .then(() => {
          this.rewardReports = [];
          return this.refreshRewardSettings();
        });
    },
    openDetails(rewardReport) {
      this.showRewardDetails = true;
      this.selectedRewardReport = rewardReport;
    },
    loadMore() {
      this.rewardsPage += 1;
      this.refreshRewards();
    },
    settingUpdated() {
      this.settingsUpdated = true;
      this.rewardReports = [];
      this.refreshRewardSettings().then(() => this.settingsUpdated = false);
    },
    rewardReportUpdated(rewardReport) {
      const index = this.rewardReports.findIndex(r => r?.period === rewardReport?.period);
      if (index !== -1) {
        this.rewardReports[index] = rewardReport;
      }
    },
    computeDistributionForecast() {
      return this.$rewardService.computeDistributionForecast(this.rewardSettings)
        .then(distributionForecast => {
          this.distributionForecast = distributionForecast;
        });
    },
  },
};
</script>
