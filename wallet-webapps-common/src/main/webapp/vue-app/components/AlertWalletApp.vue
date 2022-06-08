<template>
  <v-alert
    class="paragraph"
    v-model="displayAlert"
    :type="alertType"
    dismissible>
    <div>
      <span v-sanitized-html="alertMessage" class="mt-8"> </span>
    </div>
    <div>
      <v-btn 
        v-show="alertshouldSwitchNetwork"
        @click="switchMetamaskActions()"
        name="switchMetamaskNetworkButton"
        icon
        tile>
        <img
          src="/wallet-common/images/metamask.svg"
          alt="Metamask"
          width="18"
          class="pr-2 pl-1">
      </v-btn>
    </div>
  </v-alert>
</template>
<script>
export default {
  data: () => ({
    displayAlert: false,
    alertMessage: null,
    alertType: null,
    alertshouldSwitchNetwork: false,
    changedToNeededNetwork: false,
    //should change PolygonChainId to 0x89 to match the polygon chainId
    //PolygonChainId: '0x13881' is used for tests purposes to match mumbai
    PolygonChainId: '0x13881'
  }),
  created() {
    this.$root.$on('show-alert', alert => {
      this.alertshouldSwitchNetwork = alert.shouldSwitchNetwork;
      this.alertMessage = alert.message;
      this.alertType = alert.type;
      this.displayAlert= true;
      if (this.isSameNetwork()){
        window.setTimeout(() => this.displayAlert = false, 5000); 
      } else {
        this.displayAlert = this.closeAlert();
      }
    });
  },
  methods: {
    switchMetamaskActions() {
      if (window.ethereum?.selectedAddress && window.ethereum?.selectedAddress === window.walletSettings?.wallet?.address){
        this.tokenUtils.switchMetamaskNetwork(this.PolygonChainId);
      } else {
        this.tokenUtils.selectSuitableAccount();
      }
    },
    isSameNetwork(){
      return parseInt(window.ethereum?.networkVersion) === window.walletSettings?.network?.id;
    },
    closeAlert: async () => {
      const networkChanged = await (this.tokenUtils.onNetworkChangeToPolygon());
      if (networkChanged){
        return false;
      }
    }
  }
};
</script>