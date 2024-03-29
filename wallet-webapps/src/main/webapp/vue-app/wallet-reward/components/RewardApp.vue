<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2022 Meeds Association
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
  <v-app
    id="RewardApp"
    color="transaprent"
    class="VuetifyApp"
    flat>
    <main>
      <v-layout column>
        <v-layout column class="application-toolbar">
          <v-flex v-if="error && !loading" class="text-center">
            <div class="alert alert-error text-start">
              <i class="uiIconError"></i>{{ error }}
            </div>
          </v-flex>
          <v-flex v-if="settingWarnings && settingWarnings.length && !loading" class=" text-center">
            <div class="alert alert-warning text-start">
              <i class="uiIconWarning"></i>
              <span><a href="javascript:void(0);" @click="selectedTab = 2">{{ $t('exoplatform.wallet.label.pleaseCheckRewardConfiguration') }}</a></span>
              <ul>
                <li
                  v-for="warning in settingWarnings"
                  :key="warning"
                  class="ps-2">
                  - {{ warning }}
                </li>
              </ul>
            </div>
          </v-flex>

          <v-flex v-if="duplicatedWallets && duplicatedWallets.length" class="text-center">
            <div class="alert alert-warning">
              <i class="uiIconWarning"></i> {{ $t('exoplatform.wallet.warning.poolMemberDuplicated') }}:
              <ul>
                <li v-for="duplicatedWallet in duplicatedWallets" :key="duplicatedWallet.id">
                  <code>{{ duplicatedWallet.wallet.name }}</code>
                </li>
              </ul>
            </div>
          </v-flex>

          <v-dialog
            v-model="loading"
            persistent
            hide-overlay
            width="300">
            <v-card color="primary" dark>
              <v-card-text>
                {{ $t('exoplatform.wallet.label.loading') }} ...
                <v-progress-linear
                  indeterminate
                  color="white"
                  class="mb-0" />
              </v-card-text>
            </v-card>
          </v-dialog>

          <v-tabs
            v-model="selectedTab"
            class="card-border-radius overflow-hidden"
            slider-size="4">
            <v-tab key="SendRewards" href="#SendRewards">
              {{ $t('exoplatform.wallet.title.sendRewardsTab') }}
            </v-tab>
            <v-tab key="RewardPools" href="#RewardPools">
              {{ $t('exoplatform.wallet.title.rewardPoolsTab') }}
            </v-tab>
            <v-tab key="Configuration" href="#Configuration">
              {{ $t('exoplatform.wallet.title.rewardConfigurationTab') }}
            </v-tab>
          </v-tabs>
      
          <v-tabs-items v-model="selectedTab" class="tabs-content card-border-radius overflow-hidden">
            <v-tab-item
              id="SendRewards"
              value="SendRewards"
              eager>
              <wallet-reward-send-rewards-tab
                ref="sendRewards"
                :reward-report="rewardReport"
                :admin-wallet="adminWallet"
                :total-rewards="totalRewards"
                :contract-details="contractDetails"
                :period-dates-display="periodDatesDisplay"
                :transaction-etherscan-link="transactionEtherscanLink"
                :time-zone="timeZone"
                @dates-changed="refreshRewards($event)"
                @refresh="refreshRewards"
                @error="error = $event" />
            </v-tab-item>
            <v-tab-item
              id="RewardPools"
              value="RewardPools"
              eager>
              <wallet-reward-teams-list-tab
                ref="rewardTeams"
                :teams="teams"
                :wallet-rewards="walletRewards"
                :period="period"
                :period-dates-display="periodDatesDisplay"
                :contract-details="contractDetails"
                @refresh-teams="refreshRewardSettings"
                @refresh="refreshRewardSettings"
                @error="error = $event" />
            </v-tab-item>
            <v-tab-item
              id="Configuration"
              value="Configuration"
              eager>
              <wallet-reward-configuration-tab
                ref="configurationTab"
                @saved="refreshRewardSettings"
                @error="error = $event" />
            </v-tab-item>
          </v-tabs-items>
        </v-layout>
      </v-layout>
      <div id="walletDialogsParent">
      </div>
    </main>
  </v-app>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      error: null,
      rewardReport: null,
      adminWallet: null,
      settingWarnings: [],
      selectedTab: 'SendRewards',
      transactionEtherscanLink: null,
      addressEtherscanLink: null,
      contractDetails: null,
      rewardSettings: {},
      totalRewards: [],
      teams: [],
      period: null,
    };
  },
  computed: {
    duplicatedWallets() {
      return (this.walletRewards && this.walletRewards.filter(walletReward => walletReward.teams && walletReward.teams.length > 1)) || [];
    },
    walletRewards() {
      return (this.rewardReport && this.rewardReport.rewards) || [];
    },
    periodType() {
      return this.rewardSettings && this.rewardSettings.periodType;
    },
    timeZone() {
      return this.period?.timeZone || this.rewardSettings?.timeZone;
    },
    selectedDate() {
      return this.period?.startDate.substring(0, 10) || new Date().toISOString().substring(0, 10);
    },
    dateformat() {
      return this.timeZone && {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        timeZoneName: 'short',
        timeZone: this.timeZone,
      } || {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      };
    },
    periodDatesDisplay() {
      if (!this.period) {
        return '';
      }
      const startDateFormatted = this.$dateUtil.formatDateObjectToDisplay(new Date(this.period.startDateInSeconds * 1000), this.dateformat, eXo.env.portal.language);
      const endDateFormatted = this.$dateUtil.formatDateObjectToDisplay(new Date(this.period.endDateInSeconds * 1000 - 1), this.dateformat, eXo.env.portal.language);
      return `${startDateFormatted} ${this.$t('exoplatform.wallet.label.to')} ${endDateFormatted}`;
    },
  },
  watch: {
    rewardReport() {
      this.period = this.rewardReport?.period;
    },
  },
  created() {
    this.init()
      .then(() => {
        this.transactionEtherscanLink = this.walletUtils.getTransactionEtherscanlink();
        this.addressEtherscanLink = this.walletUtils.getAddressEtherscanlink();
      });
  },
  methods: {
    init() {
      this.loading = true;

      this.error = null;
      return this.walletUtils.initSettings(false, true)
        .then(() => {
          if (!window.walletSettings) {
            throw new Error(this.$t('exoplatform.wallet.error.emptySettings'));
          }
          this.contractDetails = window.walletSettings.contractDetail;

          return this.addressRegistry.searchWalletByTypeAndId('admin', 'admin');
        })
        .then((adminWallet) => this.adminWallet = adminWallet)
        .then(() => this.refreshRewardSettings())
        .catch((e) => {
          console.error  ('init method - error', e);
          this.error = e ? String(e) : this.$t('exoplatform.wallet.error.unknownError');
        })
        .finally(() => {
          this.loading = false;
        });
    },
    refreshRewardSettings() {
      this.loading = true;
      return this.$rewardService.getRewardSettings()
        .then(settings => this.rewardSettings = settings || {})
        .then(() => this.$nextTick())
        .then(() => this.$refs.configurationTab.init())
        .then(() => this.refreshTeams())
        .then(() => this.$nextTick())
        .then(() => this.refreshRewards())
        .finally(() => this.loading = false);
    },
    refreshRewards(period) {
      if (!this.checkConfigurationConsistency()) {
        return;
      }

      if (period) {
        this.period = period;
      }

      this.loading = true;
      return this.$rewardService.computeRewards(this.selectedDate)
        .then(rewardReport => {
          if (rewardReport.error) {
            this.error = (typeof rewardReport.error === 'object' ? rewardReport.error[0] : rewardReport.error);
            return;
          }
          this.rewardReport = rewardReport;
          return this.$nextTick();
        })
        .then(() => {
          this.computeTotalRewardsByPlugin();

          // Watch pending transactions
          this.walletRewards.forEach(walletReward => {
            if (walletReward.status === 'pending') {
              this.walletUtils.watchTransactionStatus(walletReward.transaction.hash, (transactionDetail) => {
                walletReward.status = transactionDetail.succeeded ? 'success' : 'error';
              });
            }
          });

          // compute valid members per team
          this.teams.forEach((team) => {
            team.validMembersWallets = [];
            team.computedBudget = 0;

            if (team.id && team.members) {
              team.members.forEach((memberObject) => {
                const walletReward = this.walletRewards.find((walletReward) => walletReward.wallet && walletReward.wallet.id && walletReward.wallet.technicalId === memberObject.identityId);
                if (walletReward && walletReward.enabled && walletReward.poolTokensToSend) {
                  team.validMembersWallets.push(walletReward);
                }
              });
            }
          });

          // Build 'No Team Members' team
          const membersWithEmptyTeam = this.walletRewards.filter((walletReward) => !walletReward.team);
          if (membersWithEmptyTeam && membersWithEmptyTeam.length) {
            // Members with no Team
            let noTeamMembers = this.teams.find(team => !team.id);
            if (!noTeamMembers) {
              noTeamMembers = {
                id: 0,
                name: this.$t('exoplatform.wallet.label.noPoolUsers'),
                description: this.$t('exoplatform.wallet.label.noPoolUsersDescription'),
                rewardType: 'COMPUTED',
                computedBudget: 0,
                noTeam: true,
              };
              this.teams.push(noTeamMembers);
            }
            noTeamMembers.members = membersWithEmptyTeam.map(walletReward => walletReward.wallet);
            noTeamMembers.validMembersWallets = membersWithEmptyTeam.filter(walletReward => walletReward.enabled && walletReward.poolTokensToSend);
          }

          // Sort teams by order of creation
          if (this.teams && this.teams.length) {
            this.teams.sort((team1, team2) => Number(team1.id) - Number(team2.id));
          }

          // Compute Team total budgets
          this.teams
            .filter(team => team.validMembersWallets && team.validMembersWallets.length)
            .forEach(team => {
              team.computedBudget = team.validMembersWallets.reduce((total, walletReward) => total += walletReward.poolTokensToSend || 0, 0);
            });
        })
        .finally(() => this.loading = false);
    },
    refreshTeams() {
      return this.$rewardService.getRewardTeams()
        .then(teams => this.teams = teams || [])
        .catch((e) => {
          console.error  ('Error getting teams list', e);
          this.error = this.$t('exoplatform.wallet.error.errorRetrievingPool');
        });
    },
    computeTotalRewardsByPlugin() {
      const totalRewards = {};
      if (this.rewardSettings && this.rewardSettings.pluginSettings && this.rewardSettings.pluginSettings.length) {
        this.rewardSettings.pluginSettings.forEach(pluginSetting => totalRewards[pluginSetting.pluginId] = {pluginId: pluginSetting.pluginId, total: 0});
      }

      this.walletRewards.forEach(walletReward => {
        if (walletReward && walletReward.rewards) {
          walletReward.rewards.forEach(rewardDetail => totalRewards[rewardDetail.pluginId] && (totalRewards[rewardDetail.pluginId].total += rewardDetail.points));
        }
      });

      this.totalRewards = Object.values(totalRewards);
    },
    checkConfigurationConsistency() {
      this.settingWarnings = [];

      if (!this.contractDetails) {
        this.settingWarnings.push(this.$t('exoplatform.wallet.warning.noConfiguredToken'));
      } else if (!this.periodType) {
        this.settingWarnings.push(this.$t('exoplatform.wallet.warning.noConfiguredToken'));
        this.settingWarnings.push(this.$t('exoplatform.wallet.warning.missingRewardPeriodicity'));
      }

      if (!this.rewardSettings) {
        this.settingWarnings.push(this.$t('exoplatform.wallet.error.emptySettings'));
      } else {
        if (!this.rewardSettings.pluginSettings || !this.rewardSettings.pluginSettings.length) {
          this.settingWarnings.push(this.$t('exoplatform.wallet.warning.noPluginConfiguration'));
        } else {
          this.rewardSettings.pluginSettings.forEach(pluginSetting => {
            if (pluginSetting && !pluginSetting.budgetType) {
              this.settingWarnings.push(this.$t('exoplatform.wallet.warning.noRewardBudgetConfiguredForPlugin', {0: pluginSetting.pluginId}));
            }
          });
        }
      }

      return !this.settingWarnings.length;
    },
    formatDate(date) {
      if (!date) {
        return null;
      }
      return date.toLocaleDateString(eXo.env.portal.language);
    },
  },
};
</script>
