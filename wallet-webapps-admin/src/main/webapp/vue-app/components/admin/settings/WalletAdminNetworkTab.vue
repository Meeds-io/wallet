<template>
  <v-card class="text-xs-center pr-3 pl-3 pt-3" flat>
    <v-form ref="form">
      <v-flex id="selectedNetworkParent" class="selectBoxVuetifyParent">
        <v-combobox
          v-model="selectedNetwork"
          :items="networks"
          item-text="text"
          item-value="value"
          attach="#selectedNetworkParent"
          label="Select ethereum network"
          autofocus />
      </v-flex>
  
      <v-text-field
        v-if="showSpecificNetworkFields"
        v-model="selectedNetwork.value"
        :rules="mandatoryRule"
        :label="`Ethereum Network ID ${currentNetworkIdLabel}`"
        type="number"
        name="defaultNetworkId" />
  
      <v-text-field
        v-if="showSpecificNetworkFields"
        ref="providerURL"
        v-model="selectedNetwork.httpLink"
        :rules="mandatoryRule"
        type="text"
        name="providerURL"
        label="Ethereum Network HTTP URL used for static displaying spaces wallets (without Metamask)" />
  
      <v-text-field
        v-if="showSpecificNetworkFields"
        ref="websocketProviderURL"
        v-model="selectedNetwork.wsLink"
        type="text"
        name="websocketProviderURL"
        label="Ethereum Network Websocket URL used for notifications" />
  
      <v-slider
        v-model="defaultGas"
        :label="`Maximum gas: ${defaultGas}`"
        :min="35000"
        :max="200000"
        :step="1000"
        type="number"
        class="mt-4"
        required />
  
      <v-slider
        v-model="minGasPrice"
        :label="`Cheap transaction gas price: ${minGasPrice} Gwei ${minGasFiatPrice}`"
        :min="1"
        :max="20"
        :step="1"
        type="number"
        class="mt-4"
        required />
  
      <v-slider
        v-model="normalGasPrice"
        :label="`Normal transaction gas price: ${normalGasPrice} Gwei ${normalGasFiatPrice}`"
        :min="1"
        :max="20"
        :step="1"
        type="number"
        class="mt-4"
        required />
  
      <v-slider
        v-model="maxGasPrice"
        :label="` Fast transaction gas price: ${maxGasPrice} Gwei ${maxGasFiatPrice}`"
        :min="1"
        :max="20"
        :step="1"
        type="number"
        class="mt-4"
        required />
    </v-form>

    <v-card-actions>
      <v-spacer />
      <button class="btn btn-primary mb-3" @click="save">
        Save
      </button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>
<script>

export default {
  props: {
    networkId: {
      type: String,
      default: function() {
        return null;
      },
    },
    defaultNetworkId: {
      type: String,
      default: function() {
        return null;
      },
    },
    loading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    principalContract: {
      type: Object,
      default: function() {
        return null;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      defaultGas: 150000,
      minGasPrice: null,
      normalGasPrice: null,
      maxGasPrice: null,
      enableDelegation: true,
      mandatoryRule: [(v) => !!v || 'Field is required'],
      selectedNetwork: {
        text: '',
        value: '',
      },
      networks: [
        {
          text: 'Ethereum Main Network',
          value: 1,
          wsLink: 'wss://mainnet.infura.io/ws',
          httpLink: 'https://mainnet.infura.io',
        },
        {
          text: 'Ropsten',
          value: 3,
          wsLink: 'wss://ropsten.infura.io/ws',
          httpLink: 'https://ropsten.infura.io',
        },
        {
          text: 'Other',
          value: 0,
          wsLink: 'ws://127.0.0.1:8545',
          httpLink: 'http://127.0.0.1:8545',
        },
      ],
    };
  },
  computed: {
    currentNetworkIdLabel() {
      return this.networkId ? `(current network id: ${this.networkId})` : ''
    },
    showSpecificNetworkFields() {
      return this.selectedNetwork !== this.networks[0] && this.selectedNetwork !== this.networks[1];
    },
    minGasFiatPrice() {
      const amount = this.defaultGas && this.walletUtils.gasToFiat(this.defaultGas, this.gweiToEther(this.minGasPrice));
      return amount ? `${this.toFixed(amount)} ${this.fiatSymbol}` : '';
    },
    normalGasFiatPrice() {
      const amount = this.defaultGas && this.walletUtils.gasToFiat(this.defaultGas, this.gweiToEther(this.normalGasPrice));
      return amount ? `${this.toFixed(amount)} ${this.fiatSymbol}` : '';
    },
    maxGasFiatPrice() {
      const amount = this.defaultGas && this.walletUtils.gasToFiat(this.defaultGas, this.gweiToEther(this.maxGasPrice));
      return amount ? `${this.toFixed(amount)} ${this.fiatSymbol}` : '';
    },
  },
  methods: {
    init() {
      if (window.walletSettings.defaultGas) {
        this.defaultGas = window.walletSettings.defaultGas;
      }
      if (window.walletSettings.minGasPrice) {
        this.minGasPrice = this.weiToGwei(window.walletSettings.minGasPrice);
      }
      if (window.walletSettings.normalGasPrice) {
        this.normalGasPrice = this.weiToGwei(window.walletSettings.normalGasPrice);
      }
      if (window.walletSettings.maxGasPrice) {
        this.maxGasPrice = this.weiToGwei(window.walletSettings.maxGasPrice);
      }
      this.enableDelegation = window.walletSettings.enableDelegation;
      if (window.walletSettings.defaultNetworkId === this.networks[0].value
          && window.walletSettings.providerURL === this.networks[0].httpLink
          && window.walletSettings.websocketProviderURL === this.networks[0].wsLink) {
        this.selectedNetwork = this.networks[0];
      } else if (window.walletSettings.defaultNetworkId === this.networks[1].value
          && window.walletSettings.providerURL === this.networks[1].httpLink
          && window.walletSettings.websocketProviderURL === this.networks[1].wsLink) {
        this.selectedNetwork = this.networks[1];
      } else {
        this.networks[2].value = window.walletSettings.defaultNetworkId;
        this.networks[2].wsLink = window.walletSettings.websocketProviderURL;
        this.networks[2].httpLink = window.walletSettings.providerURL;
        this.selectedNetwork = this.networks[2];
      }
    },
    save() {
      if (!this.$refs.form.validate()) {
        return;
      }
      const globalSettings = {
        providerURL: this.selectedNetwork.httpLink,
        websocketProviderURL: this.selectedNetwork.wsLink,
        defaultNetworkId: this.selectedNetwork.value,
        defaultGas: this.defaultGas,
        minGasPrice: this.gweiToWei(this.minGasPrice),
        normalGasPrice: this.gweiToWei(this.normalGasPrice),
        maxGasPrice: this.gweiToWei(this.maxGasPrice),
        enableDelegation: this.enableDelegation,
      };
      this.$emit('save', globalSettings);
    },
    gweiToWei(value) {
      return value * 1000000000;
    },
    gweiToEther(value) {
      return value / 1000000000;
    },
    weiToGwei(value) {
      return value / 1000000000;
    },
  },
};
</script>
