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
          {{ $t('exoplatform.wallet.title.tokenInitializationModal', {0: wallet && wallet.name}) }}
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
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.etherAmountPlaceholder')"
              :placeholder="$t('exoplatform.wallet.label.etherAmount')"
              name="etherAmount"
              class="mt-3" />

            <v-text-field
              v-if="dialog"
              v-model="tokenAmount"
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.tokenAmount', {0: contractDetails && contractDetails.name})"
              :placeholder="$t('exoplatform.wallet.label.tokenAmountPlaceholder', {0: contractDetails && contractDetails.name})"
              name="tokenAmount"
              class="mt-3" />

            <v-text-field
              v-if="dialog"
              v-model="transactionLabel"
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.transactionLabel')"
              :placeholder="$t('exoplatform.wallet.label.transactionLabelPlaceholder')"
              name="transactionLabel"
              type="text"
              class="mt-3" />

            <v-textarea
              v-model="transactionMessage"
              :disabled="loading"
              :label="$t('exoplatform.wallet.label.transactionMessage')"
              :placeholder="$t('exoplatform.wallet.label.transactionMessagePlaceholder')"
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
      tokenAmount: null,
      transactionLabel: null,
      transactionMessage: null,
      error: null,
    };
  },
  methods: {
    open(wallet, initialFundsMessage, etherAmount, tokenAmount) {
      if (!wallet) {
        return;
      }
      this.wallet = wallet;
      this.etherAmount = etherAmount;
      this.tokenAmount = tokenAmount;
      this.transactionLabel = `Initialize wallet of ${this.wallet.type} ${this.wallet.name}`;
      this.transactionMessage = initialFundsMessage;

      this.error = null;
      this.loading = false;

      this.dialog = true;
    },
    send() {
      this.loading = true;
      fetch('/portal/rest/wallet/api/admin/transaction/intiialize', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: $.param({
          receiver: this.wallet.address,
          etherAmount: this.etherAmount,
          tokenAmount: this.tokenAmount,
          transactionLabel: this.transactionLabel,
          transactionMessage: this.transactionMessage,
        }),
      }).then((resp) => {
        if (resp && resp.ok) {
          return resp.text();
        } else {
          throw new Error(this.$t('exoplatform.wallet.error.errorSendingInitializationTransaction', {0: this.wallet.address}));
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
