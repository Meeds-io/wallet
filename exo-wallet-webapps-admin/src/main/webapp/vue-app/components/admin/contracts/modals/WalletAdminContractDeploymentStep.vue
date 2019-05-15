<template>
  <v-card flat>
    <v-card-title v-if="processing">
      Operation in progress... <a
        v-if="transactionHash"
        :href="transactionEtherscanLink"
        target="_blank">
        See it on etherscan
      </a>
    </v-card-title>
    <v-card-title v-else-if="contractAddress" class="alert alert-success">
      <i class="uiIconSuccess"></i> Contract created under address:
      <wallet-address :value="contractAddress" />
      <a :href="tokenEtherscanLink" target="_blank">
        See it on etherscan
      </a>
    </v-card-title>
    <v-card-title v-else-if="processed" class="alert alert-success">
      <i class="uiIconSuccess"></i> Transaction processed successfully. <a
        v-if="transactionHash"
        :href="transactionEtherscanLink"
        target="_blank">
        See it on etherscan
      </a>
    </v-card-title>
    <v-card-text v-else>
      <div v-if="transactionFee">
        Estimated deployment gas: <code>{{ gas }} gas ({{ toFixed(transactionFee) }} {{ fiatSymbol }})</code>
      </div>
      <slot></slot> <h4 v-if="!storedPassword">
        Your wallet password
      </h4>
      <v-text-field
        v-if="!storedPassword"
        v-model="walletPassword"
        :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
        :type="walletPasswordShow ? 'text' : 'password'"
        :disabled="processing"
        name="walletPassword"
        placeholder="Enter your wallet password"
        counter
        autocomplete="current-passord"
        @click:append="walletPasswordShow = !walletPasswordShow" />
    </v-card-text>
    <v-card-actions>
      <v-btn
        v-if="contractAddress || processed"
        color="primary"
        @click="$emit('next')">
        Next
      </v-btn>
      <v-btn
        v-else
        :disabled="disabledButton"
        :loading="processing"
        color="primary"
        @click="$emit('proceed', walletPassword)">
        {{ buttonTitle }}
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>

export default {
  props: {
    networkId: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    contractAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    buttonTitle: {
      type: String,
      default: function() {
        return null;
      },
    },
    gas: {
      type: String,
      default: function() {
        return null;
      },
    },
    processed: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    storedPassword: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    processing: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    disabledButton: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      },
    },
    transactionHash: {
      type: String,
      default: function() {
        return null;
      },
    },
    transactionFee: {
      type: Number,
      default: function() {
        return 0;
      },
    },
  },
  data() {
    return {
      walletPassword: '',
      walletPasswordShow: false,
      mandatoryRule: [(v) => !!v || 'Field is required'],
    };
  },
  computed: {
    tokenEtherscanLink() {
      return this.contractAddress ? `${this.walletUtils.getAddressEtherscanlink(this.networkId)}${this.contractAddress}` : null;
    },
    transactionEtherscanLink() {
      return this.transactionHash ? `${this.walletUtils.getTransactionEtherscanlink(this.networkId)}${this.transactionHash}` : null;
    },
  },
};
</script>
