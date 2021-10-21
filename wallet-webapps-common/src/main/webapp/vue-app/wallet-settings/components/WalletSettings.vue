<template>
  <v-app>
    <template v-if="displayed">
      <wallet-settings-details
        v-if="displayDetails"
        :wallet="wallet"
        @back="closeDetail"
        @settings-changed="openDetail" />
      <v-card
        v-else
        class="ma-4 border-radius"
        flat>
        <v-list>
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title class="title text-color">
                {{ $t('exoplatform.wallet.label.settings') }}
              </v-list-item-title>
            </v-list-item-content>
            <v-list-item-action>
              <v-btn
                small
                icon
                @click="openDetail">
                <v-icon size="24" class="text-sub-title">
                  {{ $vuetify.rtl && 'fa-caret-left' || 'fa-caret-right' }}
                </v-icon>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
        </v-list>
      </v-card>
    </template>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    id: `Wallet${parseInt(Math.random() * 10000)}`,
    displayed: true,
    displayDetails: false,
    wallet: null,
  }),
  created() {
    document.addEventListener('hideSettingsApps', (event) => {
      if (event && event.detail && this.id !== event.detail) {
        this.displayed = false;
      }
    });
    document.addEventListener('showSettingsApps', () => this.displayed = true);
    setTimeout( () => {
      const urlPath = document.location.pathname;
      const settingsApplication = urlPath.split('settings/')[1] ? urlPath.split('settings/')[1] : null;
      if (settingsApplication === 'wallet') {
        document.dispatchEvent(new CustomEvent('hideSettingsApps', {detail: this.id}));
        this.displayDetails = true;
        const from = this.getQueryParam('from');
        const id = this.getQueryParam('id');
        const type = this.getQueryParam('type');
        this.getWallet(id,type);
        window.history.pushState('wallet', 'My wallet', `${eXo.env.portal.context}/${eXo.env.portal.portalName}/settings/wallet?from=${from}`);
      }
    }, 300);
  },
  mounted() {
    this.$nextTick().then(() => this.$root.$applicationLoaded());
  },
  methods: {
    openDetail() {
      document.dispatchEvent(new CustomEvent('hideSettingsApps', {detail: this.id}));
      this.displayDetails = true;
      this.getWallet();
      window.history.pushState('wallet', 'My wallet', `${eXo.env.portal.context}/${eXo.env.portal.portalName}/settings/wallet?from=settings`);
    },
    closeDetail() {
      if (this.getQueryParam('from') === 'settings') {
        window.history.pushState('wallet', 'My wallet', `${eXo.env.portal.context}/${eXo.env.portal.portalName}/settings`);
      } else if (this.getQueryParam('from') === 'application') {
        window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet`;
      }
      document.dispatchEvent(new CustomEvent('showSettingsApps'));
      this.displayDetails = false;
    },
    getQueryParam(paramName) {
      const uri = window.location.search.substring(1);
      const params = new URLSearchParams(uri);
      return params.get(paramName);
    },
    getWallet(id,type) {
      if (!id){
        id = eXo.env.portal.userName ;
        type ='user';
      }
      return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/account/detailsById?id=${id}&type=${type}`)
        .then((resp) => resp && resp.ok && resp.json())
        .then(wallet => {
          this.wallet = wallet;
        });
    },
  },
};
</script>
