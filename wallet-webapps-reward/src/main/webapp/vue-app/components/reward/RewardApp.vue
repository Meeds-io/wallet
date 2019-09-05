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
                  <code>{{ duplicatedWallet.name }}</code>
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
                :wallet-rewards="walletRewards"
                :contract-details="contractDetails"
                :period-type="rewardSettings.periodType"
                :transaction-etherscan-link="transactionEtherscanLink"
                :total-budget="totalBudget"
                :sent-budget="sentBudget"
                :eligible-users-count="eligibleUsersCount"
                :total-rewards="totalRewards"
                @dates-changed="refreshRewardSettings"
                @refresh="refreshRewards"
                @error="error = $event" />
            </v-tab-item>
            <v-tab-item
              id="RewardPools"
              value="RewardPools"
              eager>
              <teams-list-tab
                ref="rewardTeams"
                :wallet-rewards="walletRewards"
                :contract-details="contractDetails"
                :period="periodDatesDisplay"
                :eligible-pools-users-count="eligiblePoolsUsersCount"
                @teams-refreshed="refreshRewardSettings"
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

import {getRewardSettings, computeRewards} from '../../js/RewardServices.js';

export default {
  components: {
    SendRewardsTab,
    TeamsListTab,
    ConfigurationTab,
  },
  data() {
    return {
      loading: false,
      wallet: null,
      error: null,
      settingWarnings: [],
      selectedTab: 'SendRewards',
      transactionEtherscanLink: null,
      addressEtherscanLink: null,
      contractDetails: null,
      periodDatesDisplay: null,
      periodType: null,
      duplicatedWallets: [],
      rewardSettings: {},
      totalRewards: [],
      teams: [],
      walletRewards: [],
    };
  },
  computed: {
    validUsers() {
      return this.walletRewards.filter(wallet => wallet.enabled && wallet.tokensToSend);
    },
    eligiblePoolsUsersCount() {
      return this.validUsers.filter(wallet =>  !wallet.disabledPool && wallet.poolTokensToSend).length;
    },
    eligibleUsersCount() {
      return this.validUsers.filter(wallet =>  wallet.tokensToSend || wallet.tokensSent).length;
    },
    sentBudget() {
      if (this.walletRewards && this.walletRewards.length) {
        let sentTokens = 0;
        this.walletRewards.forEach((wallet) => {
          sentTokens += (Number(wallet.tokensSent) || 0);
        });
        return sentTokens;
      } else {
        return 0;
      }
    },
    totalBudget() {
      if (this.walletRewards && this.walletRewards.length) {
        let totalBudget = 0;
        this.walletRewards.forEach((wallet) => {
          totalBudget += (Number(wallet.tokensSent) || Number(wallet.tokensToSend) || 0);
        });
        return totalBudget;
      } else {
        return 0;
      }
    },
  },
  created() {
    this.init()
      .then(() => {
        this.transactionEtherscanLink = this.walletUtils.getTransactionEtherscanlink();
        this.addressEtherscanLink = this.walletUtils.getAddressEtherscanlink();
        this.walletUtils.setDraggable('RewardApp');
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
          this.wallet = window.walletSettings.wallet;
          this.contractDetails = window.walletSettings.contractDetail;
        })
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
        .then(settings => {
          this.rewardSettings = settings || {};
          this.periodType = this.rewardSettings.periodType;
          this.$refs.configurationTab.init();
          return this.$nextTick();
        })
        .then(() => this.$refs.rewardTeams.refreshTeams())
        .then(() => this.$refs.sendRewards.refreshDates())
        .then(() => this.$nextTick())
        .then(() => this.refreshRewards())
        .finally(() => this.loading = false);
    },
    refreshRewards() {
      this.loading = true;
      this.periodDatesDisplay = this.$refs.sendRewards.periodDatesDisplay;
      const teams = this.$refs.rewardTeams.teams || [];
      this.duplicatedWallets = [];
      if(!this.checkConfigurationConsistency()) {
        return;
      }

      return computeRewards(this.$refs.sendRewards.selectedDateInSeconds)
        .then(walletRewards => {
          if(walletRewards.error) {
            this.error = (typeof walletRewards.error === 'object' ? walletRewards.error[0] : walletRewards.error);
            return;
          }
          this.walletRewards = walletRewards;
          this.computeTotalRewardsByPlugin();

          this.walletRewards.forEach(walletReward => {
            if (walletReward && walletReward.rewardTransaction && walletReward.rewardTransaction.hash && walletReward.rewardTransaction.status === 'pending') {
              this.walletUtils.watchTransactionStatus(walletReward.rewardTransaction.hash, (transactionDetail) => {
                walletReward.rewardTransaction.status = transactionDetail.succeeded ? 'success' : 'error';
              });
            }
          });

          teams.forEach((team) => {
            team.validMembersWallets = [];
            team.computedBudget = 0;

            if (team.id && team.members) {
              team.members.forEach((memberObject) => {
                const walletReward = this.walletRewards.find((walletReward) => walletReward.wallet && walletReward.wallet.id && walletReward.wallet.technicalId === memberObject.identityId);
                if (walletReward) {
                  if (walletReward.rewardTeams && walletReward.rewardTeams.length) {
                    walletReward.rewardTeams.push(team);
                    this.duplicatedWallets.push(walletReward.wallet);
                  } else {
                    this.$set(walletReward, 'rewardTeams', [team]);
                  }
                  if(walletReward.enabled && walletReward.poolTokensToSend) {
                    team.validMembersWallets.push(walletReward);
                  }
                }
              });
            }
          });

          const membersWithEmptyTeam = this.walletRewards.filter((walletReward) => !walletReward.rewardTeams || !walletReward.rewardTeams.length);
          if (membersWithEmptyTeam && membersWithEmptyTeam.length) {
            const validMembersWallets = [];
            membersWithEmptyTeam.forEach(walletReward => {
              if(walletReward.enabled && walletReward.poolTokensToSend) {
                validMembersWallets.push(walletReward);
              }
            });

            // Members with no Team
            let noTeamMembers = teams.find(team => !team.id);
            if(!noTeamMembers) {
              noTeamMembers = {
                id: 0,
                name: this.$t('exoplatform.wallet.label.noPoolUsers'),
                description: this.$t('exoplatform.wallet.label.noPoolUsersDescription'),
                rewardType: 'COMPUTED',
                computedBudget: 0,
                noTeam: true,
              };
              teams.push(noTeamMembers);
            }
            noTeamMembers.members = membersWithEmptyTeam.map(walletReward => walletReward.wallet);
            noTeamMembers.validMembersWallets = validMembersWallets;
          }

          if (teams && teams.length) {
            teams.sort((team1, team2) => Number(team1.id) - Number(team2.id));
          }

          teams.forEach((team) => {
            if (team.validMembersWallets && team.validMembersWallets.length) {
              team.computedBudget = team.validMembersWallets.reduce((total, walletReward) => total += walletReward.poolTokensToSend || 0, 0);
            }
          });
        })
        .finally(() => this.loading = false);
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

      if (!this.contractDetails) {
        this.settingWarnings.push(this.$t('exoplatform.wallet.warning.noConfiguredToken'));
      }

      if (!this.periodType) {
        this.settingWarnings.push(this.$t('exoplatform.wallet.warning.noConfiguredToken'));
        this.settingWarnings.push(this.$t('exoplatform.wallet.warning.missingRewardPeriodicity'));
      }

      if(this.settingWarnings.length) {
        return false;
      }
      return true;
    },
  },
};
</script>
