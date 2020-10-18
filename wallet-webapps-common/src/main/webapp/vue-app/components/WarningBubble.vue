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
  <information-bubble>
    <template slot="content">
      <slot name="bubble-content"></slot>
    </template>
    <v-btn
      :title="content"
      icon
      small
      class="mt-0 mb-2">
      <v-icon small color="orange">fa-exclamation-circle</v-icon>
    </v-btn>
  </information-bubble>
</template>

<script>
import InformationBubble from './InformationBubble.vue';

export default {
  components: {
    InformationBubble,
  },
  props: {
    content: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
};
</script>