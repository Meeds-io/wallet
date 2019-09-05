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