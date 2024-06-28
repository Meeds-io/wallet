<!--

  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2023 Meeds Association contact@meeds.io

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
  <v-app flat>
    <div class="px-6">
      <v-list-item class="px-0" two-line dense>
        <v-list-item-content>
          <v-list-item-title class="text-title">
            {{ $t('wallet.administration.title') }}
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ $t('wallet.administration.subtitle') }}
          </v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-btn
            icon
            @click="$refs.adminDrawer.open()">
            <v-icon size="18" class="icon-default-color">fa-edit</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
    </div>
    <wallet-admin-drawer ref="adminDrawer" />
  </v-app>
</template>
<script>
export default {
  mounted() {
    this.$root.$applicationLoaded();
  },
};
</script>
