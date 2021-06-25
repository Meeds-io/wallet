import RewardDetail from './components/wallet-app/RewardDetail.vue';
import SettingsModal from './components/wallet-app/SettingsModal.vue';
import SettingsSecurityTab from './components/wallet-app/SettingsSecurityTab.vue';
import WalletSummary from './components/wallet-app/Summary.vue';
import SummaryBalance from './components/wallet-app/SummaryBalance.vue';
import SummaryButtons from './components/wallet-app/SummaryButtons.vue';
import SummaryReward from './components/wallet-app/SummaryReward.vue';
import SummaryTransaction from './components/wallet-app/SummaryTransaction.vue';
import SummaryEstimation from './components/wallet-app/SummaryEstimation.vue';
import SummaryPool from './components/wallet-app/SummaryPool.vue';
import ToolbarMenu from './components/wallet-app/ToolbarMenu.vue';
import TransactionHistoryChart from './components/wallet-app/TransactionHistoryChart.vue';
import WalletBalance from './components/wallet-balance/WalletBalance.vue';

const components = {
  'perk-store-reward-detail': RewardDetail,
  'perk-store-settings-modal': SettingsModal,
  'perk-store-settings-security-tab': SettingsSecurityTab,
  'perk-store-wallet-summary': WalletSummary,
  'perk-store-summary-balance': SummaryBalance,
  'perk-store-summary-buttons': SummaryButtons,
  'perk-store-summary-reward': SummaryReward,
  'perk-store-summary-transaction': SummaryTransaction,
  'perk-store-toolbar-menu': ToolbarMenu,
  'perk-store-transaction-history-chart': TransactionHistoryChart,
  'perk-store-transaction-history-chart-summary': TransactionHistoryChartSummary,
  'perk-store-wallet-balance': WalletBalance,
  'perk-store-summary-estimation': SummaryEstimation,
  'perk-store-summary-pool': SummaryPool
};

for (const key in components) {
  Vue.component(key, components[key]);
}