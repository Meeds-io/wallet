<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <v-dialog
    v-model="dialog"
    :disabled="disabled"
    content-class="uiPopup with-overflow walletDialog"
    width="500px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <template v-if="!noButton" v-slot:activator="{ on }">
      <button
        :value="true"
        class="ignore-vuetify-classes btn"
        v-on="on"
        @click="dialog = true">
        {{ $t('exoplatform.wallet.button.initializeWallet') }}
      </button>
    </template>
    <v-card class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a>
        <span class="ignore-vuetify-classes PopupTitle popupTitle">
          {{ $t('exoplatform.wallet.title.initializeWallet') }}
        </span>
      </div>

      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <v-card id="sendTokenForm" flat>
        <v-card-text class="pt-0">
          <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
            <i class="uiIconWarning"></i>{{ warning }}
          </div>
          <div v-if="!error && !warning && information && information.length" class="alert alert-info v-content">
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
              required />

            <v-text-field
              v-model.number="etherAmount"
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.etherAmount')"
              :placeholder="$t('exoplatform.wallet.label.etherAmountPlaceholder')"
              name="etherAmount"
              required />

            <v-text-field
              v-if="!storedPassword"
              v-model="walletPassword"
              :append-icon="walletPasswordShow ? 'mdi-eye' : 'mdi-eye-off'"
              :type="walletPasswordShow ? 'text' : 'password'"
              :disabled="loading"
              :rules="mandatoryRule"
              :label="$t('exoplatform.wallet.label.walletPassword')"
              :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
              name="walletPassword"
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
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <button
            :disabled="disabled"
            :loading="loading"
            class="ignore-vuetify-classes btn btn-primary me-1"
            @click="sendTokens">
            {{ $t('exoplatform.wallet.button.send') }}
          </button>
          <button
            class="ignore-vuetify-classes btn"
            color="secondary"
            @click="dialog = false">
            {{ $t('exoplatform.wallet.button.cancel') }}
          </button>
          <v-spacer />
        </v-card-actions>
      </v-card>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
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
      dialog: false,
      loading: false,
      storedPassword: false,
      transactionLabel: '',
      transactionMessage: '',
      walletPassword: '',
      walletPasswordShow: false,
      recipient: null,
      disabledRecipient: false,
      canSendToken: true,
      amount: null,
      etherAmount: null,
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
          return `${this.walletUtils.toFixed(this.transactionFeeToken)} ${this.contractDetails && this.contractDetails.symbol}`;
        } else {
          return '';
        }
      } else if (this.transactionFeeFiat) {
        return `${this.walletUtils.toFixed(this.transactionFeeFiat)} ${this.fiatSymbol}`;
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
      return this.transactionFeeEther ? this.walletUtils.etherToFiat(this.transactionFeeEther) : 0;
    },
    transactionFeeToken() {
      return this.contractDetails && (this.contractDetails.isOwner || !this.transactionFeeInWei || !this.sellPriceInWei ? 0 : this.walletUtils.toFixed(this.transactionFeeInWei / this.sellPriceInWei));
    },
  },
  watch: {
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

          return this.addressRegistry.searchWalletByAddress(this.recipient, true)
            .then((receiverWallet) => {
              // Unknown wallet address
              if (!receiverWallet || !receiverWallet.address || !receiverWallet.id) {
                this.canSendToken = true;
                return;
              }
              if (this.contractDetails && this.contractDetails.isPaused) {
                this.warning = this.$t('exoplatform.wallet.warning.contractPaused', {0: this.contractDetails.name});
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
      this.recipient = null;
      this.amount = null;
      this.warning = null;
      this.error = null;
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.error = null;
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
            .initializeAccount(recipient, String(Math.pow(10, this.contractDetails.decimals ? this.contractDetails.decimals : 0)))
            .estimateGas({
              from: this.contractDetails.contract.options.from,
              gas: window.walletSettings.network.gasLimit,
              gasPrice: this.gasPrice,
              value: (this.etherAmount && window.localWeb3.utils.toWei(String(this.etherAmount), 'ether')) || 0,
            })
            .then((estimatedGas) => {
              // Add 10% to ensure that the operation doesn't take more than the estimation
              this.estimatedGas = parseInt(estimatedGas * 1.1);
            })
            .catch((e) => {
              console.error('Error while estimating gas', e);
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

      const unlocked = this.walletUtils.unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : this.walletUtils.hashCode(this.walletPassword));
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
      const initializeAccount =  this.contractDetails.contract.methods.initializeAccount;

      this.etherAmount = this.etherAmount || 0;

      const transactionDetail = {
        from: sender.toLowerCase(),
        to: receiver,
        value: this.etherAmount,
        gas: window.walletSettings.network.gasLimit,
        gasPrice: this.gasPrice,
        pending: true,
        contractAddress: this.contractDetails.address,
        contractMethodName: 'initializeAccount',
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
      return initializeAccount(this.recipient, this.walletUtils.convertTokenAmountToSend(this.amount, this.contractDetails.decimals).toString())
        .estimateGas({
          from: sender,
          gas: window.walletSettings.network.gasLimit,
          gasPrice: this.gasPrice,
          value: String(window.localWeb3.utils.toWei(String(this.etherAmount), 'ether')),
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
          return this.tokenUtils.sendContractTransaction(transactionDetail, initializeAccount, [receiver, this.walletUtils.convertTokenAmountToSend(this.amount, this.contractDetails.decimals).toString()]);
        })
        .then((savedTransaction, error) => {
          if (error) {
            throw error;
          }
          if (savedTransaction) {
            this.$emit('sent', savedTransaction);
            this.dialog = false;

            const thiss = this;
            this.walletUtils.watchTransactionStatus(savedTransaction.hash, () => {
              thiss.$emit('success', savedTransaction.hash, thiss.methodName, thiss.autocompleteValue, thiss.inputValue);
            });
          }
        })
        .catch((e) => {
          console.error('Web3 contract.initializeAccount method - error', e);
          this.error = `${this.$t('exoplatform.wallet.error.emptySendingTransaction')}: ${this.walletUtils.truncateError(e)}`;
        })
        .finally(() => {
          this.loading = false;
          this.walletUtils.lockBrowserWallet();
        });
    },
    checkErrors() {
      this.error = null;

      if (!this.contractDetails) {
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
