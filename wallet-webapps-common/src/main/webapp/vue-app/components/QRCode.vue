<template>
  <v-flex>
    <div v-if="information" class="alert alert-info">
      <i class="uiIconInfo"></i> {{ information }}
    </div>
    <div :id="id" class="text-center"></div>
  </v-flex>
</template>

<script>
export default {
  props: {
    netId: {
      type: String,
      default: function() {
        return null;
      },
    },
    information: {
      type: String,
      default: function() {
        return null;
      },
    },
    amount: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    from: {
      type: String,
      default: function() {
        return null;
      },
    },
    to: {
      type: String,
      default: function() {
        return null;
      },
    },
    isContract: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    functionName: {
      type: String,
      default: function() {
        return null;
      },
    },
    functionPayable: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    argsNames: {
      type: Array,
      default: function() {
        return [];
      },
    },
    argsTypes: {
      type: Array,
      default: function() {
        return [];
      },
    },
    argsValues: {
      type: Array,
      default: function() {
        return [];
      },
    },
  },
  data() {
    return {
      qr: null,
      id: null,
    };
  },
  watch: {
    id() {
      this.computeCanvas();
    },
  },
  created() {
    this.id = `QRCode${parseInt(Math.random() * 10000).toString()}`;
  },
  methods: {
    computeCanvas() {
      if (!this.qr) {
        this.qr = new window.EthereumQRPlugin();
      }
      const options = {};

      if (this.amount) {
        options.value = parseInt(window.localWeb3.utils.toWei(this.amount.toString(), 'ether'));
      }

      if (this.netId) {
        options.chainId = this.netId;
      }

      if (this.to) {
        options.to = this.to;
      }

      if (this.from) {
        options.from = this.from;
      }

      if ((window.walletSettings.network.gasLimit && this.isContract) || this.amount > 0) {
        options.gas = window.walletSettings.network.gasLimit;
      }

      if (this.isContract) {
        options.mode = 'contract_function';
        options.functionSignature = {};
        options.functionSignature.name = this.functionName;
        options.functionSignature.payable = this.functionPayable;
        if (this.argsNames.length === this.argsTypes.length && this.argsTypes.length === this.argsValues.length) {
          options.functionSignature.args = [];
          options.argsDefaults = [];

          for (let i = 0; i < this.argsNames.length; i++) {
            const argsName = this.argsNames[i];
            const argsType = this.argsTypes[i];
            const argsValue = this.argsValues[i];

            options.functionSignature.args.push({
              name: argsName,
              type: argsType,
            });
            options.argsDefaults.push({
              name: argsName,
              value: argsValue,
            });
          }
        }
      }

      return this.qr.toCanvas(options, {
          selector: `#${this.id}`,
        })
        .then((res, err) => {
          if (err) {
            console.error('qrCode', err);
          }
        });
    },
  },
};
</script>
