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