<template>
  <v-flex>
    <div v-if="information" class="alert alert-info">
      <i class="uiIconInfo"></i> {{ information }}
    </div> <div :id="id" class="text-xs-center"></div>
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
      id: `QRCode${parseInt(Math.random() * 10000)
        .toString()
        .toString()}`,
    };
  },
  methods: {
    computeCanvas() {
      window.localWeb3.eth.net
        .getId()
        .then((netId) => {
          // This promise is triggered multiple times
          if (this.to && netId !== this.netId) {
            this.netId = netId;
            const qr = new window.EthereumQRPlugin();
            const options = {
              chainId: netId,
              to: this.to,
              value: parseInt(window.localWeb3.utils.toWei(this.amount.toString(), 'ether')),
            };

            if (this.from) {
              options.from = this.from;
            }

            if ((window.walletSettings.userPreferences.defaultGas && this.isContract) || this.amount > 0) {
              options.gas = window.walletSettings.userPreferences.defaultGas;
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

            return qr
              .toCanvas(options, {
                selector: `#${this.id}`,
              })
              .then((res, err) => {
                if (err) {
                  console.error('qrCode', err);
                }
              });
          }
        })
        .catch((error) => {
          console.debug('Error while generating qr code', error);
        });
    },
  },
};
</script>
