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
  <div class="d-flex justify-space-between">
    <v-icon
      color="warning"
      size="16"
      class="me-2">
      fa-exclamation-triangle
    </v-icon>
    {{ alertMessage }}
    <div class="align-self-center align-center">
      <v-btn
        v-if="notInstalled"
        :href="metamaskInstallLinlk"
        name="installMetamaskLink"
        target="_blank"
        rel="nofollow noreferrer noopener"
        class="btn primary">
        <span class="text-capitalize">{{ buttonLabel }}</span>
      </v-btn>
      <v-btn
        v-else
        class="btn primary"
        @click="operateMetamaskChange">
        <span class="text-capitalize">{{ buttonLabel }}</span>
      </v-btn>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    notInstalled: {
      type: Boolean,
      default: false,
    },
    notConnected: {
      type: Boolean,
      default: false,
    },
    invalidNetwork: {
      type: Boolean,
      default: false,
    },
    invalidAccount: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    alertMessage() {
      if (this.notInstalled) {
        return this.$t('exoplatform.wallet.warn.metamaskNotInstalled');
      } else if (this.notConnected) {
        return this.$t('exoplatform.wallet.warn.metamaskDisconnected');
      } else if (this.invalidNetwork) {
        return this.$t('exoplatform.wallet.warn.networkVersion');
      } else if (this.invalidAccount) {
        return this.$t('exoplatform.wallet.warn.selectedAddress');
      }
      return '';
    },
    buttonLabel() {
      if (this.notInstalled) {
        return this.$t('exoplatform.wallet.warn.installMetamask');
      } else if (this.notConnected) {
        return this.$t('exoplatform.wallet.warn.connectMetamask');
      } else if (this.invalidNetwork) {
        return this.$t('exoplatform.wallet.warn.changeNetwork');
      } else if (this.invalidAccount) {
        return this.$t('exoplatform.wallet.warn.changeAccount');
      }
      return '';
    },
    isMobile() {
      return this.$vuetify.breakpoint.smAndDown;
    },
    currentSiteLink() {
      return `${window.location.host}${window.location.pathname}`;
    },
    metamaskInstallLinlk() {
      return this.isMobile
        && `https://metamask.app.link/dapp/${this.currentSiteLink}`
        || 'https://metamask.io/';
    },
  },
  methods: {
    operateMetamaskChange(){
      if (this.notConnected) {
        this.walletUtils.connectToMetamask();
      } else if (this.invalidNetwork) {
        this.walletUtils.switchMetamaskNetwork();
      } else if (this.invalidAccount) {
        this.walletUtils.switchMetamaskAccount();
      }
    }
  }
};
</script>
