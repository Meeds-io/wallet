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
  <v-app :class="owner && 'walletOverviewApplication' || 'walletOverviewApplicationOther'">
    <v-toolbar
      v-if="!isOverviewDisplay"
      color="white"
      height="48"
      flat
      class="border-box-sizing py-3">
      <div class="text-header-title text-sub-title text-no-wrap">
        {{ title || '' }}
      </div>
      <v-spacer />
      <v-btn
        v-if="clickable"
        icon
        outlined
        small
        @click="openDrawer">
        <i class="uiIconInformation clickable primary--text my-auto"></i>
      </v-btn>
    </v-toolbar>
    <div
      :class="clickable && 'clickable' || ''"
      class="walletOverviewCard white"
      flat
      @click="clickable && openDrawer()">
      <div
        v-show="!loading"
        :class="isOverviewDisplay ? 'px-0 py-2' : 'py-5'"
        class="justify-center ma-auto d-flex flex-no-wrap">
        <div class="justify-center d-flex flex-no-wrap">
          <div
            :class="isOverviewDisplay ? 'pe-2 display-1' : 'px-2 display-2'"
            class="tertiary-color text-start font-weight-bold walletOverviewBalance">
            {{ currencySymbol }}
          </div>
          <div
            :class="typographyClass"
            class="text-color font-weight-bold d-flex align-self-center walletOverviewBalance">
            {{ balanceToDisplay }}
          </div>
        </div>
      </div>
    </div>

    <wallet-overview-drawer
      ref="walletOverviewDrawer"
      :symbol="currencySymbol" />
  </v-app>
</template>

<script>
import { getCountRewards } from '../../js/WalletBalanceAPI.js';

export default {
  props: {
    isOverviewDisplay: {
      type: Boolean,
      default: () => false,
    },
  },
  data: () => ({
    owner: eXo.env.portal.profileOwner === eXo.env.portal.userName,
    currentName: eXo.env.portal.profileOwner,
    title: null,
    currencyName: null,
    currencySymbol: null,
    rewardBalance: null,
    loading: true,
  }),
  computed: {
    clickable() {
      return this.owner && this.rewardBalance > 0;
    },
    balanceToDisplay() {
      return this.rewardBalance !== null && Number.isFinite(Number(this.rewardBalance)) ? Number(this.rewardBalance).toFixed() : '';
    },
    typographyClass() {
      switch (String(this.balanceToDisplay).length) {
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
        return 'text-h4';
      case 6:
        return 'text-h5';
      case 7:
      case 8:
        return 'text-h6';
      default:
        return 'body-1';
      }
    },
  },
  created() {
    this.refresh();
    this.refreshSettings(window.walletSettings);
  },
  methods: {
    openDrawer() {
      if (this.owner) {
        this.$refs.walletOverviewDrawer.open(this.title);
      }
    },
    refresh() {
      this.loading = true;
      const earningsPromise = getCountRewards(eXo.env.portal.profileOwner)
        .then((resp) => this.rewardBalance = resp.sumRewards);
      const walletDetailsPromise = fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/account/detailsById?id=${this.currentName}&type=user`, {credentials: 'include'})
        .then((resp) => resp && resp.ok && resp.json())
        .then(() => {
          if (!this.currencyName) {
            // Search settings in a sync way
            return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/settings`, {
              method: 'GET',
              credentials: 'include',
            })
              .then((resp) => resp && resp.ok && resp.json())
              .then(this.refreshSettings);
          }
        })
        .then(() => this.$nextTick())
        .finally(() => {
          this.$root.$emit('application-loaded');
        });
      return Promise.all([
        earningsPromise,
        walletDetailsPromise,
      ])
        .then(() => this.$nextTick())
        .finally(() => this.loading = false);
    },
    refreshSettings(settings) {
      if (!settings) {
        return;
      }
      const contract = settings && settings.contractDetail;
      this.currencyName = contract && contract.name;
      this.currencySymbol = contract && contract.symbol;
      if (this.currencyName) {
        this.title = this.$t('exoplatform.wallet.title.rewardedBalance', {0: this.currencyName});
      }
    },
  },
};
</script>