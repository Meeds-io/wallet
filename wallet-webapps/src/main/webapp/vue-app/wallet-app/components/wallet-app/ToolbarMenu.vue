<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <div id="walletAppMenu">
    <v-btn
      id="walletAppMenuSettingsButton"
      :title="$t('exoplatform.wallet.button.security')"
      class="me-0 ms-0"
      icon
      text
      @click="openSettings()">
      <v-icon size="24">
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
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  methods: {
    openSettings() {
      if (this.wallet.type === 'space'){
        return window.location.href = `${eXo.env.portal.context}/g/:spaces:${eXo.env.portal.spaceGroup}/${eXo.env.portal.spaceName}/settings?walletSetting`;
      } else {
        return window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/settings?walletSetting`;
      }
    }
  }
};
</script>
