<template>
  <v-avatar size="16px" class="whiteBackground">
    <svg :data-jdenticon-value="address" ref="identicon"></svg>
  </v-avatar>
</template>
<script>
export default {
  props: {
    address: {
      type: String,
      default: null,
    },
  },
  mounted() {
    window.jdenticon.updateSvg(this.$refs.identicon);
  },
};
</script>