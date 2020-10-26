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
  <v-app flat dark>
    <main>
      <v-container pa-0>
        <v-layout
          row
          wrap
          mx-0
          style="cursor: pointer">
          <v-flex
            d-flex
            sx12>
            <v-layout
              row
              ma-0
              class="white">
              <v-flex d-flex xs12>
                <v-card flat>
                  <v-card-text class="subtitle-2 text-sub-title pa-2">
                    {{ $t('exoplatform.wallet.title.walletBalanceTitle') }}
                  </v-card-text>
                </v-card>
              </v-flex>
              <v-flex 
                d-flex 
                xs12 
                justify-center>
                <v-card flat>
                  <v-card-text class="pa-2">
                    <a :href="walletUrl" class="text-color display-1 font-weight-bold big-number">
                      {{ walletBalance }} {{ currencySymbol }}
                    </a>
                  </v-card-text>
                </v-card>
              </v-flex>
            </v-layout>
          </v-flex>
        </v-layout>
      </v-container>
    </main>
  </v-app>
</template>

<script>
  import {getWalletAccount} from '../../WalletBalanceAPI.js'
  export default {
    data() {
      return {
        walletBalance: '',
        walletUrl: `${ eXo.env.portal.context }/${ eXo.env.portal.portalName }/wallet`,
        currencySymbol: '',
        firstLoadingWalletBalance: true,
      }
    },
    created() {
      this.getRewardBalance()
        .then(() => this.$root.$emit('application-loaded'));
    },
    methods: {
      getRewardBalance() {
        return getWalletAccount().then(data => {
          this.walletBalance = Math.trunc(data.tokenBalance);
          if (!this.currencySymbol) {
            // Search settings in a sync way
            return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/settings`)
              .then((resp) => resp && resp.ok && resp.json())
              .then(settings => {
                const contract = settings && settings.contractDetail;
                this.currencySymbol = contract && contract.symbol;
                return this.$nextTick();
              });
          }
          return this.$nextTick();
        });
      }
    }
  }
</script>