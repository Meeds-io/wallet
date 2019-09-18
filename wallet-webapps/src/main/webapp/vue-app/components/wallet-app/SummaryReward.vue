<template>
  <v-card class="walletSummaryBalance elevation-1">
    <v-card-title class="title subtitle-1 pb-1 text-truncate">
      {{ $t('exoplatform.wallet.label.totalWalletRewards') }}
    </v-card-title>
    <v-card-title class="rewardBalance headline pt-0 pb-1">
      <v-container fluid>
        <v-layout row>
          <v-flex grow class="amount">
            {{ walletUtils.toFixed(rewardBalance) }} {{ contractDetails.symbol }}
          </v-flex>
          <v-flex shrink>
            <v-btn
              icon
              small
              @click="displayRewards">
              <v-icon color="primary">fa-search-plus</v-icon>
            </v-btn>
          </v-flex>
        </v-layout>
      </v-container>
    </v-card-title>
    <v-navigation-drawer
      id="rewardDetailsDrawer"
      v-model="seeRewardDetails"
      fixed
      right
      stateless
      temporary
      width="700"
      max-width="100vw">
      <reward-detail
        ref="RewardDetail"
        :wallet="wallet"
        :contract-details="contractDetails"
        @back="back()" />
    </v-navigation-drawer>
  </v-card>
</template>

<script>
import RewardDetail from './RewardDetail.vue'; 

export default {
  components: {
    RewardDetail,
  },
  props: {
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
  },
  data() {
    return {
      seeRewardDetails: false,
      seeRewardDetailsPermanent: false,
    };
  },
  computed: {
    rewardBalance() {
      return (this.wallet && this.wallet.rewardBalance) || 0;
    }
  },
  watch: {
    seeRewardDetails() {
      if (this.seeRewardDetails) {
        $('body').addClass('hide-scroll');

        const thiss = this;
        setTimeout(() => {
          thiss.seeRewardDetailsPermanent = true;
        }, 200);
      } else {
        $('body').removeClass('hide-scroll');

        this.seeRewardDetailsPermanent = false;
      }
    },
  },
  created() {
    const thiss = this;
    $(document).on('keydown', (event) => {
      if (event.which === 27 && thiss.seeRewardDetailsPermanent && !$('.v-dialog:visible').length) {
        thiss.back();
      }
    });
  },
  methods: {
    displayRewards() {
      this.seeRewardDetails = true;

      this.$nextTick(() => {
        const thiss = this;
        $('.v-overlay').on('click', (event) => {
          thiss.back();
        });
      });
    },
    back() {
      this.seeRewardDetails = false;
      this.seeRewardDetailsPermanent = false;
      this.selectedAccount = null;
    },
  }
}
</script>
