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
    content-class="uiPopup with-overflow walletDialog"
    class="walletRequestFundsModal"
    width="500px"
    max-width="100vw"
    draggable="true"
    @keydown.esc="dialog = false">
    <template v-slot:activator="{ on }">
      <v-btn
        :disabled="disabledButton"
        class="btn white"
        v-on="on">
        <v-icon class="mr-6">
          mdi-cash-multiple
        </v-icon>
        {{ $t('exoplatform.wallet.button.request') }}
      </v-btn>
    </template>
    <v-card class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="ignore-vuetify-classes PopupTitle popupTitle">
            {{ $t('exoplatform.wallet.button.requestFunds') }}
          </span>
      </div> <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <v-card-text>
        <v-form
          ref="form"
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <address-auto-complete
            ref="autocomplete"
            :disabled="loading"
            :autofocus="dialog"
            :input-label="$t('exoplatform.wallet.label.recipient')"
            :input-placeholder="$t('exoplatform.wallet.label.recipientPlaceholder')"
            required
            ignore-current-user
            no-address
            validate-on-blur
            @item-selected="recipient = $event" />
  
          <v-text-field
            v-model.number="amount"
            :disabled="loading"
            :rules="amoutRules"
            :label="$t('exoplatform.wallet.label.amount')"
            :placeholder="$t('exoplatform.wallet.label.amountPlaceholder')"
            name="amount" />
  
          <v-textarea
            id="requestMessage"
            v-model="requestMessage"
            :disabled="loading"
            :label="$t('exoplatform.wallet.label.requestFundsMessage')"
            :placeholder="$t('exoplatform.wallet.label.requestFundsMessagePlaceholder')"
            name="requestMessage"
            rows="7"
            flat
            no-resize />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button
          :disabled="disabled"
          class="ignore-vuetify-classes btn btn-primary"
          @click="requestFunds">
          {{ $t('exoplatform.wallet.button.sendRequest') }}
        </button>
        <button
          class="ignore-vuetify-classes btn ms-2"
          @click="dialog = false">
          {{ $t('exoplatform.wallet.button.close') }}
        </button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import AddressAutoComplete from './AddressAutoComplete.vue';

export default {
  components: {
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
      type: String,
      default: function() {
        return null;
      },
    },
    disabledButton: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      recipient: null,
      amount: null,
      error: null,
      requestMessage: '',
      loading: false,
      dialog: false,
      amoutRules: [(v) => !!v || this.$t('exoplatform.wallet.warning.requiredField'), (v) => (!isNaN(parseFloat(v)) && isFinite(v) && v > 0) || this.$t('exoplatform.wallet.warning.invalidAmount')],
    };
  },
  computed: {
    disabled() {
      return !this.walletAddress || this.loading || !this.recipient || !this.amount;
    },
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.requestMessage = '';
        this.recipient = null;
        this.amount = null;
        if (this.$refs && this.$refs.autocomplete) {
          this.$refs.autocomplete.clear();
        }
      }
    },
  },
  methods: {
    requestFunds() {
      if (!this.$refs.form.validate()) {
        return;
      }
      if (!this.contractDetails) {
        this.error = this.t('exoplatform.wallet.warning.contractIsMandatory');
        return;
      }

      if (!this.recipient) {
        this.error = this.t('exoplatform.wallet.warning.invalidReciepientAddress');
        return;
      }

      this.loading = true;
      fetch('/portal/rest/wallet/api/account/requestFunds', {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          address: this.walletAddress,
          receipient: this.recipient.id,
          receipientType: this.recipient.type,
          contract: this.contractDetails && this.contractDetails.address,
          amount: this.amount,
          message: this.requestMessage,
        }),
      })
        .then((resp) => {
          if (resp && resp.ok) {
            this.dialog = false;
          } else {
            this.error = this.$t('exoplatform.wallet.error.errorRequestingFunds');
          }
          this.loading = false;
        })
        .catch((e) => {
          console.error('requestFunds method - error', e);
          this.error = `this.$t('exoplatform.wallet.error.errorProceeding'): ${e}`;
          this.loading = false;
        });
    },
  },
};
</script>
