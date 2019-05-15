import WalletAdminContractsApp from './components/admin/WalletAdminContractsApp.vue';

Vue.use(Vuetify);
Vue.use(WalletCommon);

new Vue({
  el: '#WalletAdminApp',
  render: (h) => h(WalletAdminContractsApp),
});
