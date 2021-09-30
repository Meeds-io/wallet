import SettingsModal from './components/wallet-app/SettingsModal.vue';
import SettingsSecurityTab from './components/wallet-app/SettingsSecurityTab.vue';
import Summary from './components/wallet-app/Summary.vue';
import SummaryBalance from './components/wallet-app/SummaryBalance.vue';
import SummaryButtons from './components/wallet-app/SummaryButtons.vue';
import SummaryTransaction from './components/wallet-app/SummaryTransaction.vue';
import ToolbarMenu from './components/wallet-app/ToolbarMenu.vue';
import TransactionHistoryChart from './components/wallet-app/TransactionHistoryChart.vue';
import TransactionHistoryChartSummary from './components/wallet-app/TransactionHistoryChartSummary.vue';

const components = {
  'wallet-reward-settings-modal': SettingsModal,
  'wallet-reward-settings-security-tab': SettingsSecurityTab,
  'wallet-reward-summary': Summary,
  'wallet-reward-summary-balance': SummaryBalance,
  'wallet-reward-summary-buttons': SummaryButtons,
  'wallet-reward-summary-transaction': SummaryTransaction,
  'wallet-reward-toolbar-menu': ToolbarMenu,
  'wallet-reward-transaction-history-chart': TransactionHistoryChart,
  'wallet-reward-transaction-history-chart-summary': TransactionHistoryChartSummary,
};

for (const key in components) {
  Vue.component(key, components[key]);
}