<template>
  <user-notification-template
    :notification="notification"
    :avatar-url="profileAvatarUrl"
    :message="message"
    :url="orderUrl">
    <template #avatar>
      <v-icon size="40">fa-medal</v-icon>
    </template>
    <template #actions>
      <div class="text-truncate">
        <v-icon size="14" class="me-1">fa-money-bill-alt</v-icon>
        {{ content }}
      </div>
    </template>
  </user-notification-template>
</template>
<script>
export default {
  props: {
    notification: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    lang: eXo.env.portal.language,
    dateFormat: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    },
  }),
  computed: {
    tokenSymbol() {
      return this.notification?.parameters?.symbol;
    },
    tokenAmount() {
      return this.notification?.parameters?.amount;
    },
    receiversCount() {
      return this.notification?.parameters?.rewardValidMembersCount;
    },
    rewardPeriodMedian() {
      const rewardPeriodStart = Number(this.notification?.parameters?.rewardStartPeriodDate);
      const rewardPeriodEnd = Number(this.notification?.parameters?.rewardEndPeriodDate);
      return (rewardPeriodEnd + rewardPeriodStart) / 2 * 1000;
    },
    rewardPeriodStart() {
      const rewardPeriodStart = new Date(this.rewardPeriodMedian);
      const day = rewardPeriodStart.getDay();
      const diff = rewardPeriodStart.getDate() - day + (day === 0 ? -6 : 1);
      return this.$dateUtil.formatDateObjectToDisplay(new Date(rewardPeriodStart.setDate(diff)), this.dateFormat, this.lang);
    },
    rewardPeriodEnd() {
      const rewardPeriodEnd = new Date(this.rewardPeriodMedian);
      const day = rewardPeriodEnd.getDay();
      const diff = rewardPeriodEnd.getDate() - day + (day === 0 ? 0 : 7);
      return this.$dateUtil.formatDateObjectToDisplay(new Date(rewardPeriodEnd.setDate(diff)), this.dateFormat, this.lang);
    },
    message() {
      return this.$t('Notification.message.RewardsSentNotificationPlugin', {
        0: `<strong>${this.rewardPeriodStart}</strong>`,
        1: `<strong>${this.rewardPeriodEnd}</strong>`,
      });
    },
    content() {
      return this.$t('Notification.subtitle.RewardsSentNotificationPlugin', {
        0: this.tokenAmount,
        1: this.tokenSymbol,
        2: this.receiversCount,
      });
    },
  },
};
</script>