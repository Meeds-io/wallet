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
      v-if="isMaximized && (!isSpace || isSpaceAdministrator)"
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
    <v-btn
      v-if="!isMaximized"
      id="walletAppMenuMaximizeButton"
      icon
      title="Open wallet application"
      class="maximizeIcon ml-0"
      @click="$emit('maximize')">
      <v-icon size="15px" class="arrow-up-right">
        fa-arrow-up
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
    isMaximized: {
      type: Boolean,
      default: function() {
        return true;
      },
    },
  },
};
</script>
