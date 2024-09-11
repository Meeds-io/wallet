<!--
  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io

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
  <v-card
    :loading="loading"
    class="border-radius border-color ma-5"
    flat>
    <div class="d-flex flex-column flex-grow-1">
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="text-header text-wrap">
            {{ $t('exoplatform.wallet.label.currentBalance') }}
          </v-list-item-title>
        </v-list-item-content>
        <v-list-item-action v-if="useWalletAdmin" class="ma-auto">
          <v-tooltip
            :disabled="$root.isMobile"
            bottom>
            <template #activator="{ on }">
              <div v-on="on">
                <v-btn
                  :href="walletAdminUri"
                  :aria-label="$t('wallet.administration.manageWallet')"
                  small
                  icon>
                  <v-icon size="18">fas fa-credit-card</v-icon>
                </v-btn>
              </div>
            </template>
            <span> {{ $t('wallet.administration.manageWallet') }}</span>
          </v-tooltip>
        </v-list-item-action>
      </v-list-item>
      <v-btn
        v-if="!loading && !useWalletAdmin"
        :href="walletAdminUri"
        class="btn btn-primary align-self-center my-4 px-2">
        <span class="mx-2">
          {{ $t('wallet.administration.fundWallet') }}
        </span>
      </v-btn>
      <template v-else>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="text-wrap">
              {{ tokenName }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action class="d-inline-block ma-auto pe-1">
            <span class="fundsLabels"> MEED {{ tokenBalance }} </span>
          </v-list-item-action>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="text-wrap">
              {{ $t('wallet.administration.gas') }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action class="ma-auto pe-1">
            {{ etherBalanceLabel }}
          </v-list-item-action>
        </v-list-item>
      </template>
    </div>
  </v-card>
</template>
<script>
export default {
  data () {
    return {
      loading: false,
      adminWallet: null,
      contractDetails: null,
      walletAdminUri: '/portal/administration/home/rewards/wallet',
    };
  },
  computed: {
    tokenName() {
      return this.contractDetails?.name;
    },
    tokenSymbol() {
      return this.contractDetails?.symbol;
    },
    tokenBalance() {
      return this.adminWallet?.tokenBalance || 0;
    },
    etherBalance() {
      return this.adminWallet?.etherBalance || 0;
    },
    etherBalanceLabel() {
      return `${this.contractDetails?.cryptocurrency} ${this.walletUtils?.toFixed(this.etherBalance)}`;
    },
    useWalletAdmin() {
      return this.etherBalance && Number(this.etherBalance) >= 0.002 && this.tokenBalance && Number(this.tokenBalance) >= 0.02;
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.loading = true;
      this.error = null;
      return this.walletUtils.initSettings(false, true)
        .then(() => {
          this.contractDetails = window.walletSettings.contractDetail;
          return this.addressRegistry.searchWalletByTypeAndId('admin', 'admin');
        })
        .then((adminWallet) => this.adminWallet = adminWallet)
        .finally(() => {
          this.loading = false;
        });
    },
  }
};
</script>