
<template>
  <v-alert
    v-show="showWarning"
    height="120"
    type="warning"
    class="position-relative"
    width="auto"
    text
    outlined
    dismissible
    color="deep-orange"
    style="left: 0px;right: 0px;top: 0px;border-right-width: 0px;"
    icon="mdi-warning-circle">
    <v-row>
      <v-col style="flex: auto;">
        {{ alertMessage }}
      </v-col>
      <v-col>
        <v-btn
          style="flex: auto;"
          centered
          elevation="2"
          fab
          small
          text
          @click="switchActions()">
          <img
            class="mt-n1"
            :src="`/wallet-common/images/metamask.svg`"
            alt="Metamask"
            width="18">      
        </v-btn>
      </v-col>
    </v-row>
  </v-alert>
</template>
<script>
export default {
  data: () => ({
    showWarning: false,
    displayAlert: false,
    alertSwitchMetamaskActions: null
  }),
  created() {
    window.ethereum.on('accountsChanged' , () => {
      if (window.ethereum?.selectedAddress && window.ethereum?.selectedAddress === window.walletSettings?.wallet?.address){
        this.showWarning = false;
      }
    });

    window.ethereum.on('chainChanged', () => {
      this.showWarning = false;
    });

    
    this.$root.$on('show-warning', alert => {
      this.showWarning = true;
      this.alertMessage = alert.message;
      this.alertSwitchMetamaskActions = alert.switchMetamaskActions;
    });
  },

  methods: {
    switchActions(){
      if (this.alertSwitchMetamaskActions === 'changeNetwork') {
        this.walletUtils.switchMetamaskNetwork('0x13881');
      } else if ( this.alertSwitchMetamaskActions === 'changeAccount') {
        this.walletUtils.selectSuitableAccount();
      }
    }
  }
};
</script>