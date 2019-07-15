<template>
  <v-layout
    mr-3
    ml-3
    pr-1>
    <v-flex
      md6
      text-xs-left
      pt-2
      class="periodicityLabel">
      {{ periodicityLabel }}
    </v-flex>
    <v-flex md6 text-xs-right>
      <v-btn-toggle v-model="periodicity" class="periodicityButtons">
        <v-btn
          :disabled="periodicity === 'year'"
          value="year"
          flat>
          {{ $t('exoplatform.wallet.chart.year') }}
        </v-btn>
        <v-btn
          :disabled="periodicity === 'month'"
          value="month"
          flat>
          {{ $t('exoplatform.wallet.chart.month') }}
        </v-btn>
      </v-btn-toggle>
    </v-flex>
  </v-layout>
</template>

<script>
export default {
  props: {
    periodicityLabel: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
 data() {
    return {
      periodicity: 'month',
    };
  },
  watch: {
    periodicity(newVal) {
      this.$emit('periodicity-changed', newVal);
    },
  },
}
</script>
