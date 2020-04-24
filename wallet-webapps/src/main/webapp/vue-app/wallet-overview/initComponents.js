import WalletOverview from './components/WalletOverview.vue';
import WalletOverviewDrawer from './components/WalletOverviewDrawer.vue';
import WalletOverviewRewardItem from './components/WalletOverviewRewardItem.vue';
import WalletOverviewRewardPluginItem from './components/WalletOverviewRewardPluginItem.vue';

const components = {
  'wallet-overview': WalletOverview,
  'wallet-overview-drawer': WalletOverviewDrawer,
  'wallet-overview-reward-item': WalletOverviewRewardItem,
  'wallet-overview-reward-plugin-item': WalletOverviewRewardPluginItem,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
