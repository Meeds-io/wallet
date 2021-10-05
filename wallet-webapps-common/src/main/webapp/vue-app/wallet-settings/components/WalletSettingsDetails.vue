<template>
  <v-card
    class="ma-4"
    flat>
    <v-toolbar
      class="border-box-sizing"
      flat>
      <v-btn
        class="mx-1"
        icon
        height="36"
        width="36"
        @click="$emit('back')">
        <v-icon size="20">
          {{ $vuetify.rtl && 'mdi-arrow-right' || 'mdi-arrow-left' }}
        </v-icon>
      </v-btn>
      <v-toolbar-title class="ps-0">
        {{ $t('exoplatform.wallet.label.settings') }}
      </v-toolbar-title>
      <v-spacer />
    </v-toolbar>
  </v-card>
</template>

<script>
export default {
};
</script>
