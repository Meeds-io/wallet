<template>
  <v-card id="sendTokenForm" flat>
    <v-card-text class="pt-0">
      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div> <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
        <i class="uiIconWarning"></i>{{ warning }}
      </div> <div v-if="!error && !warning && information && information.length" class="alert alert-info v-content">
        <i class="uiIconInfo"></i>{{ information }}
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
          :ignore-current-user="!isSpace"
          autofocus
          required
          validate-on-blur
          @item-selected="
            recipient = $event.address;
            $emit('receiver-selected', $event);
          " />

        <v-text-field
          v-model.number="amount"
          :disabled="loading"
          :label="$t('exoplatform.wallet.label.amount')"
          :placeholder="$t('exoplatform.wallet.label.amountPlaceholder')"
          name="amount"
          required
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
          @click:append="walletPasswordShow = !walletPasswordShow" />
        <gas-price-choice
          :wallet="wallet"
          :estimated-fee="transactionFeeString"
          @changed="gasPrice = $event" />
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
          name="tokenTransactionMessage"
          rows="3"
          flat
          no-resize />
      </v-form>
      <qr-code-modal
        ref="qrCodeModal"
        :to="recipient"
        :from="walletAddress"
        :amount="0"
        :is-contract="true"
        :args-names="['_to', '_value']"
        :args-types="['address', 'uint256']"
        :args-values="[recipient, amount]"
        :open="showQRCodeModal"
        :function-payable="false"
        :title="$t('exoplatform.wallet.title.sendTokenQRCode')"
        :information="$t('exoplatform.wallet.message.sendTokenQRCodeMessage')"
        function-name="transfer"
        @close="showQRCodeModal = false" />
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <button
        :disabled="disabled"
        :loading="loading"
        class="ignore-vuetify-classes btn btn-primary mr-1"
        @click="sendTokens">
        {{ $t('exoplatform.wallet.button.send') }}
      </button>
      <button
        :disabled="disabled"
        class="ignore-vuetify-classes btn"
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

import {unlockBrowserWallet, lockBrowserWallet, truncateError, hashCode, toFixed, convertTokenAmountToSend, etherToFiat, markFundRequestAsSent} from '../js/WalletUtils.js';
import {sendContractTransaction} from '../js/TokenUtils.js';
import {searchWalletByAddress} from '../js/AddressRegistry.js';

