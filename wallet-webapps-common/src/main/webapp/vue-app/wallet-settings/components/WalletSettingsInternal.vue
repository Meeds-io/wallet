<template>
  <v-list-item>
    <v-list-item-content transition="fade-transition" :class="!useInternalWallet && 'half-opacity'">
      <v-list-item-title class="text-color">
        <div class="d-flex align-center">
          <img
            class="pr-2 pl-1"
            :src="`/wallet-common/images/meeds.svg`"
            alt="Meeds" 
            width="16">
          {{ $t('exoplatform.wallet.settings.meedsWallet') }}
        </div>
      </v-list-item-title>
    </v-list-item-content>
    <v-list-item-action>
      <v-btn
        :disabled="!useInternalWallet"
        small
        icon
        @click="openDetail">
        <v-icon size="24" class="text-sub-title">
          {{ $vuetify.rtl && 'fa-caret-left' || 'fa-caret-right' }}
        </v-icon>
      </v-btn>
    </v-list-item-action>
  </v-list-item>
</template>
<script>
export default {
  props: {
    walletSettings: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    useInternalWallet: false,
  }),
  watch: {
    walletSettings: {
      immediate: true,
      handler() {
        this.useInternalWallet = this.walletSettings && this.walletSettings.wallet && (!this.walletSettings.wallet.provider || this.walletSettings.wallet.provider === 'INTERNAL_WALLET') || false;
      },
    },
  },
  created() {
    this.$root.$on('wallet-settings-provider-changing', provider => {
      this.useInternalWallet = !provider || provider === 'INTERNAL_WALLET' || false;
    });
  },
  methods: {
    openDetail() {
      this.$emit('open-detail',
        this.$t('exoplatform.wallet.label.settings.internal'),
        this.$t('exoplatform.wallet.message.settingsDescription.internal'));
    },
  },
};
</script>