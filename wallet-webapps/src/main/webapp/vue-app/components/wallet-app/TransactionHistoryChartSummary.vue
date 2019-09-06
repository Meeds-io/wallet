<template>
  <v-layout
    mr-3
    ml-3
    pr-1>
    <v-flex
      md6
      text-left
      pt-2
      class="periodicityLabel">
      <v-menu
        ref="selectDateMenu"
        v-model="selectDateMenu"
        transition="scale-transition"
        offset-y
        class="dateSelector">
        <template v-slot:activator="{ on }">
          <v-chip color="primary" @click="selectDateMenu = true">
            <v-icon class="mr-1">event</v-icon>
            {{ periodicityLabel }}
          </v-chip>
        </template>
        <v-date-picker
          ref="datePicker"
          v-model="selectedPickerDate"
          :type="periodicity"
          :locale="lang"
          :show-current="false"
          :max="maxDate"
          min="2018"
          @input="selectPeriod"
          @update:picker-date="selectPeriod" />
      </v-menu>
    </v-flex>
    <v-flex md6 text-right>
      <v-btn-toggle v-model="periodicity" class="periodicityButtons elevation-1">
        <v-btn
          :disabled="periodicity === 'year'"
          value="year"
          text>
          {{ $t('exoplatform.wallet.chart.year') }}
        </v-btn>
        <v-btn
          :disabled="periodicity === 'month'"
          value="month"
          text>
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
      selectDateMenu: false,
      selectedPickerDate: new Date().toISOString().substr(0, 7),
      selectedDate: null,
      lang: 'en',
      periodicity: 'month',
    };
  },
  computed: {
    maxDate() {
      return this.periodicity === 'year' && new Date().toISOString().substr(0, 4);
    }
  },
  watch: {
    periodicity(newVal) {
      this.selectedDate = new Date().toISOString().substr(0, 7);
      this.$emit('period-changed', this.periodicity, this.selectedDate);
    },
    selectedDate(newVal) {
      this.$emit('period-changed', this.periodicity, this.selectedDate);
    },
  },
  created() {
    this.lang = eXo.env.portal.language;
  },
  methods: {
    selectPeriod(date) {
      if (!date || date.indexOf('undefined') >= 0 || date.indexOf('Na') >= 0 || this.selectedDate === date || (this.periodicity === 'month' && date.length < 7)) {
        return;
      } else if (this.periodicity === 'year') {
        if (date.length > 4) {
          date = date.substring(0, 4);
        }
        this.selectedDate = `${date}-01`;

        if (this.$refs.datePicker) {
          this.$refs.datePicker.activePicker = 'YEAR';
        }
      } else {
        this.selectedDate = date;
      }
    },
  },
}
</script>
