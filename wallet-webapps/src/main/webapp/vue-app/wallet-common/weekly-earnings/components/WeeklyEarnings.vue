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
  <v-card
    v-show="!loading"
    flat>
    <div class="pa-0 d-flex justify-center flex-nowrap text-color font-weight-bold big-number">
      <span class="my-2 tertiary-color">{{ symbol }}</span>
      <span
        :class="typographyClass"
        class="ma-2 text-color text-h5 font-weight-bold d-flex align-self-center">
        {{ lastRewardToDisplay || '-' }}
      </span>
    </div>
  </v-card>
</template>
<script>
export default {
  data: () => ({
    lastReward: null,
    wallet: null,
    contractDetails: null,
    loading: true,
    limit: 10,
  }),
  computed: {
    symbol() {
      return this.contractDetails?.symbol;
    },
    lastRewardToDisplay() {
      return Number.isFinite(Number(this.lastReward)) ? Math.trunc(this.lastReward) : 0;
    },
    balanceToDisplay() {
      return Number.isFinite(Number(this.balance))
        ? new Intl.NumberFormat(eXo.env.portal.language, {
          style: 'decimal',
          minimumFractionDigits: 0,
          maximumFractionDigits: 0,
        }).format(Math.trunc(this.balance))
        : 0;
    },
  },
  watch: {
    lastReward() {
      this.$root.$emit('wallet-last-reward', this.lastReward);
    },
  },
  created() {
    this.init()
      .finally(() => this.loading = false);
  },
  methods: {
    init() {
      return this.walletUtils.initSettings(false, true, true)
        .then(() => {
          if (window.walletSettings) {
            this.wallet = window.walletSettings.wallet
              && {...window.walletSettings.wallet}
              || {};
            this.contractDetails = window.walletSettings.contractDetail
              && {...window.walletSettings.contractDetail}
              || {};
          }
          return this.$nextTick();
        })
        .then(() => this.refreshRewards());
    },
    refreshRewards() {
      return this.wallet?.address?.length && this.$rewardService.getRewardsByUser(this.limit)
        .then(rewards => rewards.find(r => r.transaction && r.status === 'success'))
        .then(reward => this.lastReward = reward?.tokensToSend || 0);
    },
  },
};
</script>
