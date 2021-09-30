import RewardDetailModal from './components/reward/modal/RewardDetailModal.vue';
import ConfigurationTab from './components/reward/ConfigurationTab.vue';
import SendRewardsTab from './components/reward/SendRewardsTab.vue';
import TeamForm from './components/reward/TeamForm.vue';
import TeamsListTab from './components/reward/TeamsListTab.vue';

const components = {
  'wallet-reward-reward-detail-modal': RewardDetailModal,
  'wallet-reward-configuration-tab': ConfigurationTab,
  'wallet-reward-send-rewards-tab': SendRewardsTab,
  'wallet-reward-add-team-form': TeamForm,
  'wallet-reward-teams-list-tab': TeamsListTab,
};

for (const key in components) {
  Vue.component(key, components[key]);
}