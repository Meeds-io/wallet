<template>
  <v-card id="sendEtherForm">
    <v-card-text class="pt-0">
      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <v-form
        ref="form"
        @submit="
          $event.preventDefault();
          $event.stopPropagation();
        ">
        <address-auto-complete
          ref="autocomplete"
          :disabled="loading || disabledRecipient"
          :input-label="$t('exoplatform.wallet.label.recipient')"
          :input-placeholder="$t('exoplatform.wallet.label.recipientPlaceholder')"
          :title="$t('exoplatform.wallet.label.recipientPlaceholder')"
          autofocus
          required
          ignore-current-user
          @item-selected="
            recipient = $event.address;
            $emit('receiver-selected', $event);
          " />

        <v-text-field
          ref="amountInput"
          v-model.number="amount"
          :disabled="loading"
          :label="$t('exoplatform.wallet.label.etherAmount')"
          :placeholder="$t('exoplatform.wallet.label.etherAmountPlaceholder')"
          name="amount"
          required
          class="mt-3"
          @input="$emit('amount-selected', amount)" />

        <v-text-field
          v-if="!storedPassword"
          v-model="walletPassword"
          :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
          :type="walletPasswordShow ? 'text' : 'password'"
          :disabled="loading"
          :rules="mandatoryRule"
          :label="$t('exoplatform.wallet.label.walletPassword')"
          :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
          name="walletPassword"
          counter
          required
          autocomplete="current-passord"
          class="mt-3"
          @click:append="walletPasswordShow = !walletPasswordShow" />
        <gas-price-choice @changed="gasPrice = $event" />
        <v-text-field
          v-model="transactionLabel"
          :disabled="loading"
          :label="$t('exoplatform.wallet.label.transactionLabel')"
          :placeholder="$t('exoplatform.wallet.label.transactionLabelPlaceholder')"
          type="text"
          name="transactionLabel" />
        <v-textarea
          v-model="transactionMessage"
          :disabled="loading"
          :label="$t('exoplatform.wallet.label.transactionMessage')"
          :placeholder="$t('exoplatform.wallet.label.transactionMessagePlaceholder')"
          name="etherTransactionMessage"
          class="mt-4"
          rows="3"
          flat
          no-resize />
      </v-form>

      <qr-code-modal
        :from="account"
        :to="recipient"
        :amount="amount"
        :open="showQRCodeModal"
        :title="$t('exoplatform.wallet.title.sendEtherQRCode')"
        :information="$t('exoplatform.wallet.message.sendEtherQRCodeMessage')"
        @close="showQRCodeModal = false" />
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <button
        :disabled="disabled"
        :loading="loading"
        class="btn btn-primary mr-1"
        @click="sendEther">
        {{ $t('exoplatform.wallet.button.send') }}
      </button> <button
        :disabled="disabled"
        class="btn"
        color="secondary"
        @click="showQRCodeModal = true">
        {{ $t('exoplatform.wallet.button.qrCode') }}
      </button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import AddressAutoComplete from './AddressAutoComplete.vue';
import QrCodeModal from './QRCodeModal.vue';
import GasPriceChoice from './GasPriceChoice.vue';

import {unlockBrowserWallet, lockBrowserWallet, hashCode, truncateError} from '../js/WalletUtils.js';
import {saveTransactionDetails} from '../js/TransactionUtils.js';

