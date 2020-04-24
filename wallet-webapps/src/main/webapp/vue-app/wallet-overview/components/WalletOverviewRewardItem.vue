<template>
  <div v-if="rewards">
    <wallet-overview-reward-plugin-item
      v-for="(plugin, i) in rewards"
      :id="`plugin-${plugin.pluginId}`"
      :key="i"
      :reward-item="rewardItem"
      :plugin="plugin"
      :symbol="symbol" />
  </div>
</template>
<script>
export default {
  props: {
    rewardItem: {
      type: Object,
      default: () => ({}),
    },
    symbol: {
      type: String,
      default: 'sent',
    },
  },
  computed: {
    rewards() {
      return this.rewardItem && this.rewardItem.rewards && this.rewardItem.rewards.length && this.rewardItem.rewards.filter(plugin => plugin.amount);
    }
  },
};
</script>