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