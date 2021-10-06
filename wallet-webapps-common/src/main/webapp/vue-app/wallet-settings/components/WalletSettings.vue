<template>
  <v-app>
    <template v-if="displayed">
      <wallet-settings-details
        v-if="displayDetails"
        @back="closeDetail" />
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
        this.openDetail();
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
      window.history.pushState('wallet', 'My wallet', `${eXo.env.portal.context}/${eXo.env.portal.portalName}/settings/wallet`);
    },
    closeDetail() {
      document.dispatchEvent(new CustomEvent('showSettingsApps'));
      this.displayDetails = false;
      window.history.pushState('wallet', 'My wallet', `${eXo.env.portal.context}/${eXo.env.portal.portalName}/settings`);
    },
  },
};
</script>
