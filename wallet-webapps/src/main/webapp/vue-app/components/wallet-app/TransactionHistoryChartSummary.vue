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
      <v-btn-toggle v-model="periodicity">
        <v-btn
          id="periodicityChangeYear"
          :disabled="periodicity === 'year'"
          value="year"
          class="btn py-2 px-2">
          Year
        </v-btn>
        <v-btn
          id="periodicityChangeMonth"
          :disabled="periodicity === 'month'"
          value="month"
          class="btn py-2 px-2">
          Month
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
      months: ['January','February','March','April','May','June','July','August','September','October','November','December'],
    };
  },
  watch: {
    periodicity(newVal) {
      this.$emit('periodicity-changed', newVal);
    },
  },
}
</script>
