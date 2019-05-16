<template>
  <v-dialog
    v-model="dialog"
    :content-class="title && 'uiPopup'"
    hide-overlay
    width="300px"
    max-width="100vw"
    @keydown.esc="dialog = false">
    <template slot="activator">
      <slot></slot>
    </template>
    <v-card class="elevation-12">
      <div v-if="title" class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
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
