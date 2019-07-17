<template>
  <v-dialog
    v-model="dialog"
    content-class="uiPopup"
    width="400px"
    attach="#walletDialogsParent"
    max-width="100vw"
    @keydown.esc="dialog = false">
    <v-card v-if="wallet" class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false">
        </a>
        <span class="PopupTitle popupTitle">
          {{ $t('exoplatform.wallet.title.rewardDetailsOfWallet', {0: wallet.wallet.name}) }}
        </span>
      </div>
      <v-list dense>
        <v-list-tile>
          <v-list-tile-content>{{ $t('exoplatform.wallet.label.period') }}:</v-list-tile-content>
          <v-list-tile-content class="align-end">{{ period }}</v-list-tile-content>
        </v-list-tile>
        <v-list-tile v-if="wallet.tokensSent">
          <v-list-tile-content>{{ $t('exoplatform.wallet.label.rewardsSent') }}:</v-list-tile-content>
          <v-list-tile-content class="align-end">{{ walletUtils.toFixed(wallet.tokensSent) }} {{ symbol }}</v-list-tile-content>
        </v-list-tile>
        <v-list-tile v-if="wallet.tokensToSend">
          <v-list-tile-content>{{ $t('exoplatform.wallet.label.computedRewardsToSend') }}:</v-list-tile-content>
          <v-list-tile-content class="align-end">{{ walletUtils.toFixed(wallet.tokensToSend) }} {{ symbol }}</v-list-tile-content>
        </v-list-tile>
        <template v-if="rewards.length">
          <v-list-tile>
            <v-list-tile-content>{{ $t('exoplatform.wallet.label.computedRewardsDetails') }}:</v-list-tile-content>
          </v-list-tile>
          <v-divider />
          <v-list-tile v-for="reward in rewards" :key="reward.pluginId">
            <v-list-tile-content class="pl-3">{{ $t('exoplatform.wallet.label.rewardPluginPointDetails', {0: reward.points, 1: reward.pluginId}) }}:</v-list-tile-content>
            <v-list-tile-content class="align-end">{{ walletUtils.toFixed(reward.amount) }} {{ symbol }}</v-list-tile-content>
          </v-list-tile>
        </template>
        <div v-else>
          <div class="alert alert-info">
            <i class="uiIconInfo"></i>{{ $t('exoplatform.wallet.label.noRewards') }}
          </div>
        </div>
      </v-list>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: {
    wallet: {
      type: Object,
      default: function() {
        return {};
      },
    },
    symbol: {
      type: String,
      default: function() {
        return null;
      },
    },
    period: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      dialog: false,
    }
  },
  computed: {
    rewards() {
      return this.wallet.rewards && this.wallet.rewards.length ? this.wallet.rewards.filter(reward => reward.amount || reward.points) : [];
    },
  },
  watch: {
    dialog() {
      if(!this.dialog) {
        this.$emit('closed');
      }
    }
  },
  methods: {
    open() {
      this.dialog = true;
    }
  }
};
</script>
