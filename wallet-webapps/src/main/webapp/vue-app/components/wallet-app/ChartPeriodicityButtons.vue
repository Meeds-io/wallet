<template>
  <v-layout>
    <v-flex md5 />
    <v-flex md2 pt-2>
      <v-input>
        {{ months[new Date().getMonth()] }}
      </v-input>
    </v-flex>
    <v-flex md5 text-xs-right>
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
