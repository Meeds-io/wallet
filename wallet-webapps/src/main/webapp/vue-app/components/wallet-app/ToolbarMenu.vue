<template>
  <div id="walletAppMenu">
    <v-btn
      id="walletAppMenuRefreshButton"
      icon
      flat
      title="Refresh wallet"
      class="mr-0"
      @click="$emit('refresh')">
      <v-icon size="20px">
        refresh
      </v-icon>
    </v-btn>
    <v-btn
      v-if="!isSpace || isSpaceAdministrator"
      id="walletAppMenuSettingsButton"
      class="mr-0 ml-0"
      icon
      flat
      title="Settings"
      @click="$emit('modify-settings')">
      <v-icon size="17px">
        fa-cog
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
