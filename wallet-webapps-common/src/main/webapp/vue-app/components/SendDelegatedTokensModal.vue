<template>
  <v-dialog
    id="sendDelegatedTokenModal"
    v-model="dialog"
    :disabled="disabled"
    attach="#walletDialogsParent"
    content-class="uiPopup with-overflow"
    class="sendDelegatedTokenModal"
    width="600px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <v-bottom-nav
      v-if="useNavigation"
      slot="activator"
      :disabled="disabled"
      :value="true"
      color="white"
      class="elevation-0 buttomNavigation">
      <v-btn
        :disabled="disabled"
        flat
        value="send">
        <span>
          Send delegated tokens
        </span>
        <v-icon>
          fa-share-square
        </v-icon>
      </v-btn>
    </v-bottom-nav>

    <button
      v-else-if="!noButton"
      slot="activator"
      :disabled="disabled"
      :dark="!disabled"
      class="btn btn-primary mt-1 mb-1">
      Send delegated tokens
    </button>

    <qr-code-modal
      ref="qrCodeModal"
      :to="recipient"
      :is-contract="true"
      :function-payable="false"
      :args-names="['_from', '_to', '_value']"
      :args-types="['address', 'address', 'uint256']"
      :args-values="[from, recipient, amount]"
      :open="showQRCodeModal"
      title="Send delegated Token QR Code"
      information="You can scan this QR code by using a different application that supports QR code transaction generation to send delegated tokens"
      function-name="transferFrom"
      @close="showQRCodeModal = false" />
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
            Send delegated Tokens
          </span>
      </div>
      <v-card-title v-show="loading" class="pb-0">
        <v-spacer />
        <v-progress-circular
          color="primary"
          indeterminate
          size="20" />
        <v-spacer />
      </v-card-title>
      <v-card-text>
        <div v-if="error && !loading" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div> <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
          <i class="uiIconWarning"></i>{{ warning }}
        </div>
        <v-form
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <address-auto-complete
            ref="autocompleteFrom"
            :disabled="loading"
            input-label="From"
            input-placeholder="Select from which wallet you received a delegated tokens"
            :autofocus="dialog"
            @item-selected="from = $event.address" />
          <address-auto-complete
            ref="autocompleteRecipient"
            :disabled="loading"
            class="mt-4"
            input-label="Recipient"
            input-placeholder="Select a recipient"
            @item-selected="recipient = $event.address" />
          <v-text-field
            v-model.number="amount"
            :disabled="loading"
            name="amount"
            label="Amount"
            placeholder="Select an amount to send to recipient"
            class="mt-4" />
          <v-text-field
            v-if="!storedPassword"
            v-model="walletPassword"
            :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
            :type="walletPasswordShow ? 'text' : 'password'"
            :disabled="loading"
            name="walletPassword"
            label="Wallet password"
            placeholder="Enter your wallet password"
            counter
            autocomplete="current-passord"
            @click:append="walletPasswordShow = !walletPasswordShow" />
          <gas-price-choice @changed="gasPrice = $event" />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button
          :disabled="loading || !recipient || !amount || !from"
          :loading="loading"
          class="btn btn-primary mr-1"
          @click="sendTokens">
          Send
        </button> <button
          :disabled="loading || !recipient || !amount || !from"
          class="btn"
          color="secondary"
          @click="showQRCodeModal = true">
          QRCode
        </button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import AddressAutoComplete from './AddressAutoComplete.vue';
import QrCodeModal from './QRCodeModal.vue';
import GasPriceChoice from './GasPriceChoice.vue';

import {setDraggable, unlockBrowserWallet, lockBrowserWallet, truncateError, hashCode, convertTokenAmountToSend} from '../js/WalletUtils.js';
import {saveTransactionDetails} from '../js/TransactionUtils.js';

