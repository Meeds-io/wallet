<template>
  <div id="walletAppMenu">
    <v-btn
      id="walletAppMenuRefreshButton"
      :title="$t('exoplatform.wallet.button.refreshWallet')"
      icon
      flat
      class="mr-0"
      @click="$emit('refresh')">
      <v-icon size="20px">
        refresh
      </v-icon>
    </v-btn>
    <v-btn
      v-if="!isSpace || isSpaceAdministrator"
      id="walletAppMenuSettingsButton"
      :title="$t('exoplatform.wallet.button.security')"
      class="mr-0 ml-0"
      icon
      flat
      @click="$emit('modify-settings')">
      <v-icon size="17px">
        fa-lock
      </v-icon>
    </v-btn>
  </div>
</template>

<script>
export default {
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isSpaceAdministrator: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
};
</script>
