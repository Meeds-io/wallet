<template>
  <div>
    <span>{{ title || $t('exoplatform.wallet.label.transactionFee') }}</span>
    <code v-if="estimatedFee" class="ml-2">{{ estimatedFee }}</code>
    <v-slider
      v-if="slider"
      v-model="gasPrice"
      :min="minGasPrice"
      :max="maxGasPrice"
      thumb-size="25" />
    <v-radio-group
      v-else
      v-model="choice"
      :disabled="disabled">
      <v-radio
        :disabled="choice1Disabled"
        :label="$t('exoplatform.wallet.label.transactionFeeCheap')"
        :value="1" />
      <v-radio
        :disabled="choice2Disabled"
        :label="normalChoiceLabel"
        :value="2" />
      <v-radio
        :disabled="choice3Disabled"
        :label="$t('exoplatform.wallet.label.transactionFeeFast')"
        :value="3" />
    </v-radio-group>
  </div>
</template>
<script>
export default {
  props: {
    estimatedFee: {
      type: String,
      default: function() {
        return null;
      },
    },
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    title: {
      type: String,
      default: function() {
        return null;
      },
    },
    slider: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    minGasPrice: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    disabled: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      gasPrice: 0,
      maxGasPrice: 0,
      choice: 2,
      choice1Disabled: false,
      choice2Disabled: false,
      choice3Disabled: false,
    };
  },
  computed: {
    normalChoiceLabel() {
      return window.walletSettings.useDynamicGasPrice ?
          this.$t('exoplatform.wallet.label.transactionFeeDynamic'):
             this.$t('exoplatform.wallet.label.transactionFeeNormal');
    },
  },
  watch: {
    choice() {
      this.$emit('changed', this.getGasPrice());
    },
    gasPrice() {
      this.$emit('changed', this.gasPrice);
    },
  },
  created() {
    const network = window.walletSettings && window.walletSettings.network;
    
    if (network && this.wallet && this.wallet.etherBalance) {
      network.minGasPriceEther = network.minGasPriceEther || window.localWeb3.utils.fromWei(String(network.minGasPrice * Number(network.gasLimit)), 'ether').toString();
      network.normalGasPriceEther = network.normalGasPriceEther || window.localWeb3.utils.fromWei(String(network.normalGasPrice * Number(network.gasLimit)), 'ether').toString();
      network.maxGasPriceEther = network.maxGasPriceEther || window.localWeb3.utils.fromWei(String(network.maxGasPrice * Number(network.gasLimit)), 'ether').toString();

      if (this.slider) {
        this.gasPrice = this.minGasPrice;
        this.maxGasPrice = network.maxGasPrice;

        if (this.wallet.etherBalance <= network.minGasPriceEther) {
          this.maxGasPrice = this.gasPrice;
        } else if (this.wallet.etherBalance < network.maxGasPriceEther) {
          const weiBalanceBN = LocalWeb3.utils.toBN(LocalWeb3.utils.toWei(String(this.wallet.etherBalance)));
          const gasLimitBN = LocalWeb3.utils.toBN(String(network.gasLimit));
          this.maxGasPrice = weiBalanceBN.div(gasLimitBN).toNumber();
        }
      } else {
        this.choice1Disabled = this.wallet.etherBalance < network.minGasPriceEther;
        this.choice2Disabled = this.wallet.etherBalance < network.normalGasPriceEther;
        this.choice3Disabled = this.wallet.etherBalance < network.maxGasPriceEther;

        if (this.choice1Disabled) {
          this.choice = 0;
        } else if (this.choice2Disabled) {
          this.choice = 1;
        }
      }
    }
    this.$emit('changed', this.getGasPrice());
  },
  methods: {
    getGasPrice() {
      switch (this.choice) {
        case 1:
          return window.walletSettings.network.minGasPrice;
        case 2:
          return window.walletSettings.network.normalGasPrice;
        case 3:
          return window.walletSettings.network.maxGasPrice;
        default:
          return 0;
      }
    }
  },
};
</script>
