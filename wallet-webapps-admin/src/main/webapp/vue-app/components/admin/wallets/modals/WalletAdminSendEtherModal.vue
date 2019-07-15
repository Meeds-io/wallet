<template>
  <v-dialog
    v-model="dialog"
    :disabled="disabled"
    attach="#walletDialogsParent"
    content-class="uiPopup with-overflow"
    width="500px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">
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
              v-model="etherAmountLabel"
              :autofocus="dialog"
              :label="$t('exoplatform.wallet.label.etherAmountPlaceHolder')"
              :placeholder="$t('exoplatform.wallet.label.etherAmount')"
              name="etherAmount"
              disabled
              class="mt-3" />

            <v-text-field
              v-if="dialog"
              v-model="transactionLabel"
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.transactionLabel')"
              :placeholder="$t('exoplatform.wallet.label.transactionLabelPlaceHolder')"
              name="transactionLabel"
              type="text"
              class="mt-3" />

            <v-textarea
              v-model="transactionMessage"
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.transactionMessage')"
              :placeholder="$t('exoplatform.wallet.label.transactionMessagePlaceHolder')"
              name="transactionMessage"
              class="mt-4"
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
            class="btn btn-primary mr-1"
            @click="send">
            {{ $t('exoplatform.wallet.button.send') }}
          </button>
          <button
            :disabled="loading"
            class="btn"
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
      this.transactionLabel = `Initialize wallet of ${this.wallet.type} ${this.wallet.name}`;
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
