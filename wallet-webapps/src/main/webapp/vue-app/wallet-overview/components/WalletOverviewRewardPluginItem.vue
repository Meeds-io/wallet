<template>
  <v-list-item two-line>
    <v-list-item-content class="align-end text-left">
      <v-list-item-title>
        {{ plugin.points }} {{ plugin.pluginId }}
      </v-list-item-title>
      <v-list-item-subtitle v-text="period" />
    </v-list-item-content>
    <v-list-item-icon class="my-auto">
      <span class="primary--text">
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
  data: () => ({
    lang: eXo.env.portal.language,
    dateFormat: {
      dateStyle: 'long',
    },
  }),
  computed: {
    periodStartDate() {
      return new window.Intl.DateTimeFormat(this.lang, this.dateFormat).format(new Date(this.rewardItem.period.startDateInSeconds * 1000));
    },
    periodEndDate() {
      return new window.Intl.DateTimeFormat(this.lang, this.dateFormat).format(new Date((this.rewardItem.period.endDateInSeconds - 1) * 1000));
    },
    pluginAmount() {
      const amount = parseInt(this.plugin.amount * 100) / 100;
      return `${amount} ${this.symbol}`;
    },
    period() {
      return this.$t(`exoplatform.wallet.label.${this.rewardItem.period.rewardPeriodType.toLowerCase()}Period`, {
        0: this.periodStartDate,
        1: this.periodEndDate,
      });
    },
  },
};
</script>