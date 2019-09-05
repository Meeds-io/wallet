<template>
  <div>
    <span>{{ title || $t('exoplatform.wallet.label.transactionFee') }}</span>
    <code v-if="estimatedFee" class="ml-2">{{ estimatedFee }}</code>
    <v-radio-group v-model="choice" :disabled="disabled">
      <v-radio :label="$t('exoplatform.wallet.label.transactionFeeCheap')" :value="1" />
      <v-radio :label="$t('exoplatform.wallet.label.transactionFeeNormal')" :value="2" />
      <v-radio :label="$t('exoplatform.wallet.label.transactionFeeFast')" :value="3" />
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
    title: {
      type: String,
      default: function() {
        return null;
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
      choice: 1,
    };
  },
  watch: {
    choice() {
      switch (this.choice) {
        case 1:
          this.$emit('changed', window.walletSettings.network.minGasPrice);
          break;
        case 2:
          this.$emit('changed', window.walletSettings.network.normalGasPrice);
          break;
        case 3:
          this.$emit('changed', window.walletSettings.network.maxGasPrice);
          break;
      }
    },
  },
};
</script>
