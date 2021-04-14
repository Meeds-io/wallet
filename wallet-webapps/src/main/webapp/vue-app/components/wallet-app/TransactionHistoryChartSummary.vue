<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <v-layout
    me-3
    ms-3
    pe-1>
    <v-flex
      md6
      text-start
      pt-2
      class="periodicityLabel">
      <v-menu
        ref="selectDateMenu"
        v-model="selectDateMenu"
        transition="scale-transition"
        offset-y
        class="dateSelector">
        <template v-slot:activator="{ on }">
          <v-chip color="primary" @click="openDatePicker">
            <v-icon class="me-1">event</v-icon>
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
          class="border-box-sizing"
          min="2018"
          @input="selectPeriod"
          @update:picker-date="selectPeriod" />
      </v-menu>
    </v-flex>
    <v-flex md6 text-end>
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
    periodicity() {
      this.selectedPickerDate = this.selectedDate = new Date().toISOString().substr(0, 7);
      this.updateDatePickerSelection();
      this.$emit('period-changed', this.periodicity, this.selectedDate);
    },
    selectedDate() {
      this.$emit('period-changed', this.periodicity, this.selectedDate);
    },
  },
  created() {
    this.lang = eXo.env.portal.language;
  },
  methods: {
    updateDatePickerSelection() {
      if (this.periodicity === 'month') {
        if (this.$refs.datePicker) {
          this.$refs.datePicker.activePicker = 'MONTH';
        }
      } else if (this.periodicity === 'year') {
        if (this.$refs.datePicker) {
          this.$refs.datePicker.activePicker = 'YEAR';
        }
      }
    },
    openDatePicker() {
      this.updateDatePickerSelection();
      this.selectDateMenu = true;
    },
    selectPeriod(date) {
      if (!date || this.selectedDate === date) {
        return;
      }

      if (this.periodicity === 'month') {
        if (date.indexOf('undefined') >= 0 || date.indexOf('Na') >= 0 || date.length < 7) {
          return;
        }
        this.selectedDate = date;
      } else if (this.periodicity === 'year') {
        if (date.length < 4) {
          return;
        }
        this.selectedDate = `${date.substring(0, 4)}-01`;
      }
    },
  },
};
</script>
