<template>
  <div class="mt-3">
    <span>{{ title ? title : 'Transaction fee' }}</span>
    <code v-if="estimatedFee" class="ml-2">{{ estimatedFee }}</code>
    <v-radio-group v-model="choice" :disabled="disabled">
      <v-radio label="Cheap (could take 1 day)" value="1" />
      <v-radio label="Normal (could take few hours)" value="2" />
      <v-radio label="Fast (about 1 minute)" value="3" />
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
      choice: '1',
    };
  },
  watch: {
    choice() {
      let gasPrice = window.walletSettings.minGasPrice;
      switch (Number(this.choice)) {
        case 2:
          gasPrice = window.walletSettings.normalGasPrice;
          break;
        case 3:
          gasPrice = window.walletSettings.maxGasPrice;
          break;
      }
      this.$emit('changed', gasPrice);
    },
  },
};
</script>
