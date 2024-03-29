<template>
  <user-notification-template
    v-if="!hidden"
    :notification="notification"
    :avatar-url="userAvatarUrl"
    :message="message"
    :url="userUrl">
    <template #actions>
      <div v-if="content" class="text-truncate">
        <v-icon size="14" class="me-1">far fa-comment</v-icon>
        {{ contentText }}
      </div>
      <div class="text-truncate mt-2">
        <v-btn
          :href="acceptUrl"
          class="btn success me-2"
          small
          dark>
          <v-icon size="14" class="me-2">fa-check</v-icon>
          <span class="text-none">
            {{ $t('Notification.label.Accept') }}
          </span>
        </v-btn>
        <v-btn
          :loading="refusing"
          class="btn error"
          small
          dark
          @click.stop.prevent="refuse">
          <v-icon size="14" class="me-2">fa-times</v-icon>
          {{ $t('Notification.label.Refuse') }}
        </v-btn>
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
    hidden: false,
    refusing: false,
  }),
  computed: {
    notificationId() {
      return this.notification.id;
    },
    content() {
      return this.notification?.parameters?.message || this.notification?.parameters?.label;
    },
    contentText() {
      return this.content && this.$utils.htmlToText(this.content) || '';
    },
    tokenSymbol() {
      return this.notification?.parameters?.symbol;
    },
    tokenAmount() {
      return new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(this.notification?.parameters?.amount || 0);
    },
    userFullName() {
      return this.notification?.parameters?.userFullname;
    },
    userAvatarUrl() {
      return this.notification?.parameters?.avatar;
    },
    acceptUrl() {
      return this.notification?.parameters?.fundsAcceptUrl;
    },
    userUrl() {
      if (!this.notification?.parameters?.userUrl?.length) {
        return '#';
      }
      const userUrl = this.notification.parameters.userUrl.match(/http([^"]*)"/g)[0];
      return userUrl.substring(0, userUrl.length - 1);
    },
    message() {
      return this.$t('Notification.message.FundsRequestNotificationPlugin', {
        0: `<a class="space-name font-weight-bold">${this.userFullName}</a>`,
        1: this.tokenAmount,
        2: this.tokenSymbol,
      });
    },
  },
  methods: {
    refuse() {
      this.refusing = true;
      this.$notificationService.hideNotification(this.notificationId)
        .then(() => {
          this.$root.$emit('hide-notification', this.notificationId);
          this.hidden = true;
          document.dispatchEvent(new CustomEvent('refresh-notifications'));
        })
        .finally(() => this.refusing = false);
    },
  },
};
</script>