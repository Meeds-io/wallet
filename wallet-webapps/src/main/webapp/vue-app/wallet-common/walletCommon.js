/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import AccountDetail from './components/AccountDetail.vue';
import AddressAutoComplete from './components/AddressAutoComplete.vue';
import ConfirmDialog from './components/ConfirmDialog.vue';
import InformationBubble from './components/InformationBubble.vue';
import WarningBubble from './components/WarningBubble.vue';
import ProfileChip from './components/ProfileChip.vue';
import QRCode from './components/QRCode.vue';
import QRCodeModal from './components/QRCodeModal.vue';
import SendTokensForm from './components/SendTokensForm.vue';
import SendTokensModal from './components/SendTokensModal.vue';
import TransactionsList from './components/TransactionsList.vue';
import BackupModal from './components/WalletBackupModal.vue';
import BrowserSetup from './components/WalletBrowserSetup.vue';
import ImportKeyDrawer from './components/WalletImportKeyDrawer.vue';
import WalletPasswordManagement from './components/WalletPasswordManagement.vue';
import RequestFundsModal from './components/WalletRequestFundsModal.vue';
import WalletAddress from './components/WalletAddress.vue';
import WalletSetup from './components/WalletSetup.vue';
import BackupDrawer from './components/WalletBackupDrawer.vue';
import WalletSettingsDetails from './wallet-settings/components/WalletSettingsDetails.vue';
import WalletSettingsInternal from './wallet-settings/components/WalletSettingsInternal.vue';
import WalletSettingsInternalDetails from './wallet-settings/components/WalletSettingsInternalDetails.vue';
import WalletSettingsMetamask from './wallet-settings/components/WalletSettingsMetamask.vue';
import WalletSettingsJdenticon from './wallet-settings/components/jdenticon.vue';
import WalletSettings from './wallet-settings/components/WalletSettings.vue';
import WalletNotificationAlert from './components/WalletNotificationAlert.vue';
import MetamaskWarnings from './components/MetamaskWarnings.vue';
import WalletWelcomeScreen from './components/WalletWelcomeScreen.vue';
import WalletOverview from './wallet-overview/components/WalletOverview.vue';
import WalletOverviewDrawer from './wallet-overview/components/WalletOverviewDrawer.vue';
import WalletOverviewRewardItem from './wallet-overview/components/WalletOverviewRewardItem.vue';
import WalletOverviewRewardPluginItem from './wallet-overview/components/WalletOverviewRewardPluginItem.vue';
import WalletUserBalance from './wallet-user-balance/components/WalletUserBalance.vue';
import WeeklyEarnings from './weekly-earnings/components/WeeklyEarnings.vue';

import * as constants from './js/Constants.js';
import * as addressRegistry from './js/AddressRegistry.js';
import * as walletUtils from './js/WalletUtils.js';
import * as tokenUtils from './js/TokenUtils.js';
import * as transactionUtils from './js/TransactionUtils.js';
import * as rewardService from './js/RewardService.js';

const WalletCommon = {
  install: (Vue) => {
    Vue.prototype.constants = constants;
    Vue.prototype.addressRegistry = addressRegistry;
    Vue.prototype.walletUtils = walletUtils;
    Vue.prototype.tokenUtils = tokenUtils;
    Vue.prototype.transactionUtils = transactionUtils;
  },
};

if (!Vue.prototype.$rewardService) {
  window.Object.defineProperty(Vue.prototype, '$rewardService', {
    value: rewardService,
  });
}

export const components = {
  'wallet-reward-account-detail': AccountDetail,
  'wallet-reward-address-auto-complete': AddressAutoComplete,
  'wallet-reward-confirm-dialog': ConfirmDialog,
  'wallet-reward-information-bubble': InformationBubble,
  'wallet-reward-warning-bubble': WarningBubble,
  'wallet-reward-profile-chip': ProfileChip,
  'wallet-reward-qr-code': QRCode,
  'wallet-reward-qr-code-modal': QRCodeModal,
  'wallet-reward-send-tokens-form': SendTokensForm,
  'wallet-reward-send-tokens-modal': SendTokensModal,
  'wallet-reward-transactions-list': TransactionsList,
  'wallet-reward-backup-modal': BackupModal,
  'wallet-reward-browser-setup': BrowserSetup,
  'wallet-reward-import-key-drawer': ImportKeyDrawer,
  'wallet-reward-password-management': WalletPasswordManagement,
  'wallet-reward-request-funds-modal': RequestFundsModal,
  'wallet-reward-address': WalletAddress,
  'wallet-reward-setup': WalletSetup,
  'wallet-reward-backup-drawer': BackupDrawer,
  'wallet-settings': WalletSettings,
  'wallet-settings-details': WalletSettingsDetails,
  'wallet-settings-internal': WalletSettingsInternal,
  'wallet-settings-internal-details': WalletSettingsInternalDetails,
  'wallet-settings-metamask': WalletSettingsMetamask,
  'wallet-settings-jdenticon': WalletSettingsJdenticon,
  'wallet-welcome-screen': WalletWelcomeScreen,
  'wallet-notification-alert': WalletNotificationAlert,
  'wallet-metamask-warnings': MetamaskWarnings,
  'wallet-overview': WalletOverview,
  'wallet-overview-drawer': WalletOverviewDrawer,
  'wallet-overview-reward-item': WalletOverviewRewardItem,
  'wallet-overview-reward-plugin-item': WalletOverviewRewardPluginItem,
  'wallet-user-balance': WalletUserBalance,
  'wallet-reward-weekly-earnings': WeeklyEarnings,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
window.WalletCommon = WalletCommon;

if (extensionRegistry) {
  extensionRegistry.registerComponent('SpaceSettings', 'space-settings-components', {
    id: 'wallet-space-settings',
    vueComponent: Vue.options.components['wallet-settings'],
    rank: 10,
    isEnabled: (params) => {
      return params.spaceApplications != null && params.spaceApplications.some( item => {
        return item.id === 'SpaceWallet';
      });
    },
  });
  extensionRegistry.registerComponent('my-rewards-overview', 'my-rewards-item', {
    id: 'wallet-rewards-overview',
    vueComponent: Vue.options.components['wallet-reward-weekly-earnings'],
    rank: 10,
  }); 
  extensionRegistry.registerComponent('my-rewards-wallet-overview', 'my-rewards-wallet-item', {
    id: 'wallet-balance-overview',
    vueComponent: Vue.options.components['wallet-user-balance'],
    rank: 20,
  });
}
