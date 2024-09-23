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
  <v-card
    :loading="loading"
    class="border-radius border-color my-5 me-5"
    width="60%"
    flat>
    <div class="d-flex flex-column flex-grow-1">
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="text-header text-wrap">
            {{ $t('wallet.administration.budgetConfiguration') }}
          </v-list-item-title>
        </v-list-item-content>
        <v-list-item-action v-if="!loading && hasConfiguredBudget" class="ma-auto">
          <v-tooltip
            :disabled="$root.isMobile"
            bottom>
            <template #activator="{ on }">
              <div v-on="on">
                <v-btn
                  :aria-label="$t('wallet.administration.budgetConfiguration.editConfiguration')"
                  small
                  icon
                  @click="openConfigurationDrawer">
                  <v-icon size="18">fas fa-edit</v-icon>
                </v-btn>
              </div>
            </template>
            <span>{{ $t('wallet.administration.budgetConfiguration.editConfiguration') }}</span>
          </v-tooltip>
        </v-list-item-action>
        <v-list-item-action v-if="!loading && hasConfiguredBudget" class="ma-auto">
          <v-tooltip
            :disabled="$root.isMobile"
            bottom>
            <template #activator="{ on }">
              <div v-on="on">
                <v-btn
                  :aria-label="$t('wallet.administration.budgetConfiguration.deleteConfiguration')"
                  color="error"
                  small
                  icon
                  @click="deleteConfirmDialog">
                  <v-icon size="18">fas fa-trash</v-icon>
                </v-btn>
              </div>
            </template>
            <span>{{ $t('wallet.administration.budgetConfiguration.editConfiguration') }}</span>
          </v-tooltip>
        </v-list-item-action>
      </v-list-item>
      <v-spacer />
      <template v-if="!loading && hasConfiguredBudget">
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="text-wrap">
              {{ $t('wallet.administration.budgetConfiguration.periodType.title') }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action class="ma-auto font-weight-bold pe-1">
            {{ periodTypeLabel }}
          </v-list-item-action>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="text-wrap">
              {{ $t('wallet.administration.budgetConfiguration.timeZone.title') }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action class="ma-auto font-weight-bold pe-1">
            {{ periodDatesDisplay }}
          </v-list-item-action>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="text-wrap">
              {{ $t('wallet.administration.budgetConfiguration.threshold.title') }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action class="ma-auto font-weight-bold pe-1">
            {{ threshold }} {{ $t('wallet.administration.budgetConfiguration.points') }}
          </v-list-item-action>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="text-wrap">
              {{ budgetTypeLabel }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action class="ma-auto font-weight-bold pe-1">
            {{ amount }}  MEED
          </v-list-item-action>
        </v-list-item>
      </template>
      <template v-else-if="!loading">
        <div class="d-flex flex-column align-center justify-center full-width full-height pt-5">
          <v-icon color="orange darken-2" size="50">fas fa-coins</v-icon>
          <span class="mt-5">{{ $t('wallet.administration.budgetConfiguration.setUp') }}</span>
        </div>
        <v-btn
          class="btn btn-primary align-self-center my-4 px-2"
          @click="openConfigurationDrawer">
          <span class="mx-2">
            {{ $t('wallet.administration.budgetConfiguration.setBudget') }}
          </span>
        </v-btn>
      </template>
    </div>
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :message="$t('wallet.administration.budgetConfiguration.message.confirmDeleteConfiguration')"
      :title="$t('wallet.administration.budgetConfiguration.confirmDeleteConfiguration')"
      :ok-label="$t('wallet.administration.confirm.label')"
      :cancel-label="$t('wallet.administration.cancel.label')"
      @ok="$emit('deleteSetting')" />
  </v-card>
</template>

<script>

export default {
  props: {
    loading: {
      type: Boolean,
      default: false,
    },
    rewardSettings: {
      type: Object,
      default: null
    },
    rewardReport: {
      type: Object,
      default: null
    }
  },
  data: () => ({

  }),
  computed: {
    periodType() {
      return this.rewardSettings?.periodType;
    },
    periodTypeLabel() {
      return this.$t(`wallet.administration.periodType.label.${this.periodType?.toLowerCase()}`) || '';
    },
    rewardPeriod() {
      return this.rewardReport?.period;
    },
    periodDatesDisplay() {
      if (!this.rewardPeriod) {
        return '';
      }
      const startDateFormatted = this.$dateUtil.formatDateObjectToDisplay(new Date(this.rewardPeriod.startDateInSeconds * 1000), this.dateformat, eXo.env.portal.language);
      const endDateFormatted = this.$dateUtil.formatDateObjectToDisplay(new Date(this.rewardPeriod.endDateInSeconds * 1000 - 1), this.dateformat, eXo.env.portal.language);
      return `${startDateFormatted} ${this.$t('exoplatform.wallet.label.to')} ${endDateFormatted}`;
    },
    dateformat() {
      return this.timeZone && {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        timeZoneName: 'short',
        timeZone: this.timeZone,
      } || {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      };
    },
    timeZone() {
      return this.rewardPeriod?.timeZone || this.rewardSettings?.timeZone;
    },
    amount() {
      return this.valueFormatted(this.rewardSettings?.amount);
    },
    threshold() {
      return this.valueFormatted(this.rewardSettings?.threshold);
    },
    budgetType() {
      return this.rewardSettings?.budgetType;
    },
    budgetTypeLabel() {
      return this.budgetType === 'FIXED' ? this.$t('wallet.administration.budgetConfiguration.budgetFixed.title') : this.$t('wallet.administration.budgetConfiguration.budgetPerContributor.title');
    },
    hasConfiguredBudget() {
      return this.rewardSettings?.storedSetting;
    },
  },
  methods: {
    openConfigurationDrawer() {
      this.$emit('openConfiguration');
    },
    deleteConfirmDialog() {
      this.$refs.deleteConfirmDialog.open();
    },
    valueFormatted(max) {
      return new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(max);
    },
  },
};
</script>