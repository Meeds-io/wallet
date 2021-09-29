import RewardDetailModal from './components/reward/modal/RewardDetailModal.vue';
import ConfigurationTab from './components/reward/ConfigurationTab.vue';
import SendRewardsTab from './components/reward/SendRewardsTab.vue';
import TeamForm from './components/reward/TeamForm.vue';
import TeamsListTab from './components/reward/TeamsListTab.vue';

const components = {
  'reward-detail-modal': RewardDetailModal,
  'configuration-tab': ConfigurationTab,
  'send-rewards-tab': SendRewardsTab,
  'add-team-form': TeamForm,
  'teams-list-tab': TeamsListTab,
};

for (const key in components) {
  Vue.component(key, components[key]);
}