export default {
  components: {
    QrCodeModal,
    GasPriceChoice,
    AddressAutoComplete,
  },
  props: {
    account: {
      type: String,
      default: function() {
        return null;
      },
    },
    balance: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    defaultLabel: {
      type: String,
      default: function() {
        return null;
      },
    },
    defaultMessage: {
      type: String,
      default: function() {
        return null;
      },
    },
    disabledRecipient: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      showQRCodeModal: false,
      storedPassword: false,
      transactionLabel: '',
      transactionMessage: '',
      transactionHash: null,
      walletPassword: '',
      walletPasswordShow: false,
      loading: false,
      recipient: null,
      amount: null,
      gasPrice: 0,
      error: null,
      mandatoryRule: [(v) => !!v || this.$t('exoplatform.wallet.warning.requiredField')],
    };
  },
  computed: {
    disabled() {
      return !this.account || this.loading || !this.recipient || !this.amount;
    }
  },
  watch: {
    amount() {
      if (this.amount && $.isNumeric(this.amount)) {
        this.error = this.balance >= this.amount ? null : this.$t('exoplatform.wallet.warning.unsufficientFunds');
      } else {
        this.error = null;
      }
    },
  },
  methods: {
    init(recipient) {
      this.loading = false;
      this.recipient = null;
      this.amount = null;
      this.error = null;
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.transactionLabel = this.defaultLabel;
      this.transactionMessage = this.defaultMessage;
      this.transactionHash = null;
      if (!this.gasPrice) {
        this.gasPrice = window.walletSettings.network.minGasPrice;
      }
      this.storedPassword = window.walletSettings.storedPassword && window.walletSettings.browserWalletExists;

      this.$nextTick(() => {
        if (this.$refs.autocomplete) {
          this.$refs.autocomplete.clear();
          this.$nextTick(() => {
            if (recipient) {
              this.$refs.autocomplete.selectItem(recipient);
              this.$refs.amountInput.focus();
            } else {
              this.$refs.autocomplete.focus();
            }
          });
        }
      });
    },
    sendEther() {
      this.error = null;
      if (!this.$refs.form.validate()) {
        return;
      }
      if (!window.localWeb3.utils.isAddress(this.recipient)) {
        this.error = this.$t('exoplatform.wallet.warning.invalidReciepientAddress');
        return;
      }

      if (!this.amount || isNaN(parseInt(this.amount)) || !isFinite(this.amount) || this.amount <= 0) {
        this.error = this.$t('exoplatform.wallet.warning.invalidAmount');
        return;
      }

      if (!this.storedPassword && (!this.walletPassword || !this.walletPassword.length)) {
        this.error = this.$t('exoplatform.wallet.warning.requiredPassword');
        return;
      }

      const gas = window.walletSettings.network.gasLimit ? window.walletSettings.network.gasLimit : 35000;
      if (this.amount >= this.balance) {
        this.error = this.$t('exoplatform.wallet.warning.unsufficientFunds');
        return;
      }

      const unlocked = unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = this.$t('exoplatform.wallet.warning.wrongPassword');
        return;
      }

      this.error = null;
      this.loading = true;
      try {
        const amount = window.localWeb3.utils.toWei(this.amount.toString(), 'ether');
        const sender = this.account;
        const receiver = this.recipient;
        const transaction = {
          from: sender,
          to: receiver,
          value: amount,
          gas: gas,
          gasPrice: this.gasPrice,
        };
        // Send an amount of ether to a third person
        return window.localWeb3.eth
          .sendTransaction(transaction)
          .on('transactionHash', (hash) => {
            this.transactionHash = hash;

            const pendingTransaction = {
              hash: hash,
              from: transaction.from,
              to: transaction.to,
              value: this.amount,
              gas: transaction.gas,
              gasPrice: transaction.gasPrice,
              pending: true,
              label: this.transactionLabel,
              message: this.transactionMessage,
              type: 'sendEther',
              timestamp: Date.now(),
            };

            // *async* save transaction message for contract, sender and receiver
            saveTransactionDetails(pendingTransaction)
              .then(() => {
                // The transaction has been hashed and will be sent
                this.$emit(
                  'sent',
                  pendingTransaction
                );
                this.$emit('close');
              });
          })
          .on('error', (error, receipt) => {
            // The transaction has failed
            this.error = truncateError(`Error sending ether: ${error}`);
            this.loading = false;
            // Display error on main screen only when dialog is not opened
            if (!this.dialog) {
              this.$emit('error', this.error);
            }
          })
          .then(() => this.$emit('success', this.transactionHash));
      } catch (e) {
        console.debug('Web3.eth.sendTransaction method - error', e);
        this.loading = false;
        this.error = truncateError(`Error sending ether: ${e}`);
      } finally {
        lockBrowserWallet();
      }
    },
  },
};
</script>
