import WalletAdminInitialFundsTab from './components/admin/settings/WalletAdminInitialFundsTab.vue';
import WalletAdminSendEtherModal from './components/admin/wallets/modals/WalletAdminSendEtherModal.vue';
import WalletAdminSendTokenModal from './components/admin/wallets/modals/WalletAdminSendTokenModal.vue';
import AdminWallet from './components/admin/wallets/AdminWallet.vue';
import WalletAdminWalletsTab from './components/admin/wallets/WalletAdminWalletsTab.vue';

const components = {
  'wallet-initial-funds-tab': WalletAdminInitialFundsTab,
  'wallet-send-ether-modal': WalletAdminSendEtherModal,
  'wallet-send-token-modal': WalletAdminSendTokenModal,
  'wallet-admin-wallet': AdminWallet,
  'wallet-tab': WalletAdminWalletsTab,
};

for (const key in components) {
  Vue.component(key, components[key]);
}