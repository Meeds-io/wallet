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
  'perk-store-contract-admin-modal': ContractAdminModal,
  'perk-store-initialize-account-admin-modal': InitializeAccountAdminModal,
  'perk-store-upgrade-token-modal': UpgradeTokenModal,
  'perk-store-contract-tab': ContractTab,
  'perk-store-initial-funds-tab': InitialFundsTab,
  'perk-store-initialize-account-modal': InitializeAccountModal,
  'perk-store-send-ether-modal': SendEtherModal,
  'perk-store-admin-wallet': AdminWallet,
  'perk-store-wallets-tab': WalletsTab,
  'perk-store-send-token-modal': SendTokenModal,
};
for (const key in components) {
  Vue.component(key, components[key]);
}