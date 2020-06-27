<template>
  <v-dialog
    id="sendTokensModal"
    v-model="dialog"
    :disabled="disabled"
    content-class="uiPopup with-overflow walletDialog"
    width="600px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <template v-slot:activator="{ on }">
      <v-btn
        v-if="canBoostTransaction"
        v-show="showBoostButton"
        color="primary"
        text
        v-on="on">
        {{ $t('exoplatform.wallet.button.boost') }}
      </v-btn>
      <v-btn
        v-else-if="!transaction"
        :disabled="isReadOnly"
        class="btn btn-primary"
        v-on="on">
        <v-icon color="white" class="mr-1">
          send
        </v-icon>
        {{ $t('exoplatform.wallet.button.send') }}
      </v-btn>
    </template>
    <v-card class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="ignore-vuetify-classes PopupTitle popupTitle">
            {{ $t('exoplatform.wallet.button.sendfunds') }}
          </span>
      </div>
      <send-tokens-form
        ref="sendTokensForm"
        :wallet="wallet"
        :transaction="transaction"
        :contract-details="contractDetails"
        @sent="$emit('sent', $event, contractDetails)"
        @close="dialog = false"
        @error="$emit('error', $event)" />
    </v-card>
  </v-dialog>
</template>

<script>
import SendTokensForm from './SendTokensForm.vue';

import {checkFundRequestStatus} from '../js/WalletUtils.js';
import {getTransactionCount} from '../js/TokenUtils.js';

export default {
  components: {
    SendTokensForm,
  },
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
        return {};
      },
    },
    isReadOnly: {
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
  },
  data() {
    return {
      dialog: false,
      firstPendingTransaction: false,
    };
  },
  computed: {
    tokenBalance() {
      return this.wallet && this.wallet.tokenBalance;
    },
    etherBalance() {
      return this.wallet && this.wallet.etherBalance;
    },
    showBoostButton() {
      return this.canBoostTransaction && this.firstPendingTransaction;
    },
    canBoostTransaction() {
      return this.transaction && this.wallet && this.wallet.isApproved && ((this.wallet.type === 'user' && this.wallet.id === eXo.env.portal.userName) || (this.wallet.type === 'space' && this.wallet.spaceAdministrator));
    },
    disabled() {
      return this.isReadOnly || (this.wallet && !this.wallet.isApproved) || !this.tokenBalance || this.tokenBalance === 0 || (typeof this.tokenBalance === 'string' && (!this.tokenBalance.length || this.tokenBalance.trim() === '0')) || !this.etherBalance || this.etherBalance === 0 || (typeof this.etherBalance === 'string' && (!this.etherBalance.length || this.etherBalance.trim() === '0'));
    },
  },
  watch: {
    open() {
      this.dialog = this.open;
    },
    dialog() {
      if (this.dialog) {
        this.$nextTick().then(() => {
          if (this.transaction && this.transaction.fromWallet) {
            this.prepareSendForm(this.transaction.toWallet.id, this.transaction.toWallet.type, this.transaction.contractAmount);
          }
          this.$refs.sendTokensForm.init();
        });
      } else {
        this.$emit('close');
      }
    },
  },
  created() {
    if (this.canBoostTransaction) {
      if (this.wallet.lastMinedNonce === 0 || this.wallet.lastMinedNonce > 0) {
        this.firstPendingTransaction = this.wallet.nextNonceToMine === this.transaction.nonce;
      }
      getTransactionCount(this.wallet.address).then(nonce => {
        this.wallet.nextNonceToMine = nonce;
        this.firstPendingTransaction = this.wallet.nextNonceToMine === this.transaction.nonce;
      });
    }
  },
  methods: {
    prepareSendForm(receiver, receiverType, amount, notificationId) {
      if (!this.contractDetails) {
        console.debug('prepareSendForm error - no accounts found');
        return;
      }

      if (receiver && notificationId) {
        receiverType = receiverType || 'user';
        checkFundRequestStatus(notificationId).then((sent) => {
          this.dialog = !sent;
          return this.$nextTick(() => {
            if (receiver) {
              this.$refs.sendTokensForm.$refs.autocomplete.selectItem(receiver, receiverType);
              this.$refs.sendTokensForm.disabledRecipient = true;
              this.$refs.sendTokensForm.amount = amount && Number(amount) || '';
              this.$refs.sendTokensForm.notificationId = notificationId;
            }
          });
        });
      } else if (receiver) {
        this.dialog = true;
        return this.$nextTick(() => {
          if (receiver) {
            receiverType = receiverType || 'user';
            this.$refs.sendTokensForm.$refs.autocomplete.selectItem(receiver, receiverType);
            this.$refs.sendTokensForm.disabledRecipient = true;
            this.$refs.sendTokensForm.amount = amount && Number(amount) || '';
            this.$refs.sendTokensForm.notificationId = notificationId;
          }
        });
      } else {
        this.dialog = true;
        this.$refs.sendTokensForm.init();
      }
    },
  },
};
</script>
