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
        @click="editWorkExperiences">
        <i
          class="uiIconInformation clickable primary--text my-auto pb-2 me-1"
          @click="openDrawer"></i>
      </v-btn>
    </v-toolbar>
    <v-card
      :class="clickable && 'clickable' || ''"
      class="walletOverviewCard white"
      flat>
      <v-card-text
        :class="isOverviewDisplay ? 'px-0 py-2' : 'py-5'"
        class="justify-center ma-auto d-flex flex-no-wrap"
        @click="clickable && openDrawer()">
        <div class="justify-center d-flex flex-no-wrap">
          <template>
            <div
              :class="isOverviewDisplay ? 'pe-2' : 'px-2'"
              class="tertiary-color display-2 text-start font-weight-bold walletOverviewBalance">
              {{ currencySymbol }}
            </div>
            <div
              class="text-color display-2 text-start font-weight-bold walletOverviewBalance">
              {{ countReward.toFixed(2) }}
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
import { getCountRewards } from '../../../../../../../wallet-webapps/src/main/webapp/vue-app/WalletBalanceAPI.js';

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
    countReward: 0
  }),
  computed: {
    clickable() {
      return this.owner && this.countReward > 0;
    },
  },
  created() {
    this.refresh();
    getCountRewards(eXo.env.portal.profileOwner).then((resp) => {
      this.countReward = resp.sumRewards;
    });
  },
  methods: {
    openDrawer() {
      if (this.owner) {
        this.$refs.walletOverviewDrawer.open(this.title);
      }
    },
    refresh() {
      return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/account/detailsById?id=${this.currentName}&type=user`, {credentials: 'include'})
        .then((resp) => resp && resp.ok && resp.json())
        .then(() => {
          if (!this.currencyName) {
            // Search settings in a sync way
            return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/settings`, {
              method: 'GET',
              credentials: 'include',
            })
              .then((resp) => resp && resp.ok && resp.json())
              .then(settings => {
                const contract = settings && settings.contractDetail;
                this.currencyName = contract && contract.name;
                this.currencySymbol = contract && contract.symbol;
                if (this.currencyName) {
                  this.title = this.$t('exoplatform.wallet.title.rewardedBalance', {0: this.currencyName});
                }
                return this.$nextTick();
              });
          }
          return this.$nextTick();
        })
        .finally(() => {
          this.$root.$emit('application-loaded');
        });
    }
  },
};
</script>