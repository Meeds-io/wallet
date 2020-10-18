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
      color="white"
      height="48"
      flat
      class="border-box-sizing py-3">
      <div
        :class="skeleton && 'skeleton-text skeleton-text-width skeleton-background skeleton-text-height-thick skeleton-border-radius'"
        class="text-header-title text-sub-title text-no-wrap">
        {{ skeleton && '&nbsp;' || title || '' }}
      </div>
      <v-spacer />
      <v-btn
        v-if="clickable || skeleton"
        :disabled="skeleton"
        :class="skeleton && 'skeleton-background skeleton-text'"
        icon
        outlined
        small
        @click="editWorkExperiences">
        <i
          v-if="!skeleton"
          class="uiIconInformation clickable primary--text my-auto pb-2 mr-1"
          @click="openDrawer"></i>
      </v-btn>
    </v-toolbar>
    <v-card
      :class="clickable && 'clickable' || ''"
      class="walletOverviewCard white"
      flat>
      <v-card-text
        class="justify-center ma-auto py-5 d-flex flex-no-wrap"
        @click="!skeleton && clickable && openDrawer()">
        <div
          :class="skeleton && 'skeleton-background skeleton-border-radius skeleton-text-half-width skeleton-text-height-block skeleton-text my-2'"
          class="justify-center d-flex flex-no-wrap">
          <template v-if="skeleton">
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
          </template>
          <template v-else>
            <v-icon
              v-if="currencySymbol"
              class="tertiary-color walletOverviewCurrencySymbol px-2"
              size="48">
              {{ currencySymbol }}
            </v-icon>
            <div
              class="text-color display-2 text-left font-weight-bold walletOverviewBalance">
              {{ rewardBalance }}
            </div>
          </template>
        </div>
      </v-card-text>
    </v-card>

    <wallet-overview-drawer
      ref="walletOverviewDrawer"
      :symbol="currencySymbol" />
  </v-app>
</template>

<script>
export default {
  data: () => ({
    owner: eXo.env.portal.profileOwner === eXo.env.portal.userName,
    currentName: eXo.env.portal.profileOwner,
    title: null,
    currencyName: null,
    currencySymbol: null,
    rewardBalance: 0,
    skeleton: true,
  }),
  computed: {
    clickable() {
      return this.owner && this.rewardBalance > 0;
    },
  },
  created() {
    this.refresh();
  },
  methods: {
    openDrawer() {
      if (this.owner) {
        this.$refs.walletOverviewDrawer.open(this.title);
      }
    },
    refresh() {
      let loading = 1;

      if (!this.currencyName) {
        loading++;
        // Search settings in a sync way
        fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/settings`)
          .then((resp) => resp && resp.ok && resp.json())
          .then(settings => {
            const contract = settings && settings.contractDetail;
            this.currencyName = contract && contract.name;
            this.currencySymbol = contract && contract.symbol;
            if (this.currencyName) {
              this.title = this.$t('exoplatform.wallet.title.rewardedBalance', {0: this.currencyName});
            }
          })
          .finally(() => {
            loading--;
            if (loading === 0) {
              this.skeleton = false;
              // Decrement 'loading' effect after having incremented it in main.js
              document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
            }
          });
      }

      return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/account/detailsById?id=${this.currentName}&type=user`, {credentials: 'include'})
        .then((resp) => resp && resp.ok && resp.json())
        .then(wallet => {
          this.rewardBalance = parseInt(wallet && wallet.rewardBalance * 100 || 0) / 100;
        })
        .finally(() => {
          loading--;
          if (loading === 0) {
            this.skeleton = false;
            // Decrement 'loading' effect after having incremented it in main.js
            document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
          }
        });
    },
  },
};
</script>