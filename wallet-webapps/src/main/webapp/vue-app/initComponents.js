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
  'wallet-reward-detail': RewardDetail,
  'wallet-settings-modal': SettingsModal,
  'wallet-settings-security-tab': SettingsSecurityTab,
  'wallet-summary': WalletSummary,
  'wallet-summary-balance': SummaryBalance,
  'wallet-summary-buttons': SummaryButtons,
  'wallet-summary-reward': SummaryReward,
  'wallet-summary-transaction': SummaryTransaction,
  'wallet-toolbar-menu': ToolbarMenu,
  'wallet-transaction-history-chart': TransactionHistoryChart,
  'wallet-balance': WalletBalance,
  'wallet-summary-estimation': SummaryEstimation,
  'wallet-summary-pool': SummaryPool
};

for (const key in components) {
  Vue.component(key, components[key]);
}