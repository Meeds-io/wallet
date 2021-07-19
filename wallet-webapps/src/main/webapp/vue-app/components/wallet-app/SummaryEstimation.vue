<template>
  <v-flex
    text-center>
    <v-card class="walletSummaryBalance elevation-3 pa-1 ma-1">
      <v-card-title class="title subtitle-1 pb-1 text-truncate">
        {{ $t('exoplatform.wallet.label.nextPayEstimation') }}
      </v-card-title>
      <v-card-title class="rewardBalance headline pt-0 pb-1">
        <v-container fluid>
          <v-layout row>
            <v-flex grow class="amount">
              <span class="symbol"> {{ symbol }} </span>  {{ walletUtils.toFixed(nextPayEstimation) }}
            </v-flex>
          </v-layout>
        </v-container>
      </v-card-title>
    </v-card>
  </v-flex>
</template>

<script>
export default {
  props: {
    nextPayEstimation: {
      type: String,
      default: function() {
        return '';
      },
    },
    symbol: {
      type: Object,
      default: function() {
        return {};
      },
    },
  }
};
</script>
