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
  <v-dialog
    v-model="dialog"
    content-class="uiPopup walletDialog"
    width="400px"
    max-width="100vw"
    @keydown.esc="dialog = false">
    <v-card v-if="wallet" class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false">
        </a>
        <span class="ignore-vuetify-classes PopupTitle popupTitle">
          {{ $t('exoplatform.wallet.title.rewardDetailsOfWallet', {0: wallet.wallet.name}) }}
        </span>
      </div>
      <v-list dense>
        <v-list-item>
          <v-list-item-content>{{ $t('exoplatform.wallet.label.period') }}:</v-list-item-content>
          <v-list-item-content class="align-end">{{ period }}</v-list-item-content>
        </v-list-item>
        <v-list-item v-if="wallet.tokensSent">
          <v-list-item-content>{{ $t('exoplatform.wallet.label.rewardsSent') }}:</v-list-item-content>
          <v-list-item-content class="align-end">{{ walletUtils.toFixed(wallet.tokensSent) }} {{ symbol }}</v-list-item-content>
        </v-list-item>
        <v-list-item v-else-if="wallet.tokensToSend">
          <v-list-item-content>{{ $t('exoplatform.wallet.label.computedRewardsToSend') }}:</v-list-item-content>
          <v-list-item-content class="align-end">{{ walletUtils.toFixed(wallet.tokensToSend) }} {{ symbol }}</v-list-item-content>
        </v-list-item>
        <template v-if="rewards.length">
          <v-divider />
          <v-list-item v-for="reward in rewards" :key="reward.pluginId">
            <v-list-item-content>{{ $t('exoplatform.wallet.label.rewardPluginPointDetails', {0: reward.points, 1: reward.pluginId}) }}:</v-list-item-content>
            <v-list-item-content class="align-end">{{ walletUtils.toFixed(reward.amount) }} {{ symbol }}</v-list-item-content>
          </v-list-item>
        </template>
        <div v-else>
          <div class="alert alert-info">
            <i class="uiIconInfo"></i>{{ $t('exoplatform.wallet.label.noRewards') }}
          </div>
        </div>
      </v-list>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: {
    wallet: {
      type: Object,
      default: function() {
        return {};
      },
    },
    symbol: {
      type: String,
      default: function() {
        return null;
      },
    },
    period: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      dialog: false,
    };
  },
  computed: {
    rewards() {
      return this.wallet.rewards && this.wallet.rewards.length ? this.wallet.rewards.filter(reward => reward.amount || reward.points) : [];
    },
  },
  watch: {
    dialog() {
      if (!this.dialog) {
        this.$emit('closed');
      }
    }
  },
  methods: {
    open() {
      this.dialog = true;
    }
  }
};
</script>
