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
    class="border-radius border-color ma-5 mb-2"
    flat>
    <div class="d-flex flex-column flex-grow-1">
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="text-header text-wrap">
            {{ $t('exoplatform.wallet.label.currentBalance') }}
          </v-list-item-title>
        </v-list-item-content>
        <v-list-item-action v-if="!loading && balanceBelowBudget" class="ma-auto">
          <v-tooltip
            :disabled="$root.isMobile"
            bottom>
            <template #activator="{ on }">
              <div class="d-flex me-2" v-on="on">
                <v-icon
                  color="warning"
                  size="18">
                  fas fa-exclamation-triangle
                </v-icon>
              </div>
            </template>
            <span>{{ balanceBelowBudgetLabel }}</span>
          </v-tooltip>
        </v-list-item-action>
        <v-list-item-action v-if="!loading && useWalletAdmin" class="ma-auto">
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
            <span class="fundsLabels"> MEED {{ tokenBalanceLabel }} </span>
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
  props: {
    rewardSettings: {
      type: Object,
      default: null
    },
    adminWallet: {
      type: Object,
      default: null
    },
    contractDetails: {
      type: Object,
      default: null
    },
    configuredBudget: {
      type: Number,
      default: 0
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data () {
    return {
      walletAdminUri: '/portal/administration/home/rewards/wallet',
    };
  },
  computed: {
    tokenName() {
      return this.contractDetails?.name;
    },
    tokenBalance() {
      return this.adminWallet?.tokenBalance || 0;
    },
    tokenBalanceLabel() {
      return this.walletUtils?.toFixed(this.tokenBalance, 2);
    },
    etherBalance() {
      return this.adminWallet?.etherBalance || 0;
    },
    etherBalanceLabel() {
      return `${this.contractDetails?.cryptocurrency} ${this.walletUtils?.toFixed(this.etherBalance, 2)}`;
    },
    useWalletAdmin() {
      return this.etherBalance && Number(this.etherBalance) >= 0.002 && this.tokenBalance && Number(this.tokenBalance) >= 0.02;
    },
    lowEtherBalance() {
      return this.etherBalance < 1;
    },
    lowTokenBalance() {
      return this.configuredBudget > this.tokenBalance;
    },
    balanceBelowBudget() {
      return this.lowEtherBalance || this.lowTokenBalance;
    },
    balanceBelowBudgetLabel() {
      return this.lowEtherBalance ? this.$t('wallet.administration.lowEtherBalance') : this.$t('wallet.administration.lowTokenBalance');
    }
  },
};
</script>