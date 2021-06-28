import ContractAdminModal from './components/admin/common/WalletAdminOperationModal.vue';
import InitializeAccountAdminModal from './components/admin/contracts/modals/WalletAdminInitializeModal.vue';
import UpgradeTokenModal from './components/admin/contracts/modals/WalletAdminUpgradeTokenModal.vue';
import ContractTab from './components/admin/contracts/WalletAdminContractTab.vue';
import InitialFundsTab from './components/admin/settings/WalletAdminInitialFundsTab.vue';
import InitializeAccountModal from './components/admin/wallets/modals/WalletAdminInitializeAccountModal.vue';
import SendEtherModal from './components/admin/wallets/modals/WalletAdminSendEtherModal.vue';
import SendTokenModal from './components/admin/wallets/modals/WalletAdminSendTokenModal.vue';
import AdminWallet from './components/admin/wallets/AdminWallet.vue';
import WalletsTab from './components/admin/wallets/WalletAdminWalletsTab.vue';

const components = {
  'wallet-contract-admin-modal': ContractAdminModal,
  'wallet-initialize-account-admin-modal': InitializeAccountAdminModal,
  'wallet-upgrade-token-modal': UpgradeTokenModal,
  'wallet-contract-tab': ContractTab,
  'wallet-initial-funds-tab': InitialFundsTab,
  'wallet-initialize-account-modal': InitializeAccountModal,
  'wallet-send-ether-modal': SendEtherModal,
  'wallet-admin-wallet': AdminWallet,
  'wallet-wallets-tab': WalletsTab,
  'wallet-send-token-modal': SendTokenModal,
};
for (const key in components) {
  Vue.component(key, components[key]);
}