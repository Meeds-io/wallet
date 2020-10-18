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
  <v-list-item v-if="pluginEarnedAmount" dense>
    <v-list-item-icon class="my-auto" />
    <v-list-item-content class="align-end text-left">
      <v-list-item-subtitle>
        <span class="text-sub-title caption uiIcon" :class="iconClass"></span>
        <span class="text-sub-title caption">{{ plugin.points }} {{ pluginName }}</span>
      </v-list-item-subtitle>
    </v-list-item-content>
    <v-list-item-icon class="my-auto">
      <span class="text-sub-title caption">
        {{ pluginAmount }}
      </span>
    </v-list-item-icon>
  </v-list-item>
</template>

<script>
export default {
  props: {
    rewardItem: {
      type: Object,
      default: () => ({}),
    },
    plugin: {
      type: Object,
      default: () => ({}),
    },
    symbol: {
      type: String,
      default: 'sent',
    },
  },
  computed: {
    pluginName() {
      return this.plugin.pluginId.charAt(0).toUpperCase() + this.plugin.pluginId.slice(1);
    },
    iconClass() {
      return `uiIconReward${this.pluginName}`;
    },
    pluginEarnedAmount() {
      return parseInt(this.plugin.amount * 100) / 100;
    },
    pluginAmount() {
      return `${this.pluginEarnedAmount} ${this.symbol}`;
    },
  },
};
</script>