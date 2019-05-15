<template>
  <v-tooltip bottom>
    <information-bubble slot="activator">
      <template slot="content">
        <slot name="bubble-content"></slot>
      </template>
      <v-btn
        icon
        small
        class="mt-0 mb-2">
        <v-icon small color="orange">fa-exclamation-circle</v-icon>
      </v-btn>
    </information-bubble>
    <span>
      <slot name="content"></slot>
    </span>
  </v-tooltip>
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