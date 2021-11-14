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
    <v-card class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a>
        <span class="ignore-vuetify-classes PopupTitle popupTitle">
          <template v-if="wallet && wallet.name">
            {{ $t('exoplatform.wallet.title.sendEtherModalForWallet', {0: wallet.name}) }}
          </template>
          <template v-else>
            {{ $t('exoplatform.wallet.title.sendEtherModal') }}
          </template>
        </span>
      </div>

      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <v-card flat>
        <v-card-text class="pt-0">
          <v-form
            @submit="
              $event.preventDefault();
              $event.stopPropagation();
            ">
            <v-text-field
              v-if="dialog"
              v-model="etherAmount"
              :autofocus="dialog"
              :label="$t('exoplatform.wallet.label.etherAmountPlaceholder')"
              :placeholder="$t('exoplatform.wallet.label.etherAmount')"
              name="etherAmount" />

            <v-text-field
              v-if="dialog"
              v-model="transactionLabel"
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.transactionLabelPlaceholder')"
              :placeholder="$t('exoplatform.wallet.label.transactionLabel')"
              name="transactionLabel"
              type="text" />

            <v-textarea
              v-model="transactionMessage"
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.transactionMessage')"
              :placeholder="$t('exoplatform.wallet.label.transactionMessagePlaceholder')"
              name="transactionMessage"
              rows="3"
              flat
              no-resize />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <button
            :disabled="loading"
            :loading="loading"
            class="ignore-vuetify-classes btn btn-primary me-1"
            @click="send">
            {{ $t('exoplatform.wallet.button.send') }}
          </button>
          <button
            :disabled="loading"
            class="ignore-vuetify-classes btn"
            color="secondary"
            @click="dialog = false">
            {{ $t('exoplatform.wallet.button.close') }}
          </button>
          <v-spacer />
        </v-card-actions>
      </v-card>
    </v-card>
  </v-dialog>
</template>

<script>

export default {
  data() {
    return {
      dialog: null,
      loading: false,
      wallet: null,
      etherAmount: null,
      transactionLabel: null,
      transactionMessage: null,
      error: null,
    };
  },
  computed: {
    etherAmountLabel() {
      return this.etherAmountInFiat ? `${this.etherAmount} (${this.etherAmountInFiat} ${window.walletSettings.fiatSymbol})` : this.etherAmount;
    },
    etherAmountInFiat() {
      return (this.etherAmount && this.walletUtils.etherToFiat(this.etherAmount)) || 0;
    },
  },
  methods: {
    open(wallet, initialFundsMessage, etherAmount) {
      if (!wallet) {
        return;
      }
      this.wallet = wallet;
      this.etherAmount = etherAmount;
      this.transactionLabel = `Send ether for wallet of ${this.wallet.type} ${this.wallet.name}`;
      this.transactionMessage = initialFundsMessage;

      this.error = null;
      this.loading = false;

      this.dialog = true;
    },
    send() {
      this.loading = true;
      fetch('/portal/rest/wallet/api/admin/transaction/sendEther', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: $.param({
          receiver: this.wallet.address,
          etherAmount: this.etherAmount,
          transactionLabel: this.transactionLabel,
          transactionMessage: this.transactionMessage,
        }),
      }).then((resp) => {
        if (resp && resp.ok) {
          return resp.text();
        } else {
          throw new Error(`Error sending ether to wallet ${this.wallet.address}`);
        }
      }).then((hash) => {
        this.$emit('sent', hash);
        this.dialog = false;
      }).catch((error) => {
        this.error = String(error);
      })
        .finally(() => {
          this.loading = false;
        });
    }
  },
};
</script>
