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
  <exo-drawer
    ref="sendTokensForm"
    width="100%"
    :right="!$vuetify.rtl">
    <template slot="title">
      <div>
        <i class="uiIcon uiArrowBAckIcon" @click="close"></i> 
        <span> {{ $t('exoplatform.wallet.button.sendfunds') }} </span>
      </div>
    </template>
    
    <template slot="content">
      <v-card
        id="sendTokenForm"
        flat
        class="mt-6">
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
            <wallet-reward-address-auto-complete
              ref="autocomplete"
              :disabled="loading || disabledRecipient"
              :input-label="$t('exoplatform.wallet.label.recipient')"
              :input-placeholder="$t('exoplatform.wallet.label.recipientPlaceholder')"
              :ignore-current-user="!isSpace"
              autofocus="true"
              required
              validate-on-blur
              @item-selected="
                recipient = $event.address;
                $emit('receiver-selected', $event);
              " />
            <p class="amountLabel mb-0 mt-2"> {{ $t('exoplatform.wallet.label.amount') }} </p>
            <v-text-field
              v-model.number="amount"
              ref="amount"
              :disabled="loading || transaction"
              :placeholder="$t('exoplatform.wallet.label.amountPlaceholder')"
              type="number"
              name="amount"
              required
              validate-on-blur
              class="mt-n4"
              @input="$emit('amount-selected', amount)" />
            <p v-if="!storedPassword && isInternalWallet" class="amountLabel mb-0 mt-2">{{ $t('exoplatform.wallet.label.walletPassword') }}</p>
            <p v-else-if="!isInternalWallet" class="amountLabel mb-0 mt-2">{{ $t('exoplatform.wallet.label.settings.internal') }}</p>
            <v-row class="pl-5" v-if="!isInternalWallet">
              <v-col
                cols="12"
                sm="6" 
                class="d-flex align-center ml-n5">
                <img
                  class="mt-n1"
                  :src="`/wallet-common/images/metamask.svg`"
                  alt="Metamask"
                  width="18">
                <span class="pl-2 amountLabel">{{ $t('exoplatform.wallet.label.metamask') }}</span>
              </v-col>
              <v-col
                cols="12"
                sm="6"
                align="right">
                <v-chip class="grey-background">  
                  <span class="mr-3 dark-grey-color walletTitle">
                    {{ metamaskAddressPreview }}
                  </span>
                  <wallet-settings-jdenticon :address="walletAddress" />
                </v-chip>
              </v-col>
            </v-row>
            <v-text-field
              v-if="!storedPassword && isInternalWallet"
              ref="walletPassword"
              v-model="walletPassword"
              :append-icon="walletPasswordShow ? 'mdi-eye' : 'mdi-eye-off'"
              :type="walletPasswordShow ? 'text' : 'password'"
              :disabled="loading"
              :rules="mandatoryRule"
              :placeholder="$t('exoplatform.wallet.label.walletPasswordPlaceholder')"
              name="walletPassword"
              required
              validate-on-blur
              class="mt-n4"
              autocomplete="current-password"
              @click:append="walletPasswordShow = !walletPasswordShow" />
            <v-text-field
              v-model="transactionLabel"
              :disabled="loading || transaction"
              :label="$t('exoplatform.wallet.label.transactionLabel')"
              :placeholder="$t('exoplatform.wallet.label.transactionLabelPlaceholder')"
              type="text"
              name="transactionLabel" />
            <v-textarea
              v-model="transactionMessage"
              :disabled="loading || transaction"
              :label="$t('exoplatform.wallet.label.transactionMessage')"
              :placeholder="$t('exoplatform.wallet.label.transactionMessagePlaceholder')"
              name="tokenTransactionMessage"
              flat
              no-resize />
          </v-form>
        </v-card-text>
      </v-card>
    </template>
    <template slot="footer">
      <div v-if="switchAlertAndWarning">
        <wallet-notification-warnings
          :alert-message="alertMessage"
          :alert-switch-metamask-actions="alertSwitchMetamaskActions"
          :alert-button-message="alertButtonMessage" />      
      </div>
      <div class="VuetifyApp flex d-flex" v-else>
        <v-spacer />
        <button
          :disabled="disabled"
          class="ignore-vuetify-classes btn mx-1"
          color="secondary"
          @click="showQRCodeModal = true">
          {{ $t('exoplatform.wallet.button.qrCode') }}
        </button>
        <button
          :disabled="disabled"
          :loading="loading"
          class="ignore-vuetify-classes btn btn-primary"
          @click="sendTokens">
          {{ $t('exoplatform.wallet.button.send') }}
        </button>
        <wallet-reward-qr-code-modal
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
      </div>
    </template>
  </exo-drawer>