export default {
  components: {
    QrCodeModal,
    GasPriceChoice,
    AddressAutoComplete,
  },
  props: {
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
    noButton: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isReadonly: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    useNavigation: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    open: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    hasDelegatedTokens: {
      type: Boolean,
      default: function() {
        return true;
      },
    },
  },
  data() {
    return {
      showQRCodeModal: false,
      storedPassword: false,
      walletPassword: '',
      walletPasswordShow: false,
      useMetamask: false,
      loading: false,
      from: null,
      recipient: null,
      amount: null,
      gasPrice: 0,
      dialog: null,
      warning: null,
      error: null,
    };
  },
  computed: {
    etherBalance() {
      return this.contractDetails && this.contractDetails.etherBalance;
    },
    disabled() {
      return this.isReadonly || !this.hasDelegatedTokens || !this.etherBalance || this.etherBalance === 0 || (typeof this.etherBalance === 'string' && (!this.etherBalance.length || this.etherBalance.trim() === '0'));
    },
  },
  watch: {
    open() {
      this.dialog = this.open;
      if (this.open) {
        this.$nextTick(() => {
          setDraggable();
        });
      }
    },
    dialog() {
      if (this.dialog) {
        this.$refs.autocompleteFrom.clear();
        this.$refs.autocompleteRecipient.clear();
        this.showQRCodeModal = false;
        this.from = null;
        this.recipient = null;
        this.amount = null;
        this.warning = null;
        this.error = null;
        this.walletPassword = '';
        this.walletPasswordShow = false;
        if (!this.gasPrice) {
          this.gasPrice = window.walletSettings.minGasPrice;
        }
        this.useMetamask = window.walletSettings.userPreferences.useMetamask;
        this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
      } else {
        this.$emit('close');
      }
      this.loading = false;
    },
  },
  methods: {
    sendTokens() {
      this.error = null;
      this.warning = null;
      if (!window.localWeb3.utils.isAddress(this.from)) {
        this.error = 'Invalid from address';
        return;
      }

      if (!window.localWeb3.utils.isAddress(this.recipient)) {
        this.error = 'Invalid recipient address';
        return;
      }

      if (!this.amount || isNaN(parseFloat(this.amount)) || !isFinite(this.amount) || this.amount <= 0) {
        this.error = 'Invalid amount';
        return;
      }

      if (!this.storedPassword && (!this.walletPassword || !this.walletPassword.length)) {
        this.error = 'Password field is mandatory';
        return;
      }

      const unlocked = this.useMetamask || unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = 'Wrong password';
        return;
      }

      this.loading = true;
      try {
        return this.contractDetails.contract.methods
          .transferFrom(this.from, this.recipient, convertTokenAmountToSend(this.amount, this.contractDetails.decimals))
          .estimateGas({
            from: this.contractDetails.contract.options.from,
            gas: window.walletSettings.userPreferences.defaultGas,
            gasPrice: this.gasPrice,
          })
          .then((result) => {
            if (result > window.walletSettings.userPreferences.defaultGas) {
              this.warning = `You have set a low gas ${window.walletSettings.userPreferences.defaultGas} while the estimation of necessary gas is ${result}. Please change it in your preferences.`;
              return;
            }
            this.$emit('loading');
            return this.contractDetails.contract.methods
              .transferFrom(this.from, this.recipient, convertTokenAmountToSend(this.amount, this.contractDetails.decimals))
              .send({
                from: this.contractDetails.contract.options.from,
                gas: window.walletSettings.userPreferences.defaultGas,
                gasPrice: this.gasPrice,
              })
              .on('transactionHash', (hash) => {
                const gas = window.walletSettings.userPreferences.defaultGas ? window.walletSettings.userPreferences.defaultGas : 35000;

               const pendingTransaction = {
                  hash: hash,
                  from: this.from,
                  to: this.recipient,
                  by: this.walletAddress,
                  value: 0,
                  gas: gas,
                  gasPrice: this.gasPrice,
                  contractAddress: this.contractDetails.address,
                  contractMethodName: 'transferFrom',
                  contractAmount: this.amount,
                  pending: true,
                  timestamp: Date.now(),
                };

                saveTransactionDetails(pendingTransaction)
                  .then(() => {
                    // The transaction has been hashed and will be sent
                    this.$emit(
                      'sent',
                      pendingTransaction,
                      this.contractDetails
                    );
                    this.dialog = false;
                  });
              })
              .on('error', (error, receipt) => {
                console.debug('Web3 contract.transferFrom method - error', error);
                this.loading = false;
                this.error = `Error sending delegated tokens: ${truncateError(error)}`;
                // Display error on main screen only when dialog is not opened
                if (!this.dialog) {
                  this.$emit('error', this.error);
                }
              });
          })
          .catch((e) => {
            console.debug('Web3 contract.transferFrom method - error', e);
            this.loading = false;
            this.error = `Error sending delegated tokens: ${truncateError(e)}`;
          })
          .finally(() => this.useMetamask || lockBrowserWallet());
      } catch (e) {
        console.debug('Web3 contract.transferFrom method - error', e);
        this.loading = false;
        this.error = `Error sending delegated tokens: ${truncateError(e)}`;
      }
    },
  },
};
</script>
