<!--

  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2023 Meeds Association contact@meeds.io

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
  <div class="walletConfiguration">
    <div class="subtitle-1 mt-4 mb-2 text-color">
      {{ $t('wallet.administration.initialFundsFormLabel') }}
    </div>
    <div class="d-flex flex-row flex-grow-1 flex-shrink-1 mb-5">
      <v-text-field
        v-model="tokenAmount"
        name="tokenAmount"
        class="pa-0 col-6"
        type="number"
        step="1"
        hide-details
        outlined
        dense
        required />
      <span class="my-auto ms-2">{{ $t('wallet.administration.meeds') }}</span>
    </div>
    <div class="subtitle-1 mt-4 mb-2 text-color">
      {{ $t('wallet.administration.initialFundsMessageLabel') }}
    </div>
    <rich-editor
      v-if="initialized"
      id="requestMessage"
      v-model="requestMessage"
      :tag-enabled="false"
      :placeholder="$t('wallet.administration.initialFundsMessagePlaceholder')"
      name="requestMessage"
      max-length="250"
      ck-editor-type="walletAdmin" />
  </div>
</template>
<script>
export default {
  data: () => ({
    loading: true,
    initialized: false,
    tokenAmount: 10,
    requestMessage: '',
    initialFunds: null,
    originalSettings: [10, ''],
  }),
  computed: {
    etherAmountLabel() {
      return this.etherAmountInFiat ? `${this.etherAmount} (${this.etherAmountInFiat} ${window.walletSettings.fiatSymbol})` : this.etherAmount;
    },
    etherAmountInFiat() {
      return (!this.loading && this.etherAmount && this.walletUtils.etherToFiat(this.etherAmount)) || 0;
    },
    etherAmount() {
      return this.initialFunds?.etherAmount || 0;
    },
    modified() {
      return JSON.stringify(this.originalSettings) !== JSON.stringify([this.tokenAmount, this.requestMessage]);
    },
  },
  watch: {
    loading() {
      this.$emit('loading', this.loading);
      if (!this.loading) {
        this.initialized = true;
      }
    },
    modified() {
      this.$emit('modified', this.modified);
    },
    etherAmount() {
      this.$emit('ether-amount', this.etherAmount);
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.loading = true;
      return this.walletUtils.initSettings(false, false, true)
        .then(() => {
          this.initialFunds = window.walletSettings?.initialFunds || {};
          this.etherAmount = this.initialFunds.etherAmount;
          this.tokenAmount = this.initialFunds.tokenAmount;
          this.requestMessage = this.initialFunds.requestMessage || '';
          this.originalSettings = [this.tokenAmount, this.requestMessage];
        })
        .finally(() => this.loading = false);
    },
    reset() {
      this.initialized = false;
      this.tokenAmount = 10;
      this.requestMessage = '';
      this.$nextTick().then(() => this.initialized = true);
    },
    save() {
      this.loading = true;
      return fetch('/portal/rest/wallet/api/settings/saveInitialFunds', {
        method: 'POST',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          ...this.initialFunds,
          requestMessage: this.requestMessage,
          tokenAmount: this.tokenAmount,
        }),
      })
        .then((resp) => {
          if (resp && resp.ok) {
            return resp.text();
          } else {
            throw new Error('Error saving settings');
          }
        })
        .then(() => this.$emit('saved'))
        .catch((e) => {
          console.error('fetch settings - error', e);
          this.$root.$emit('alert-message', this.$t('exoplatform.wallet.warning.errorSavingSettings'), 'error');
        })
        .finally(() => this.loading = false);
    },
  },
};
</script>
