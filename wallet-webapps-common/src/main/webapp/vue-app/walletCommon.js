import AccountDetail from './components/AccountDetail.vue';
import AddressAutoComplete from './components/AddressAutoComplete.vue';
import ConfirmDialog from './components/ConfirmDialog.vue';
import DelegateTokensModal from './components/DelegateTokensModal.vue';
import GasPriceChoice from './components/GasPriceChoice.vue';
import InformationBubble from './components/InformationBubble.vue';
import WarningBubble from './components/WarningBubble.vue';
import ProfileChip from './components/ProfileChip.vue';
import QRCode from './components/QRCode.vue';
import QRCodeModal from './components/QRCodeModal.vue';
import SendDelegatedTokensModal from './components/SendDelegatedTokensModal.vue';
import SendEtherForm from './components/SendEtherForm.vue';
import SendEtherModal from './components/SendEtherModal.vue';
import SendFundsForm from './components/SendFundsForm.vue';
import SendFundsModal from './components/SendFundsModal.vue';
import SendTokensForm from './components/SendTokensForm.vue';
import SendTokensModal from './components/SendTokensModal.vue';
import TransactionsList from './components/TransactionsList.vue';
import BackupModal from './components/WalletBackupModal.vue';
import BrowserSetup from './components/WalletBrowserSetup.vue';
import ImportKeyModal from './components/WalletImportKeyModal.vue';
import MetamaskSetup from './components/WalletMetamaskSetup.vue';
import ReceiveModal from './components/WalletReceiveModal.vue';
import ResetModal from './components/WalletResetModal.vue';
import RequestFundsModal from './components/WalletRequestFundsModal.vue';
import WalletAddress from './components/WalletAddress.vue';
import WalletSetup from './components/WalletSetup.vue';
import WalletSummary from './components/WalletSummary.vue';

import * as constants from './js/Constants.js';
import * as addressRegistry from './js/AddressRegistry.js';
import * as walletUtils from './js/WalletUtils.js';
import * as tokenUtils from './js/TokenUtils.js';
import * as transactionUtils from './js/TransactionUtils.js';

// Added to watch changes on less file
import './../css/main.less';

const WalletCommon = {
  install: (Vue, options) => {
    Vue.prototype.constants = constants;
    Vue.prototype.addressRegistry = addressRegistry;
    Vue.prototype.walletUtils = walletUtils;
    Vue.prototype.tokenUtils = tokenUtils;
    Vue.prototype.transactionUtils = transactionUtils;

    Vue.component('account-detail', AccountDetail);
    Vue.component('address-auto-complete', AddressAutoComplete);
    Vue.component('confirm-dialog', ConfirmDialog);
    Vue.component('delegate-tokens-modal', DelegateTokensModal);
    Vue.component('gas-price-choice', GasPriceChoice);
    Vue.component('information-bubble', InformationBubble);
    Vue.component('warning-bubble', WarningBubble);
    Vue.component('profile-chip', ProfileChip);
    Vue.component('qr-code', QRCode);
    Vue.component('qr-code-modal', QRCodeModal);
    Vue.component('send-delegated-tokens-modal', SendDelegatedTokensModal);
    Vue.component('send-ether-form', SendEtherForm);
    Vue.component('send-ether-modal', SendEtherModal);
    Vue.component('send-funds-form', SendFundsForm);
    Vue.component('send-funds-modal', SendFundsModal);
    Vue.component('send-tokens-form', SendTokensForm);
    Vue.component('send-tokens-modal', SendTokensModal);
    Vue.component('transactions-list', TransactionsList);
    Vue.component('backup-modal', BackupModal);
    Vue.component('browser-setup', BrowserSetup);
    Vue.component('import-key-modal', ImportKeyModal);
    Vue.component('metamask-setup', MetamaskSetup);
    Vue.component('receive-modal', ReceiveModal);
    Vue.component('reset-modal', ResetModal);
    Vue.component('request-funds-modal', RequestFundsModal);
    Vue.component('wallet-address', WalletAddress);
    Vue.component('wallet-setup', WalletSetup);
    Vue.component('wallet-summary', WalletSummary);
  },
};
window.WalletCommon = WalletCommon;
