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
  'contract-admin-modal': WalletAdminOperationModal,
  'initialize-modal': WalletAdminInitializeModal,
  'upgrade-token-modal': WalletAdminUpgradeTokenModal,
  'contract-tab': WalletAdminContractTab,
  'initial-funds-tab': WalletAdminInitialFundsTab,
  'initialize-account-modal': WalletAdminInitializeAccountModal,
  'send-ether-modal': WalletAdminSendEtherModal,
  'send-token-modal': WalletAdminSendTokenModal,
  'admin-wallet': AdminWallet,
  'wallets-tab': WalletAdminWalletsTab,

};

for (const key in components) {
  Vue.component(key, components[key]);
}