<template>
  <v-app
    id="RewardApp"
    color="transaprent"
    class="VuetifyApp"
    flat>
    <main>
      <v-layout column>
        <v-flex>
          <v-card class="applicationToolbar mb-3" flat>
            <v-card-text class="pt-2 pb-2">
              <strong>{{ $t('exoplatform.wallet.title.rewardAdministration') }}</strong>
            </v-card-text>
          </v-card>
        </v-flex>
        <v-layout column class="white">
          <v-flex v-if="error && !loading" class="text-center">
            <div class="alert alert-error text-left">
              <i class="uiIconError"></i>{{ error }}
            </div>
          </v-flex>
          <v-flex v-if="settingWarnings && settingWarnings.length && !loading" class=" text-center">
            <div class="alert alert-warning text-left">
              <i class="uiIconWarning"></i>
              <span><a href="javascript:void(0);" @click="selectedTab = 2">{{ $t('exoplatform.wallet.label.pleaseCheckRewardConfiguration') }}</a></span>
              <ul>
                <li
                  v-for="warning in settingWarnings"
                  :key="warning"
                  class="pl-2">
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
            attach="#walletDialogsParent"
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

          <v-tabs v-model="selectedTab" grow>
            <v-tabs-slider color="primary" />
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
      
          <v-tabs-items v-model="selectedTab">
            <v-tab-item
              id="SendRewards"
              value="SendRewards"
              eager>
              <send-rewards-tab
                ref="sendRewards"
                :reward-report="rewardReport"
                :admin-wallet="adminWallet"
                :total-rewards="totalRewards"
                :contract-details="contractDetails"
                :period-dates-display="periodDatesDisplay"
                :transaction-etherscan-link="transactionEtherscanLink"
                @dates-changed="refreshRewards($event)"
                @refresh="refreshRewards"
                @error="error = $event" />
            </v-tab-item>
            <v-tab-item
              id="RewardPools"
              value="RewardPools"
              eager>
              <teams-list-tab
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
              <configuration-tab
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
import SendRewardsTab from './SendRewardsTab.vue';
import TeamsListTab from './TeamsListTab.vue';
import ConfigurationTab from './ConfigurationTab.vue';

import {getRewardTeams, getRewardSettings, computeRewards} from '../../js/RewardServices.js';

export default {
  components: {
    SendRewardsTab,
    TeamsListTab,
    ConfigurationTab,
  },
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
    };
  },
  computed: {
    duplicatedWallets() {
      return (this.walletRewards && this.walletRewards.filter(walletReward => walletReward.teams && walletReward.teams.length > 1)) || [];
    },
    period() {
      return this.rewardReport && this.rewardReport.period;
    },
    walletRewards() {
      return (this.rewardReport && this.rewardReport.rewards) || [];
    },
    periodType() {
      return this.rewardSettings && this.rewardSettings.periodType;
    },
    periodDatesDisplay() {
      const selectedStartDate = this.period && this.formatDate(new Date(this.period.startDateInSeconds * 1000));
      const selectedEndDate = this.period && this.formatDate(new Date((this.period.endDateInSeconds -1) * 1000));
      if (selectedStartDate && selectedEndDate) {
        return `${selectedStartDate} ${this.$t('exoplatform.wallet.label.to')} ${selectedEndDate}`;
      } else if (selectedStartDate) {
        return selectedStartDate;
      } else {
        return '';
      }
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
          console.debug('init method - error', e);
          this.error = e ? String(e) : this.$t('exoplatform.wallet.error.unknownError');
        })
        .finally(() => {
          this.loading = false;
        });
    },
    refreshRewardSettings() {
      this.loading = true;
      return getRewardSettings()
        .then(settings => this.rewardSettings = settings || {})
        .then(() => this.$nextTick())
        .then(() => this.$refs.configurationTab.init())
        .then(() => this.refreshTeams())
        .then(() => this.$nextTick())
        .then(() => this.refreshRewards())
        .finally(() => this.loading = false);
    },
    refreshRewards(period) {
      if(!this.checkConfigurationConsistency()) {
        return;
      }

      period = period || this.period;
      const selectedDateInSeconds = period && period.startDateInSeconds;

      this.loading = true;
      return computeRewards(selectedDateInSeconds)
        .then(rewardReport => {
          if(rewardReport.error) {
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
            if(!noTeamMembers) {
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
      return getRewardTeams()
        .then(teams => this.teams = teams || [])
        .catch((e) => {
          console.debug('Error getting teams list', e);
          this.error = this.$t('exoplatform.wallet.error.errorRetrievingPool');
        });
    },
    computeTotalRewardsByPlugin() {
      const totalRewards = {};
      if(this.rewardSettings && this.rewardSettings.pluginSettings && this.rewardSettings.pluginSettings.length) {
        this.rewardSettings.pluginSettings.forEach(pluginSetting => totalRewards[pluginSetting.pluginId] = {pluginId: pluginSetting.pluginId, total: 0})
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

      if(!this.rewardSettings) {
        this.settingWarnings.push(this.$t('exoplatform.wallet.error.emptySettings'));
      } else {
        if(!this.rewardSettings.pluginSettings || !this.rewardSettings.pluginSettings.length) {
          this.settingWarnings.push(this.$t('exoplatform.wallet.warning.noPluginConfiguration'));
        } else {
          this.rewardSettings.pluginSettings.forEach(pluginSetting => {
            if(pluginSetting && !pluginSetting.budgetType) {
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
