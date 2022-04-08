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
  <v-dialog
    v-model="dialog"
    :content-class="title ? 'uiPopup walletDialog' : 'walletDialog'"
    hide-overlay
    width="300px"
    max-width="100vw"
    @keydown.esc="dialog = false">
    <template #activator>
      <slot></slot>
    </template>
    <v-card class="elevation-12">
      <div v-if="title" class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="ignore-vuetify-classes PopupTitle popupTitle">
            {{ title }}
          </span>
      </div>
      <v-card-text>
        <slot name="content"></slot>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: {
    title: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      dialog: false,
    };
  },
};
</script>
