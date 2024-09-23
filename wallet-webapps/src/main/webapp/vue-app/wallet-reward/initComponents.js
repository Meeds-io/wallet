import RewardApp from './components/RewardApp.vue';
import RewardManagement from './components/reward/RewardManagement.vue';
import RewardCard from './components/reward/RewardCard.vue';
import RewardDetails from './components/reward/RewardDetails.vue';
import CurrentBalance from './components/reward/CurrentBalance.vue';
import BudgetConfiguration from './components/reward/BudgetConfiguration.vue';
import BudgetConfigurationDrawer from './components/reward/BudgetConfigurationDrawer.vue';
import DistributionForecast from './components/reward/DistributionForecast.vue';
import TimeZoneSelectBox from './components/reward/TimeZoneSelectBox.vue';
import RewardCardMaskContent from './components/reward/RewardCardMaskContent.vue';

const components = {
  'wallet-reward-app': RewardApp,
  'wallet-reward-management': RewardManagement,
  'wallet-reward-card': RewardCard,
  'wallet-reward-details': RewardDetails,
  'wallet-current-balance': CurrentBalance,
  'wallet-budget-configuration': BudgetConfiguration,
  'wallet-budget-configuration-drawer': BudgetConfigurationDrawer,
  'wallet-budget-distribution-forecast': DistributionForecast,
  'wallet-reward-timezone-selectbox': TimeZoneSelectBox,
  'wallet-reward-card-mask-content': RewardCardMaskContent,
};

for (const key in components) {
  Vue.component(key, components[key]);
}