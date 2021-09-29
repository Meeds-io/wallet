import WalletAdminOperationModal from './components/admin/common/WalletAdminOperationModal.vue';
import WalletAdminInitializeModal from './components/admin/contracts/modals/WalletAdminInitializeModal.vue';
import WalletAdminUpgradeTokenModal from './components/admin/contracts/modals/WalletAdminUpgradeTokenModal.vue';
import WalletAdminContractTab from './components/admin/contracts/WalletAdminContractTab.vue';
import WalletAdminInitialFundsTab from './components/admin/settings/WalletAdminInitialFundsTab.vue';
import WalletAdminInitializeAccountModal from './components/admin/wallets/modals/WalletAdminInitializeAccountModal.vue';
import WalletAdminSendEtherModal from './components/admin/wallets/modals/WalletAdminSendEtherModal.vue';
import WalletAdminSendTokenModal from './components/admin/wallets/modals/WalletAdminSendTokenModal.vue';
import AdminWallet from './components/admin/wallets/AdminWallet.vue';
import WalletAdminWalletsTab from './components/admin/wallets/WalletAdminWalletsTab.vue';

const components = {
  'wallet-contract-admin-modal': WalletAdminOperationModal,
  'wallet-initialize-modal': WalletAdminInitializeModal,
  'wallet-upgrade-token-modal': WalletAdminUpgradeTokenModal,
  'wallet-contract-tab': WalletAdminContractTab,
  'wallet-initial-funds-tab': WalletAdminInitialFundsTab,
  'wallet-initialize-account-modal': WalletAdminInitializeAccountModal,
  'wallet-send-ether-modal': WalletAdminSendEtherModal,
  'wallet-send-token-modal': WalletAdminSendTokenModal,
  'wallet-admin-wallet': AdminWallet,
  'wallet-tab': WalletAdminWalletsTab,
};

for (const key in components) {
  Vue.component(key, components[key]);
}