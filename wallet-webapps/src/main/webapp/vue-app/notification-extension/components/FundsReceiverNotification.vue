<template>
  <user-notification-template
    :notification="notification"
    :message="message"
    :actions-class="!content && 'd-none'"
    :url="transactionUrl">
    <template #avatar>
      <v-icon size="40">fa-wallet</v-icon>
    </template>
    <template v-if="content" #actions>
      <div class="text-truncate">
        <v-icon size="14" class="me-1">far fa-comment</v-icon>
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
  computed: {
    content() {
      return this.notification?.parameters?.message || this.notification?.parameters?.label;
    },
    tokenSymbol() {
      return this.notification?.parameters?.symbol;
    },
    tokenAmount() {
      return this.notification?.parameters?.amount;
    },
    isSpaceWallet() {
      return this.notification?.parameters?.account_type === 'space';
    },
    spaceDisplayName() {
      return this.isSpaceWallet && this.notification?.parameters?.receiver || null;
    },
    spaceLink() {
      return this.isSpaceWallet && this.notification?.parameters?.receiverUrl || null;
    },
    spaceUrl() {
      if (!this.isSpaceWallet) {
        return null;
      }
      const spaceUrl = this.spaceLink.match(/http([^"]*)"/g)[0];
      return spaceUrl.substring(0, spaceUrl.length - 1);
    },
    transactionHash() {
      return this.notification?.parameters?.hash;
    },
    transactionUrl() {
      return this.isSpaceWallet && `${this.spaceUrl}/SpaceWallet?hash=${this.transactionHash}`
            || `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet?hash=${this.transactionHash}`;
    },
    message() {
      return this.isSpaceWallet && this.$t('Notification.message.FundsSpaceReceiverNotificationPlugin', {
        0: `<a class="space-name font-weight-bold">${this.spaceDisplayName}</a>`,
        1: this.tokenAmount,
        2: this.tokenSymbol,
      }) || this.$t('Notification.message.FundsReceiverNotificationPlugin', {
        0: this.tokenAmount,
        1: this.tokenSymbol,
      });
    },
  },
};
</script>