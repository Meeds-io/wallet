<template>
  <v-alert
    v-model="displayAlert"
    :type="alertType"
    class="paragraph"
    dismissible>
    <div v-sanitized-html="alertMessage" class="mt-8"></div>
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
      this.alertType = alert.type;
      this.alertMessage = alert.message;
      this.displayAlert= true;
      window.setTimeout(() => this.displayAlert = false, 5000); 
    });
  },
};
</script>