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
  <v-flex
    text-center
    class="summaryRewardCard">
    <v-card class="walletSummaryBalance headline pb-1 elevation-3 pa-1 ma-1">
      <v-card-title class="title subtitle-1 pb-1 text-truncate">
        {{ $t('exoplatform.wallet.label.totalWalletRewards') }}
      </v-card-title>
      <v-card-title class="rewardBalance headline pt-0 pb-1">
        <v-container fluid>
          <v-layout row>
            <v-flex grow class="amount">
              <span class="symbol"> {{ contractDetails.symbol }} </span>  {{ walletUtils.toFixed(rewardBalance) }}
            </v-flex>
          </v-layout>
        </v-container>
      </v-card-title>
    </v-card>
  </v-flex>
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
      type: Object,
      default: function() {
        return {};
      },
    },
  },
  data() {
    return {
      seeRewardDetails: false,
      seeRewardDetailsPermanent: false,
    };
  },
  computed: {
    rewardBalance() {
      return (this.wallet && this.wallet.rewardBalance) || 0;
    },
  },
  watch: {
    seeRewardDetails() {
      if (this.seeRewardDetails) {
        $('body').addClass('hide-scroll');

        const thiss = this;
        setTimeout(() => {
          thiss.seeRewardDetailsPermanent = true;
        }, 200);
      } else {
        $('body').removeClass('hide-scroll');

        this.seeRewardDetailsPermanent = false;
      }
    },
  },
  created() {
    const thiss = this;
    $(document).on('keydown', (event) => {
      if (event.which === 27 && thiss.seeRewardDetailsPermanent && !$('.v-dialog:visible').length) {
        thiss.back();
      }
    });
    this.init();
  },
  methods: {
    displayRewards() {
      this.seeRewardDetails = true;

      this.$nextTick(() => {
        const thiss = this;
        $('.v-overlay').on('click', () => {
          thiss.back();
        });
      });
    },
    back() {
      this.seeRewardDetails = false;
      this.seeRewardDetailsPermanent = false;
      this.selectedAccount = null;
    },
  }
};
</script>
