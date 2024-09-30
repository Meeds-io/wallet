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
  <tr>
    <td class="text-start">
      <v-avatar size="36px">
        <img
          :src="walletAvatar"
          onerror="this.src = '/platform-ui/skin/images/avatar/DefaultSpaceAvatar.png'"
          alt="">
      </v-avatar>
      <wallet-reward-profile-chip
        :address="walletAddress"
        :profile-id="walletId"
        :profile-technical-id="walletTechnicalId"
        :space-id="walletSpaceId"
        :profile-type="walletType"
        :display-name="walletName"
        :enabled="enabled"
        :deleted-user="deletedUser"
        :disabled-user="disabledUser"
        :avatar="walletAvatar"
        :initialization-state="initializationState"
        display-no-address />
    </td>
    <td class="text-center">
      <span>
        {{ points }}
      </span>
    </td>
    <td class="text-center">
      <template v-if="!status">
        <v-tooltip
          v-if="!walletAddress"
          bottom>
          <template #activator="{ on, attrs }">
            <v-icon
              color="orange darken-2"
              size="16"
              v-bind="attrs"
              v-on="on">
              fas fa-exclamation-triangle
            </v-icon>
          </template>
          <span>{{ $t('exoplatform.wallet.label.noAddress') }}</span>
        </v-tooltip>
        <v-tooltip
          v-else-if="!amount"
          bottom>
          <template #activator="{ on, attrs }">
            <v-icon
              color="orange darken-2"
              size="16"
              v-bind="attrs"
              v-on="on">
              fas fa-exclamation-triangle
            </v-icon>
          </template>
          <span>{{ $t('exoplatform.wallet.label.noEnoughEarnedPoints') }}</span>
        </v-tooltip>
        <div v-else>
          -
        </div>
      </template>
      <v-progress-circular
        v-else-if="status === 'pending'"
        color="primary"
        indeterminate
        size="20" />
      <v-tooltip
        v-else
        bottom>
        <template #activator="{ on, attrs }">
          <v-icon
            :color="statusIconColor"
            size="16"
            v-bind="attrs"
            v-on="on">
            {{ statusIcon }}
          </v-icon>
        </template>
        <span>{{ statusIconTitle }}</span>
      </v-tooltip>
    </td>
    <td class="text-center">
      <span
        v-if="amount"
        :title="$t('exoplatform.wallet.label.amountSent')"
        class="grey--text text--darken-1">
        <span class="symbol fundsLabels"> {{ tokenSymbol }} </span>{{ walletUtils.toFixed(amount) }}
      </span>
      <span
        v-else
        :title="$t('exoplatform.wallet.label.noRewardsForPeriod')"
        class="grey--text text--darken-1">
        <span class="symbol fundsLabels"> {{ tokenSymbol }} </span> 0
      </span>
    </td>
    <td class="text-center">
      <v-menu
        v-model="showMenu"
        :close-on-content-click="false"
        transition="scale-transition"
        offset-y
        eager>
        <template #activator="{ on }">
          <v-btn
            icon
            class="ml-2"
            v-on="on"
            @blur="closeMenu()">
            <v-icon>mdi-dots-vertical</v-icon>
          </v-btn>
        </template>
        <v-list class="pa-0">
          <v-tooltip
            bottom
            :disabled="status">
            <template #activator="{ on }">
              <div v-on="on" class="d-inline-block">
                <v-list-item
                  :disabled="!status"
                  dense
                  @mousedown="$event.preventDefault()"
                  @click="openTransaction">
                  <v-list-item-icon class="me-2 my-auto">
                    <v-icon
                      :disabled="!status"
                      size="14"
                      class="icon-default-color">
                      fas fa-external-link-alt
                    </v-icon>
                  </v-list-item-icon>
                  <v-list-item-title class="d-flex">{{ $t('wallet.administration.rewardDetails.label.openTransaction') }}</v-list-item-title>
                </v-list-item>
              </div>
            </template>
            <span>{{ $t('wallet.administration.rewardDetails.label.notSentYet') }}</span>
          </v-tooltip>
          <v-list-item
            dense
            @mousedown="$event.preventDefault()"
            @click="seeHistory">
            <v-list-item-icon class="me-2 my-auto">
              <v-icon size="14" class="icon-default-color">fas fa-history</v-icon>
            </v-list-item-icon>
            <v-list-item-title class="d-flex">{{ $t('wallet.administration.rewardDetails.label.seeHistory') }}</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </td>
  </tr>
</template>

<script>
export default {
  props: {
    reward: {
      type: Object,
      default: null
    },
    tokenSymbol: {
      type: String,
      default: ''
    },
    completelyProceeded: {
      type: Boolean,
      default: false
    },
  },
  data: () => ({
    term: null,
    showMenu: false,
    currentTimeInSeconds: Date.now() / 1000,
    lang: eXo.env.portal.language,
    dateFormat: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    },
    sendingInProgress: false
  }),
  computed: {
    walletAvatar() {
      return this.reward?.wallet?.avatar;
    },
    walletAddress() {
      return this.reward?.wallet?.address;
    },
    walletId() {
      return this.reward?.wallet?.id;
    },
    walletTechnicalId() {
      return this.reward?.wallet?.technicalId;
    },
    walletSpaceId() {
      return this.reward?.wallet?.spaceId;
    },
    walletType() {
      return this.reward?.wallet?.type;
    },
    walletName() {
      return this.reward?.wallet?.name;
    },
    enabled() {
      return this.reward?.wallet?.enabled;
    },
    deletedUser() {
      return this.reward?.wallet?.deletedUser;
    },
    disabledUser() {
      return this.reward?.wallet?.disabledUser;
    },
    initializationState() {
      return this.reward?.wallet?.initializationState;
    },
    points() {
      return this.reward?.points;
    },
    status() {
      return this.reward?.status;
    },
    success() {
      return this.status === 'success';
    },
    pending() {
      return this.status === 'pending';
    },
    statusIcon() {
      return this.success ? 'fa-check-circle' : 'fa-exclamation-circle';
    },
    statusIconColor() {
      return this.success ? 'success' : 'error';
    },
    statusIconTitle() {
      return this.success ? this.$t('wallet.administration.rewardDetails.successfullyProceeded') :
        this.pending ? this.$t('wallet.administration.rewardDetails.transactionInProgress') : this.$t('wallet.administration.rewardDetails.transactionError');
    },
    amount() {
      return this.completelyProceeded ? this.reward?.tokensSent : this.reward?.amount;
    },
  },
  methods: {
    closeMenu(){
      this.showMenu = false;
    },
    openTransaction() {
      // TO DO
    },
    seeHistory() {
      // TO DO
    }
  }
};

</script>