import RewardDetailModal from './components/reward/modal/RewardDetailModal.vue';
import ConfigurationTab from './components/reward/ConfigurationTab.vue';
import SendRewardsTab from './components/reward/SendRewardsTab.vue';
import AddTeamForm from './components/reward/TeamForm.vue';
import TeamsListTab from './components/reward/TeamsListTab.vue';

const components = {
  'wallet-reward-detail-modal': RewardDetailModal,
  'wallet-configuration-tab': ConfigurationTab,
  'wallet-send-rewards-tab': SendRewardsTab,
  'wallet-add-team-form': AddTeamForm,
  'wallet-teams-list-tab': TeamsListTab,
};
for (const key in components) {
  Vue.component(key, components[key]);
}