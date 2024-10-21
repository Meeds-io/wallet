<!--
  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io

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
  <exo-drawer
    ref="drawer"
    v-model="drawer"
    :loading="loading"
    allow-expand
    right>
    <template #title>
      {{ $t('wallet.administration.budgetConfigurationDrawer.title') }}
    </template>
    <template #content>
      <v-card-title class="pb-2">
        {{ $t('wallet.administration.budgetConfigurationDrawer.periodType') }}
      </v-card-title>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="text-wrap">
            {{ $t('wallet.administration.budgetConfigurationDrawer.periodType.title') }}
          </v-list-item-title>
        </v-list-item-content>
        <v-list-item-action class="d-inline-block ma-auto pe-1">
          <v-card
            flat
            width="125">
            <v-select
              v-model="settingsToSave.periodType"
              :items="periods"
              class="pa-0"
              item-text="text"
              item-value="value"
              hide-details
              outlined
              dense />
          </v-card>
        </v-list-item-action>
      </v-list-item>
      <v-card-title class="pb-2">
        {{ $t('wallet.administration.budgetConfigurationDrawer.timeZone') }}
      </v-card-title>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="text-wrap">
            {{ $t('wallet.administration.budgetConfigurationDrawer.timeZone.title') }}
          </v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <wallet-reward-timezone-selectbox class="px-4" v-model="settingsToSave.timeZone" />
      <v-card-title class="pb-2">
        {{ $t('wallet.administration.budgetConfigurationDrawer.threshold.title') }}
      </v-card-title>
      <div class="d-flex align-center px-4">
        <number-input
          v-model="settingsToSave.threshold"
          :step="1"
          :max="1000"
          editable
          class="me-n1 pa-0 ma-0"
          @valid="invalidThreshold = !$event" />
        <div class="ps-4">{{ $t('wallet.administration.budgetConfiguration.points') }}</div>
      </div>
      <span v-if="invalidThreshold" class="error-color d-flex px-4">{{ $t('wallet.administration.budgetConfigurationDrawer.invalidThreshold', {
        0: maxValueFormatted(1000),
      }) }}</span>
      <v-card-title class="pb-2">
        {{ $t('wallet.administration.budgetConfigurationDrawer.amount.title') }}
      </v-card-title>
      <v-radio-group v-model="settingsToSave.budgetType" class="my-auto px-4">
        <v-radio
          :label="$t('wallet.administration.budgetConfigurationDrawer.budgetFixed')"
          value="FIXED"
          class="mx-0" />
        <v-radio
          :label="$t('wallet.administration.budgetConfigurationDrawer.budgetPerContributor')"
          value="FIXED_PER_MEMBER"
          class="mx-0" />
      </v-radio-group>
      <div class="d-flex align-center px-4">
        <number-input
          v-model="settingsToSave.amount"
          :step="1"
          :max="5000"
          editable
          class="me-n1 my-0 pa-0"
          @valid="invalidAmount = !$event" />
        <div class="ps-4">Meeds</div>
      </div>
      <span v-if="invalidAmount" class="error-color d-flex px-4">{{ $t('wallet.administration.budgetConfigurationDrawer.invalidAmount', {
        0: maxValueFormatted(5000),
      }) }}</span>
      <wallet-budget-distribution-forecast
        v-if="distributionForecast"
        :distribution-forecast="distributionForecast"
        :loading-forecast="loadingForecast" />
    </template>
    <template #footer>
      <div class="d-flex">
        <v-spacer />
        <v-btn
          class="btn me-2"
          @click="close">
          {{ $t('exoplatform.wallet.button.cancel') }}
        </v-btn>
        <v-btn
          :disabled="disableApply"
          class="btn btn-primary"
          @click="apply">
          {{ $t('wallet.administration.apply') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>

<script>

export default {
  props: {
    rewardSettings: {
      type: Object,
      default: null
    },
    rewardReport: {
      type: Object,
      default: null
    },
  },
  data: () => ({
    drawer: false,
    settingsToSave: {},
    loading: false,
    invalidThreshold: false,
    invalidAmount: false,
    distributionForecast: null,
    loadingForecast: false,
    timeoutId: null
  }),
  computed: {
    hasConfiguredBudget() {
      return this.rewardSettings?.storedSetting;
    },
    periodType() {
      return this.rewardSettings?.periodType;
    },
    budgetType() {
      return this.rewardSettings?.budgetType;
    },
    timeZone() {
      return this.rewardSettings?.timeZone;
    },
    amount() {
      return this.rewardSettings?.amount;
    },
    threshold() {
      return this.rewardSettings?.threshold;
    },
    configurationChanged() {
      return this.amount !== this.settingsToSave.amount
          || this.budgetType !== this.settingsToSave.budgetType
          || this.threshold !== this.settingsToSave.threshold
          || this.timeZone !== this.settingsToSave?.timeZone
          || this.timeZone !== this.settingsToSave?.timeZone
          || this.periodType !== (this.settingsToSave?.periodType?.value || this.settingsToSave?.periodType);
    },
    disableApply() {
      return this.invalidThreshold || this.invalidAmount || !this.settingsToSave?.timeZone || !this.configurationChanged;
    },
    periods() {
      return [
        {
          text: this.$t('wallet.administration.periodType.label.week'),
          value: 'WEEK',
        },
        {
          text: this.$t('wallet.administration.periodType.label.month'),
          value: 'MONTH',
        },
        {
          text: this.$t('wallet.administration.periodType.label.quarter'),
          value: 'QUARTER',
        },
      ];
    },
  },
  watch: {
    settingsToSave: {
      handler() {
        if (this.timeoutId) {
          clearTimeout(this.timeoutId);
        }
        if (!this.disableApply) {
          this.timeoutId = setTimeout(() => {
            this.computeDistributionForecast();
          }, 1000);
        }
      },
      deep: true,
    },
  },
  methods: {
    open() {
      if (this.rewardSettings) {
        this.settingsToSave = JSON.parse(JSON.stringify(this.rewardSettings));
        this.settingsToSave.periodType = this.hasConfiguredBudget ? this.periods.find(period => period.value === this.settingsToSave.periodType) : this.periods.find(p => p.value === 'WEEK');
        this.settingsToSave.amount = this.hasConfiguredBudget ? this.settingsToSave?.amount : 1000;
        this.settingsToSave.threshold = this.hasConfiguredBudget ? this.settingsToSave?.threshold : 50;
        this.settingsToSave.budgetType = this.hasConfiguredBudget ? this.settingsToSave?.budgetType : 'FIXED';
        this.computeDistributionForecast();
      } else {
        this.settingsToSave = {};
      }
      this.$refs.drawer.open();
    },
    close() {
      this.settingsToSave = {};
      this.$refs.drawer.close();
    },
    apply() {
      this.loading = true;
      if (this.settingsToSave?.periodType?.value) {
        this.settingsToSave.periodType = this.settingsToSave.periodType.value;
      }
      this.$rewardService.saveRewardSettings(this.settingsToSave)
        .then(() => {
          return this.$emit('setting-updated');
        }).finally(() => {
          this.loading = false;
          this.close();
        });
    },
    maxValueFormatted(max) {
      return new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(max);
    },
    computeDistributionForecast() {
      this.loadingForecast = true;
      if (this.settingsToSave?.periodType?.value) {
        this.settingsToSave.periodType = this.settingsToSave.periodType.value;
      }
      return this.$rewardService.computeDistributionForecast(this.settingsToSave)
        .then(distributionForecast => {
          this.distributionForecast = distributionForecast;
        }).finally(() => {
          this.loadingForecast = false;
        });
    },
  },
};
</script>