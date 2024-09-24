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
  <div class="mx-5">
    <div class="py-6 text-title">{{ $t('wallet.administration.rewardManagement') }}</div>
    <v-progress-linear
      v-if="rewardReportsCount === 0 && loading"
      indeterminate
      height="2"
      color="primary" />
    <v-container v-else class="pa-0 ma-0">
      <v-row class="mx-n3">
        <v-col
          v-for="rewardReport in rewardReports"
          :key="rewardReport.period.id">
          <wallet-reward-card
            @openDetails="openDetails"
            :reward-report="rewardReport"
            :reward-settings="rewardSettings" />
        </v-col>
      </v-row>
    </v-container>
    <div class="d-flex justify-center py-4">
      <v-btn
        v-if="rewardReportsCount > 0"
        :loading="loading"
        min-width="95%"
        class="btn"
        text
        @click="$emit('loadMore')">
        Load More
      </v-btn>
    </div>
  </div>
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
    rewardReports: {
      type: Array,
      default: () => [],
    }
  },
  data: () => ({

  }),
  computed: {
    rewardReportsCount() {
      return this.rewardReports?.length || 0;
    }
  },
  methods: {
    openDetails(rewardReport) {
      this.$emit('openDetails', rewardReport);
    },
  },
};
</script>