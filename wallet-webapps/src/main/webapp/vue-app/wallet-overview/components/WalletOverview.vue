<template>
  <v-app :class="owner && 'walletOverviewApplication' || 'walletOverviewApplicationOther'">
    <v-toolbar
      color="white"
      height="48"
      flat
      class="border-box-sizing py-3">
      <div class="text-header-title text-sub-title text-no-wrap">
        {{ title || '' }}
      </div>
      <v-spacer />
      <i
        v-if="clickable"
        class="uiIconInformation clickable primary--text my-auto ml-3 pb-2"
        @click="openDrawer"></i>
    </v-toolbar>
    <v-card
      :class="clickable && 'clickable' || ''"
      class="walletOverviewCard white"
      flat>
      <v-card-text
        class="justify-center ma-auto py-5 d-flex flex-no-wrap"
        @click="clickable && openDrawer()">
        <v-icon
          v-if="currencySymbol"
          size="48"
          class="tertiary-color walletOverviewCurrencySymbol px-2">
          {{ currencySymbol }}
        </v-icon>
        <div class="text-color display-2 text-left font-weight-bold walletOverviewBalance">
          {{ rewardBalance }}
        </div>
      </v-card-text>
    </v-card>

    <wallet-overview-drawer
      ref="walletOverviewDrawer"
      :symbol="currencySymbol" />
  </v-app>
</template>

<script>
export default {
  data: () => ({
    owner: eXo.env.portal.profileOwner === eXo.env.portal.userName,
    currentName: eXo.env.portal.profileOwner,
    title: null,
    currencyName: null,
    currencySymbol: null,
    rewardBalance: 0,
    skeleton: true,
  }),
  computed: {
    clickable() {
      return this.owner && this.rewardBalance > 0;
    },
  },
  created() {
    this.refresh();
  },
  methods: {
    openDrawer() {
      if (this.owner) {
        this.$refs.walletOverviewDrawer.open(this.title, );
      }
    },
    refresh() {
      let loading = 1;

      if (!this.currencyName) {
        loading++;
        // Search settings in a sync way
        fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/settings`)
          .then((resp) => resp && resp.ok && resp.json())
          .then(settings => {
            const contract = settings && settings.contractDetail;
            this.currencyName = contract && contract.name;
            this.currencySymbol = contract && contract.symbol;
            if (this.currencyName) {
              this.title = this.$t('exoplatform.wallet.title.rewardsOverview', {0: this.currencyName});
            }
          })
          .finally(() => {
            loading--;
            if (loading === 0) {
              this.skeleton = false;
              // Decrement 'loading' effect after having incremented it in main.js
              document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
            }
          });
      }

      return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/account/detailsById?id=${this.currentName}&type=user`, {credentials: 'include'})
        .then((resp) => resp && resp.ok && resp.json())
        .then(wallet => {
          this.rewardBalance = parseInt(wallet && wallet.rewardBalance * 100 || 0) / 100;
        })
        .finally(() => {
          loading--;
          if (loading === 0) {
            this.skeleton = false;
            // Decrement 'loading' effect after having incremented it in main.js
            document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
          }
        });
    },
  },
};
</script>