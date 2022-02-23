import WalletApp from './components/WalletApp.vue';
import Summary from './components/wallet-app/Summary.vue';
import SummaryBalance from './components/wallet-app/SummaryBalance.vue';
import SummaryButtons from './components/wallet-app/SummaryButtons.vue';
import SummaryTransaction from './components/wallet-app/SummaryTransaction.vue';
import ToolbarMenu from './components/wallet-app/ToolbarMenu.vue';
import TransactionHistoryChart from './components/wallet-app/TransactionHistoryChart.vue';
import TransactionHistoryChartSummary from './components/wallet-app/TransactionHistoryChartSummary.vue';

Vue.use(WalletCommon);

const components = {
  'wallet-app': WalletApp,
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
