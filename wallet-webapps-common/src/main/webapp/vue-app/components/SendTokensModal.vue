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
  <div>
    <v-btn
      v-if="canBoostTransaction"
      v-show="showBoostButton"
      color="primary"
      block
      text>
      {{ $t('exoplatform.wallet.button.boost') }}
    </v-btn>
    <v-btn
      v-else
      :disabled="isReadOnly"
      class="btn btn-primary"
      block
      @click="openSendTokenDrawer">
      <v-icon color="white" class="mr-8">
        mdi-cash-refund
      </v-icon>
      {{ $t('exoplatform.wallet.button.sendfunds') }}
    </v-btn>
    <send-tokens-form
      ref="sendTokensForm"
      :wallet="wallet"
      :transaction="transaction"
      :contract-details="contractDetails"
      @sent="$emit('sent', $event, contractDetails)"
      @error="$emit('error', $event)" />
  </div>
</template>


<script>
import {checkFundRequestStatus} from '../js/WalletUtils.js';
import {getTransactionCount} from '../js/TokenUtils.js';

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
        console.error('prepareSendForm error - no accounts found');
        return;
      }

      if (receiver && notificationId) {
        receiverType = receiverType || 'user';
        checkFundRequestStatus(notificationId).then(() => {
          this.openSendTokenDrawer();
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
        this.$refs.sendTokensForm.init();
      }
    },
    openSendTokenDrawer(){
      this.$refs.sendTokensForm.open();
      this.$refs.sendTokensForm.init();
    }
  },
};
</script>
