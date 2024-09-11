import RewardApp from './components/RewardApp.vue';
import RewardManagement from './components/reward/RewardManagement.vue';
import CurrentBalance from './components/reward/CurrentBalance.vue';
import BudgetConfiguration from './components/reward/BudgetConfiguration.vue';
import BudgetConfigurationDrawer from './components/reward/BudgetConfigurationDrawer.vue';
import DistributionForecast from './components/reward/DistributionForecast.vue';
import TimeZoneSelectBox from './components/reward/TimeZoneSelectBox.vue';

const components = {
  'wallet-reward-app': RewardApp,
  'wallet-reward-management': RewardManagement,
  'wallet-current-balance': CurrentBalance,
  'wallet-budget-configuration': BudgetConfiguration,
  'wallet-budget-configuration-drawer': BudgetConfigurationDrawer,
  'wallet-budget-distribution-forecast': DistributionForecast,
  'wallet-reward-timezone-selectbox': TimeZoneSelectBox,
};

for (const key in components) {
  Vue.component(key, components[key]);
}