export default {
  components: {
    QrCodeModal,
    GasPriceChoice,
    AddressAutoComplete,
  },
  props: {
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return null;
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
  },
  data() {
    return {
      loading: false,
      showQRCodeModal: false,
      storedPassword: false,
      transactionLabel: '',
      transactionMessage: '',
      walletPassword: '',
      walletPasswordShow: false,
      recipient: null,
      notificationId: null,
      isApprovedRecipient: true,
      disabledRecipient: false,
      canSendToken: true,
      amount: null,
      isSpace: false,
      gasPrice: 0,
      estimatedGas: 0,
      fiatSymbol: null,
      warning: null,
      information: null,
      error: null,
      mandatoryRule: [(v) => !!v || this.$t('exoplatform.wallet.warning.requiredField')],
    };
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    disabled() {
      return !this.walletAddress || this.loading || !this.gasPrice || !this.recipient || !this.amount || !this.canSendToken || (!this.storedPassword && (!this.walletPassword || !this.walletPassword.trim().length));
    },
    transactionFeeString() {
      if (this.transactionFeeToken) {
        if (this.contractDetails) {
          return `${toFixed(this.transactionFeeToken)} ${this.contractDetails && this.contractDetails.symbol}`;
        } else {
          return '';
        }
      } else if (this.transactionFeeFiat) {
        return `${toFixed(this.transactionFeeFiat)} ${this.fiatSymbol}`;
      }
      return '';
    },
    sellPriceInWei() {
      return this.contractDetails && this.contractDetails.sellPrice ? window.localWeb3.utils.toWei(String(this.contractDetails.sellPrice), 'ether') : 0;
    },
    transactionFeeInWei() {
      return this.estimatedGas && this.gasPrice ? parseInt(this.estimatedGas * this.gasPrice) : 0;
    },
    transactionFeeEther() {
      return this.transactionFeeInWei ? window.localWeb3.utils.fromWei(String(this.transactionFeeInWei), 'ether') : 0;
    },
    transactionFeeFiat() {
      return this.transactionFeeEther ? etherToFiat(this.transactionFeeEther) : 0;
    },
    transactionFeeToken() {
      return this.contractDetails && (this.contractDetails.isOwner || !this.transactionFeeInWei || !this.sellPriceInWei ? 0 : toFixed(this.transactionFeeInWei / this.sellPriceInWei));
    },
  },
  watch: {
    dialog() {
      if (!this.dialog) {
        this.recipient = null;
        this.amount = null;
        this.notificationId = null;
        this.warning = null;
        this.error = null;
        this.walletPassword = '';
        this.walletPasswordShow = false;
        this.error = null;
      }
    },
    contractDetails() {
      if (this.contractDetails && this.contractDetails.isPaused) {
        this.warning = this.$t('exoplatform.wallet.warning.contractPaused', {0: this.contractDetails.name});
      }
    },
    amount() {
      this.checkErrors();
    },
    recipient(newValue, oldValue) {
      if (oldValue !== newValue) {
        this.error = null;
        this.warning = null;
        this.information = null;

        this.isApprovedRecipient = true;
        this.canSendToken = true;
      }

      if (newValue && oldValue !== newValue) {
        this.checkErrors();
        if (this.error) {
          return;
        }

        if (this.contractDetails.contractType > 0) {
          this.warning = null;
          this.information = null;

          return searchWalletByAddress(this.recipient, true)
            .then((receiverWallet) => {
              // Unknown wallet address
              if (!receiverWallet || !receiverWallet.address || !receiverWallet.id) {
                this.canSendToken = true;
                return;
              }
              this.isApprovedRecipient = receiverWallet.isApproved;
              if (this.contractDetails && this.contractDetails.isPaused) {
                this.warning = this.$t('exoplatform.wallet.warning.contractPaused', {0: this.contractDetails.name});
                this.canSendToken = false;
              } else if (!this.isApprovedRecipient) {
                this.warning = this.$t('exoplatform.wallet.warning.recipientIsDisapproved');
                this.canSendToken = false;
              } else {
                this.canSendToken = true;
              }

              // Async gas estimation if current address is owner of contract
              if (this.canSendToken && this.contractDetails && this.contractDetails.isOwner) {
                this.estimateTransactionFee();
              }
            });
        }
      }
    },
  },
  methods: {
    init() {
      this.$nextTick(() => {
        if (this.$refs.autocomplete) {
          this.$refs.autocomplete.clear();
          this.$refs.autocomplete.focus();
        }
      });
      this.loading = false;
      this.disabledRecipient = false;
      this.showQRCodeModal = false;
      this.isSpace = window.walletSettings && window.walletSettings.wallet && window.walletSettings.wallet.type === 'space';
      this.transactionLabel = this.defaultLabel;
      this.transactionMessage = this.defaultMessage;
      if (!this.gasPrice) {
        this.gasPrice = window.walletSettings.network.normalGasPrice;
      }
      this.fiatSymbol = window.walletSettings.fiatSymbol;
      this.storedPassword = window.walletSettings.storedPassword && window.walletSettings.browserWalletExists;
      this.$nextTick(() => {
        if (this.contractDetails && this.contractDetails.isPaused) {
          this.warning = this.$t('exoplatform.wallet.warning.contractPaused', {0: this.contractDetails.name});
        } else {
          this.estimateTransactionFee();
          this.warning = null;
        }
      });
    },
    estimateTransactionFee() {
      if (this.contractDetails && !this.contractDetails.isPaused && this.wallet.tokenBalance && this.wallet.etherBalance && this.contractDetails.sellPrice && this.contractDetails.owner && this.contractDetails.contractType) {
        const recipient = this.contractDetails.isOwner ? this.recipient : this.contractDetails.owner;

        if (recipient) {
          // Estimate gas
          this.contractDetails.contract.methods
            .transfer(recipient, String(Math.pow(10, this.contractDetails.decimals ? this.contractDetails.decimals : 0)))
            .estimateGas({
              from: this.contractDetails.contract.options.from,
              gas: window.walletSettings.network.gasLimit,
              gasPrice: this.gasPrice,
            })
            .then((estimatedGas) => {
              // Add 10% to ensure that the operation doesn't take more than the estimation
              this.estimatedGas = estimatedGas * 1.1;
            })
            .catch((e) => {
              console.debug('Error while estimating gas', e);
            });
        }
      }
    },
    sendTokens() {
      this.error = null;
      this.warning = null;

      if (!this.$refs.form.validate()) {
        return;
      }
      if (this.contractDetails && this.contractDetails.isPaused) {
        this.warning = this.$t('exoplatform.wallet.warning.contractPaused', {0: this.contractDetails.name});
        return;
      }

      if (!window.localWeb3.utils.isAddress(this.recipient)) {
        this.error = this.$t('exoplatform.wallet.warning.invalidReciepientAddress');
        return;
      }

      if (!this.amount || isNaN(parseFloat(this.amount)) || !isFinite(this.amount) || this.amount <= 0) {
        this.error = this.$t('exoplatform.wallet.warning.invalidAmount');
        return;
      }

      if (!this.storedPassword && (!this.walletPassword || !this.walletPassword.length)) {
        this.error = this.$t('exoplatform.wallet.warning.requiredPassword');
        return;
      }

      const unlocked = unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = this.$t('exoplatform.wallet.warning.wrongPassword');
        return;
      }

      if (this.wallet.tokenBalance < this.amount) {
        this.error = this.$t('exoplatform.wallet.warning.unsufficientFunds');
        return;
      }

      if (!this.canSendToken) {
        return;
      }

      const sender = this.contractDetails.contract.options.from;
      const receiver = this.recipient;
      const transfer =  this.contractDetails.contract.methods.transfer;

      const transactionDetail = {
        from: sender.toLowerCase(),
        to: receiver,
        value: 0,
        gas: window.walletSettings.network.gasLimit,
        gasPrice: this.gasPrice,
        pending: true,
        contractAddress: this.contractDetails.address,
        contractMethodName: 'transfer',
        contractAmount: this.amount,
        label: this.transactionLabel,
        message: this.transactionMessage,
        timestamp: Date.now(),
        fee: this.transactionFeeEther,
        feeFiat: this.transactionFeeFiat,
        tokenFee: this.transactionFeeToken,
      };

      this.error = null;
      this.loading = true;
      return transfer(this.recipient, convertTokenAmountToSend(this.amount, this.contractDetails.decimals).toString())
        .estimateGas({
          from: sender,
          gas: window.walletSettings.network.gasLimit,
          gasPrice: this.gasPrice,
        })
        .catch((e) => {
          console.error('Error estimating necessary gas', e);
          return 0;
        })
        .then((estimatedGas) => {
          if (estimatedGas > window.walletSettings.network.gasLimit) {
            this.warning = this.$t('exoplatform.wallet.warning.lowConfiguredTransactionGas', {0: window.walletSettings.network.gasLimit, 1: estimatedGas});
            return;
          }
          return sendContractTransaction(transactionDetail, transfer, [receiver, convertTokenAmountToSend(this.amount, this.contractDetails.decimals).toString()]);
        })
        .then((savedTransaction, error) => {
          if (error) {
            throw error;
          }
          if (savedTransaction) {
            // The transaction has been hashed and will be sent
            this.$emit(
              'sent',
              savedTransaction,
              this.contractDetails
            );

            this.$emit('close');

            if (this.notificationId) {
              // Asynchronously mark notification as sent
              markFundRequestAsSent(this.notificationId);
            }
          }
        })
        .catch((e) => {
          console.debug('Web3 contract.transfer method - error', e);
          this.error = `${this.$t('exoplatform.wallet.error.emptySendingTransaction')}: ${truncateError(e)}`;
        })
        .finally(() => {
          this.loading = false;
          lockBrowserWallet();
        });
    },
    checkErrors() {
      this.error = null;

      if(!this.contractDetails) {
        return;
      }

      if (this.recipient === this.walletAddress && this.contractDetails.contractType > 0) {
        this.error = `You can't send '${this.contractDetails.name}' to yourself`;
        this.canSendToken = false;
        return;
      }

      if (this.amount && (isNaN(parseFloat(this.amount)) || !isFinite(this.amount) || this.amount <= 0)) {
        this.error = 'Invalid amount';
        return;
      } else if (this.amount && $.isNumeric(this.amount)) {
        this.error = (!this.wallet || this.wallet.tokenBalance >= this.amount) ? null : 'Unsufficient funds';
        return;
      }
    },
  },
};
</script>
