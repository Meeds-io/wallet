<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2022 Meeds Association
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
  <v-snackbar
    v-model="snackbar"
    :left="!$vuetify.rtl"
    :right="$vuetify.rtl"
    color="transparent"
    elevation="0"
    app>
    <exo-notification-alert
      :alert="alert"
      @dismissed="clear">
      <template #actions v-if="showTransactionLink">
        <a
          :href="transactionHashLink"
          :title="$t('exoplatform.wallet.message.transactionExplorerLink')"
          rel="external nofollow noreferrer noopener"
          class="d-block"
          target="_blank">
          {{ transactionLinkLabel }}
        </a>
      </template>
    </exo-notification-alert>
  </v-snackbar>
</template>
<script>
export default {
  data: () => ({
    snackbar: false,
    alert: null,
  }),
  computed: {
    transactionLinkLabel() {
      return this.$t('exoplatform.wallet.message.followTransaction', {0: this.walletUtils.getTransactionExplorerName()});
    },
    transactionHashLink(){
      return this.walletUtils.getTransactionEtherscanlink().concat(this.alert.transactionHash);
    },
    showTransactionLink() {
      return this.alert?.transactionHash;
    }
  },
  watch: {
    alert() {
      this.snackbar = !!this.alert;
    }
  },
  created() {
    this.$root.$on('wallet-notification-alert', alert => this.alert = alert);
  },
  methods: {
    clear() {
      this.alert = null;
    },
  },
};
</script>
