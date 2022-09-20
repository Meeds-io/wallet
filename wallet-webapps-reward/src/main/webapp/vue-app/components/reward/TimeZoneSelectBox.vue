<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2022 Meeds Association
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
  <v-autocomplete
    id="rewardSettingTimeZone"
    v-model="timeZone"
    ref="selectAutoComplete"
    :items="timeZones"
    :disabled="disabled"
    attach
    hide-no-data
    class="subtitle-1 pa-0"
    dense
    filled
    @blur="$refs.selectAutoComplete.isFocused = false" />
</template>

<script>
export default {
  props: {
    value: {
      type: String,
      default: null,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      timeZone: null,
      timeZones: [],
    };
  },
  watch: {
    timeZone() {
      this.$emit('input', this.timeZone);
    },
    value() {
      this.timeZone = this.value;
    },
  },
  created() {
    this.timeZone = this.value;
    this.timeZones = this.$rewardService.getTimeZones();
  },
};
</script>