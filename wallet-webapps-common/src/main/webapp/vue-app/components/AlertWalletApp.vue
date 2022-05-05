<template>
  <v-alert
    v-model="displayAlert"
    :type="alertType"
    dismissible
    :icon="alertType === 'warning' ? 'mdi-alert-circle' : ''">
    <span v-sanitized-html="alertMessage" class="mt-8"> </span>
  </v-alert>
</template>
<script>
export default {
  data: () => ({
    displayAlert: false,
    alertMessage: null,
    alertType: null,
  }),
  created() {
    this.$root.$on('show-alert', alert => {
      this.alertMessage = alert.message;
      this.alertType = alert.type;
      this.displayAlert= true;
      window.setTimeout(() => this.displayAlert = false, 5000);
    });
  }
};
</script>