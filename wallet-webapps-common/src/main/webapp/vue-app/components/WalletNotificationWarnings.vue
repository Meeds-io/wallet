
<template>
  <v-container>
    <div class="d-flex justify-space-between ">
      <div class="me-2">
        <v-icon color="warning">mdi-alert-circle</v-icon>
      </div>
      <div>
        <p class="font-weight-medium">
          {{ alertMessage }}
        </p>
      </div>
      <div class="align-self-center">
        <v-btn
          width="145px"
          class="btn btn-primary"
          @click="switchActions">
          {{ alertButtonMessage }}
        </v-btn>
      </div>
    </div>
  </v-container>
</template>
<script>
export default {
  props: {
    alertMessage: {
      type: String,
      default: () => '',
    },    
    alertSwitchMetamaskActions: {
      type: String,
      default: () => '',
    },
    alertButtonMessage: {
      type: String,
      default: () => '',
    },
  },
  data() {
    return {
    };
  },
  methods: {
    switchActions(){
      switch (this.alertSwitchMetamaskActions){
      case 'changeNetwork': 
        this.walletUtils.switchMetamaskNetwork('0x89');
        break;
      case 'changeAccount':
        this.walletUtils.selectSuitableAccount();
        break;
      }
    }
  }
};
</script>