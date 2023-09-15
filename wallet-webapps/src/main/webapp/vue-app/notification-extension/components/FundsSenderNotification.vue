<template>
  <user-notification-template
    :notification="notification"
    :message="message"
    :actions-class="!content && 'd-none'"
    :url="transactionUrl">
    <template #avatar>
      <v-icon size="40">fa-money-bill-alt</v-icon>
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
    senderDisplayName() {
      return this.notification?.parameters?.sender;
    },
    isSenderSpaceWallet() {
      return this.notification?.parameters?.account_type === 'space';
    },
    senderSpaceLink() {
      return this.isSenderSpaceWallet && this.notification?.parameters?.senderUrl || null;
    },
    senderSpaceUrl() {
      if (!this.isSenderSpaceWallet) {
        return null;
      }
      const spaceUrl = this.senderSpaceLink.match(/http([^"]*)"/g)[0];
      return spaceUrl.substring(0, spaceUrl.length - 1);
    },
    receiverDisplayName() {
      return this.notification?.parameters?.receiver;
    },
    isReceiverSpaceWallet() {
      return this.notification?.parameters?.receiver_type === 'space';
    },
    transactionHash() {
      return this.notification?.parameters?.hash;
    },
    transactionUrl() {
      if (this.isSenderSpaceWallet) {
        return `${this.senderSpaceUrl}/SpaceWallet?hash=${this.transactionHash}`;
      } else {
        return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet?hash=${this.transactionHash}`;
      }
    },
    message() {
      if (this.isReceiverSpaceWallet && this.isSenderSpaceWallet) {
        return this.$t('Notification.message.FundsSpaceSentToSpaceNotificationPlugin', {
          0: `<a class="space-name font-weight-bold">${this.senderDisplayName}</a>`,
          1: this.tokenAmount,
          2: this.tokenSymbol,
          3: `<a class="space-name font-weight-bold">${this.receiverDisplayName}</a>`,
        });
      } else if (this.isSenderSpaceWallet) {
        return this.$t('Notification.message.FundsSpaceSentNotificationPlugin', {
          0: `<a class="space-name font-weight-bold">${this.senderDisplayName}</a>`,
          1: this.tokenAmount,
          2: this.tokenSymbol,
          3: `<a class="space-name font-weight-bold">${this.receiverDisplayName}</a>`,
        });
      } else if (this.isReceiverSpaceWallet) {
        return this.$t('Notification.message.FundsSentToSpaceNotificationPlugin', {
          0: this.tokenAmount,
          1: this.tokenSymbol,
          2: `<a class="space-name font-weight-bold">${this.receiverDisplayName}</a>`,
        });
      } else {
        return this.$t('Notification.message.FundsSentNotificationPlugin', {
          0: this.tokenAmount,
          1: this.tokenSymbol,
          2: `<a class="user-name font-weight-bold">${this.receiverDisplayName}</a>`,
        });
      }
    },
  },
};
</script>