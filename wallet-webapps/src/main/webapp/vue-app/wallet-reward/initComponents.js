import RewardApp from './components/RewardApp.vue';
import RewardManagement from './components/reward/RewardManagement.vue';
import CurrentBalance from './components/reward/CurrentBalance.vue';
import BudgetConfiguration from './components/reward/BudgetConfiguration.vue';
import RewardDetailModal from './components/reward/modal/RewardDetailModal.vue';
import TimeZoneSelectBox from './components/reward/TimeZoneSelectBox.vue';
import ConfigurationTab from './components/reward/ConfigurationTab.vue';
import SendRewardsTab from './components/reward/SendRewardsTab.vue';
import TeamForm from './components/reward/TeamForm.vue';
import TeamsListTab from './components/reward/TeamsListTab.vue';

const components = {
  'wallet-reward-app': RewardApp,
  'wallet-reward-management': RewardManagement,
  'wallet-current-balance': CurrentBalance,
  'wallet-budget-configuration': BudgetConfiguration,
  'wallet-reward-reward-detail-modal': RewardDetailModal,
  'wallet-reward-timezone-selectbox': TimeZoneSelectBox,
  'wallet-reward-configuration-tab': ConfigurationTab,
  'wallet-reward-send-rewards-tab': SendRewardsTab,
  'wallet-reward-add-team-form': TeamForm,
  'wallet-reward-teams-list-tab': TeamsListTab,
};

for (const key in components) {
  Vue.component(key, components[key]);
}