</template>

<script>
import {unlockBrowserWallet, lockBrowserWallet, truncateError, hashCode, toFixed, convertTokenAmountToSend, etherToFiat, markFundRequestAsSent} from '../js/WalletUtils.js';
import {sendContractTransaction} from '../js/TokenUtils.js';
import {searchWalletByAddress} from '../js/AddressRegistry.js';
import {saveTransactionDetails} from '../js/TransactionUtils.js';

export default {
  props: {
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    transaction: {
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
      switchAlertAndWarning: false,
      loading: false,
      showQRCodeModal: false,
      storedPassword: false,
      transactionLabel: '',
      transactionMessage: '',
      walletPassword: '',
      walletPasswordShow: false,
      recipient: null,
      notificationId: null,
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
      isSameNetworkVersion: true,
      isSameAddress: true,
      warningInsteadOfbtns: false,
      alertMessage: '',
      alertButtonMessage: '',
      alertSwitchMetamaskActions: ''
    };
  },
  computed: {
  
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    metamaskAddressPreview() {
      return this.walletAddress && `${this.walletAddress.substring(0,5)}...${this.walletAddress.substring(this.walletAddress.length-4,this.walletAddress.length)}`;
    },
    disabled() {
      return (!this.isSameAddress || !this.isSameNetworkVersion) || (!this.walletAddress || this.loading || !this.gasPrice || !this.recipient || !this.amount || !this.canSendToken || (this.isInternalWallet && (!this.storedPassword && (!this.walletPassword || !this.walletPassword.trim().length))));
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
    isInternalWallet() {
      return window.walletSettings.wallet?.provider === 'INTERNAL_WALLET';
    },
    isMetamaskWallet() {
      return window.walletSettings.wallet?.provider === 'METAMASK';
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

          return searchWalletByAddress(this.recipient, true)
            .then((receiverWallet) => {
              // Unknown wallet address
              if (!receiverWallet || !receiverWallet.address || !receiverWallet.id) {
                this.canSendToken = true;
                return;
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
  created () {
    this.init();
  },
  mounted() {
    window.ethereum.on('accountsChanged' , () => {
      if (window.walletSettings?.wallet?.provider !== 'INTERNAL_WALLET') {
        this.isSameNetworkVersion = parseInt(window.ethereum?.networkVersion) === window.walletSettings?.network?.id;
        this.isSameAddress = window.ethereum?.selectedAddress && window.ethereum?.selectedAddress === window.walletSettings?.wallet?.address || false;
        if (!this.isSameAddress) {
          this.alertMessage=this.$t('exoplatform.wallet.warn.selectedAddress');
          this.alertSwitchMetamaskActions= 'changeAccount';
          this.alertButtonMessage= this.$t('exoplatform.wallet.warn.changeAccount');
          this.switchAlertAndWarning = true;}
        if (!this.isSameNetworkVersion){
          this.switchAlertAndWarning=false;
          this.alertMessage=`${this.$t('exoplatform.wallet.warn.networkVersion')} Polygon`;
          this.alertSwitchMetamaskActions= 'changeNetwork';
          this.alertButtonMessage= this.$t('exoplatform.wallet.warn.changeNetwork');
          this.switchAlertAndWarning=true;
        }
        if (this.isSameAddress && this.isSameNetworkVersion )
        {this.switchAlertAndWarning = false;}
      }
    });

    window.ethereum.on('chainChanged', () => {
      if (window.walletSettings?.wallet?.provider !== 'INTERNAL_WALLET') {
        this.isSameAddress = window.ethereum?.selectedAddress && window.ethereum?.selectedAddress === window.walletSettings?.wallet?.address || false;
        this.isSameNetworkVersion = parseInt(window.ethereum?.networkVersion) !== window.walletSettings?.network?.id;
        if (!this.isSameNetworkVersion){
          this.alertMessage=`${this.$t('exoplatform.wallet.warn.networkVersion')} Polygon`;
          this.alertSwitchMetamaskActions= 'changeNetwork';
          this.alertButtonMessage= this.$t('exoplatform.wallet.warn.changeNetwork');
          this.switchAlertAndWarning=true;
        }
        if (!this.isSameAddress){
          this.switchAlertAndWarning=false;
          this.alertMessage=this.$t('exoplatform.wallet.warn.selectedAddress');
          this.alertSwitchMetamaskActions= 'changeAccount';
          this.alertButtonMessage= this.$t('exoplatform.wallet.warn.changeAccount');
          this.switchAlertAndWarning = true;
        }
        if (this.isSameNetworkVersion && this.isSameAddress )
        {this.switchAlertAndWarning= false;}
      }
    });
  },
  
  methods: {
    //generateId    

    init() {
      this.$nextTick(() => {
        if (this.$refs.autocomplete) {
          this.$refs.autocomplete.clear();
          this.$refs.autocomplete.focus();
        }
      });
      if (this.$refs.walletPassword) {
        this.$refs.walletPassword.blur();
      }
      if (this.$refs.amount) {
        this.$refs.amount.blur();
      }
      this.recipient = null;
      this.amount = null;
      this.notificationId = null;
      this.warning = null;
      this.error = null;
      this.transactionLabel = '';
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.error = null;
      this.loading = false;
      this.disabledRecipient = !!this.transaction;
      this.showQRCodeModal = false;
      this.isSpace = window.walletSettings && window.walletSettings.wallet && window.walletSettings.wallet.type === 'space';

      if (this.transaction) {
        this.transactionLabel = this.transaction.label || '';
        this.transactionMessage = this.transaction.message || '';
        this.gasPrice = this.transaction.gasPrice + 1;
      } else {
        this.transactionLabel = this.defaultLabel;
        this.transactionMessage = this.defaultMessage;
        if (!this.gasPrice) {
          this.gasPrice = window.walletSettings.network.normalGasPrice;
        }
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
              this.estimatedGas = parseInt(estimatedGas * 1.1);
            })
            .catch((e) => {
              console.error('Error while estimating gas', e);
            });
        }
      }
    },
    finalizeSendTransaction(savedTransaction, contractDetails, notificationId) {
      // The transaction has been hashed and will be sent
      this.$emit(
        'sent',
        savedTransaction,
        contractDetails
      );

      this.$emit('close');

      if (notificationId) {
        // Asynchronously mark notification as sent
        markFundRequestAsSent(notificationId);
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

      if (this.isInternalWallet) {
        if (!this.storedPassword && (!this.walletPassword || !this.walletPassword.length)) {
          this.error = this.$t('exoplatform.wallet.warning.requiredPassword');
          return;
        }

        const unlocked = unlockBrowserWallet(this.storedPassword ? window.walletSettings.userP : hashCode(this.walletPassword));
        if (!unlocked) {
          this.error = this.$t('exoplatform.wallet.warning.wrongPassword');
          return;
        }}

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

      this.error = null;
      this.loading = true;
      const transactionDetail = {
        from: sender.toLowerCase(),
        to: receiver,
        value: 0,
        pending: true,
        contractAddress: this.contractDetails.address,
        contractMethodName: 'transfer',
        contractAmount: this.amount,
        label: this.transactionLabel,
        message: this.transactionMessage,
        timestamp: Date.now()
      };
        
      if (this.isInternalWallet) {

        Object.assign(transactionDetail, { 
          gas: window.walletSettings.network.gasLimit,
          gasPrice: this.gasPrice,
          fee: this.transactionFeeEther,
          feeFiat: this.transactionFeeFiat,
          tokenFee: this.transactionFeeToken,
        });

        if (this.transaction) {
          Object.assign(transactionDetail, {
            nonce: this.transaction.nonce,
            label: this.transaction.label || '',
            message: this.transaction.message || '',
            contractAmount: this.transaction.contractAmount,
            to: this.transaction.toWallet.address,
            boost: true,
          });
        }
      
        return transfer(transactionDetail.to, convertTokenAmountToSend(transactionDetail.contractAmount, this.contractDetails.decimals).toString())
          .estimateGas({
            from: transactionDetail.from,
            gas: transactionDetail.gas,
            gasPrice: transactionDetail.gasPrice,
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
              this.finalizeSendTransaction(savedTransaction, this.contractDetails, this.notificationId);
            }
          })
          .catch((e) => {
            console.error('Web3 contract.transfer method - error', e);
            this.error = `${this.$t('exoplatform.wallet.error.emptySendingTransaction')}: ${truncateError(e)}`;
          })
          .finally(() => {
            this.loading = false;
            lockBrowserWallet();
            this.close();
          });
      } else if (this.isMetamaskWallet) {
        
        if (window.ethereum?.isMetaMask) {
          const transactionParameters = {
            to: this.contractDetails.address.toLowerCase(),
            from: sender.toLowerCase(),
            data: transfer( receiver, convertTokenAmountToSend(this.amount, this.contractDetails.decimals).toString())
              .encodeABI()
          };
          return window.ethereum.request({
            method: 'eth_sendTransaction',
            params: [transactionParameters],
          })
            .then((transactionHash) =>{
              Object.assign(transactionDetail, {
                hash: transactionHash
              });
              return saveTransactionDetails(transactionDetail);
            })
            .then((savedTransaction, error) => {
              if (error) {
                throw error;
              }
              if (savedTransaction) {
                this.finalizeSendTransaction(savedTransaction, this.contractDetails, this.notificationId);
              }
            })
            .catch((e) => {
              console.error('Web3 contract.transfer method - error', e);
              this.error = `${this.$t('exoplatform.wallet.error.emptySendingTransaction')}: ${truncateError(e)}`;
            })
            .finally(() => {
              this.loading = false;
              lockBrowserWallet();
              this.close();
            });}
      } else {
        console.error('Error getting provider');
      }
      
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
    open(){
      if (window.walletSettings?.wallet?.provider !== 'INTERNAL_WALLET') {
        this.isSameNetworkVersion = parseInt(window.ethereum?.networkVersion) === window.walletSettings?.network?.id;
        this.isSameAddress = window.ethereum?.selectedAddress && window.ethereum?.selectedAddress === window.walletSettings?.wallet?.address || false;
      }
      if (!this.isSameNetworkVersion){
        this.alertMessage=`${this.$t('exoplatform.wallet.warn.networkVersion')}`;
        this.alertSwitchMetamaskActions= 'changeNetwork';
        this.alertButtonMessage= this.$t('exoplatform.wallet.warn.changeNetwork');
        this.switchAlertAndWarning=true;
      }
      if (!this.isSameAddress){
        this.alertMessage=this.$t('exoplatform.wallet.warn.selectedAddress');
        this.alertSwitchMetamaskActions= 'changeAccount';
        this.alertButtonMessage= this.$t('exoplatform.wallet.warn.changeAccount');
        this.switchAlertAndWarning= true;
      }
      this.$refs.sendTokensForm.open();

    },
    close(){
      this.$refs.sendTokensForm.close();
    },
  },
};
</script>
