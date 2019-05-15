import WalletAdminSettingsApp from './components/admin/WalletAdminSettingsApp.vue';

Vue.use(Vuetify);
Vue.use(WalletCommon);

new Vue({
  el: '#WalletAdminApp',
  render: (h) => h(WalletAdminSettingsApp),
});
