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
import GasPriceChoice from './components/GasPriceChoice.vue';
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
import ImportKeyModal from './components/WalletImportKeyModal.vue';
import ResetModal from './components/WalletResetModal.vue';
import RequestFundsModal from './components/WalletRequestFundsModal.vue';
import WalletAddress from './components/WalletAddress.vue';
import WalletSetup from './components/WalletSetup.vue';

import * as constants from './js/Constants.js';
import * as addressRegistry from './js/AddressRegistry.js';
import * as walletUtils from './js/WalletUtils.js';
import * as tokenUtils from './js/TokenUtils.js';
import * as transactionUtils from './js/TransactionUtils.js';

const WalletCommon = {
  install: (Vue) => {
    Vue.prototype.constants = constants;
    Vue.prototype.addressRegistry = addressRegistry;
    Vue.prototype.walletUtils = walletUtils;
    Vue.prototype.tokenUtils = tokenUtils;
    Vue.prototype.transactionUtils = transactionUtils;
  },
};
const components = {
  'account-detail': AccountDetail,
  'address-auto-complete': AddressAutoComplete,
  'confirm-dialog': ConfirmDialog,
  'gas-price-choice': GasPriceChoice,
  'information-bubble': InformationBubble,
  'warning-bubble': WarningBubble,
  'profile-chip': ProfileChip,
  'qr-code': QRCode,
  'qr-code-modal': QRCodeModal,
  'send-tokens-form': SendTokensForm,
  'send-tokens-modal': SendTokensModal,
  'transactions-list': TransactionsList,
  'backup-modal': BackupModal,
  'browser-setup': BrowserSetup,
  'import-key-modal': ImportKeyModal,
  'reset-modal': ResetModal,
  'request-funds-modal': RequestFundsModal,
  'wallet-address': WalletAddress,
  'wallet-setup': WalletSetup,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
window.WalletCommon = WalletCommon;