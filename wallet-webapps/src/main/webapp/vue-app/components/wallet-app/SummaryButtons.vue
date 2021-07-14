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
    ref="walletSummaryActions"
    :right="!$vuetify.rtl">
    <template slot="title">
      <span class="mx-2"> {{ $t('exoplatform.wallet.label.exchanges') }} </span>
    </template>
    <template slot="content">
      <v-layout
        row
        wrap
        class="walletSummaryActions walletSummaryBalance align-end">
        <v-flex
          class="walletSummaryAction">
          <send-tokens-modal
            ref="sendTokensModal"
            :wallet="wallet"
            :is-read-only="isReadOnly"
            :contract-details="contractDetails"
            @sent="$emit('transaction-sent', $event)"
            @error="$emit('error', $event)" />
        </v-flex>
        <v-flex
          class="walletSummaryAction">
          <request-funds-modal
            ref="walletRequestFundsModal"
            :disabled-button="isDisapproved"
            :wallet-address="walletAddress"
            :contract-details="contractDetails" />
        </v-flex>
      </v-layout>
    </template>
  </exo-drawer>
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
      type: Array,
      default: function() {
        return [];
      },
    },
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    isDisapproved() {
      return !this.wallet || !this.wallet.isApproved;
    }
  },
  methods: {
    init(isReadOnly) {
      if (document.location.search && document.location.search.length) {
        const search = document.location.search.substring(1);
        const parameters = JSON.parse(
          `{"${decodeURI(search)
            .replace(/"/g, '\\"')
            .replace(/&/g, '","')
            .replace(/=/g, '":"')}"}`
        );
        if (parameters && parameters.receiver && parameters.receiver_type) {
          if (this.isReadOnly || isReadOnly) {
            throw new Error(this.$t('exoplatform.wallet.warning.walletReadonly'));
          }
          this.$refs.sendTokensModal.prepareSendForm(parameters.receiver, parameters.receiver_type, parameters.amount, parameters.id);
        }
      }
    },
    open() {
      this.$refs.walletSummaryActions.open();
    },
  },
};
</script>
