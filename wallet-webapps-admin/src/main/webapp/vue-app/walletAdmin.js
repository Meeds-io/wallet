import WalletAdminApp from './components/admin/WalletAdminApp.vue';

Vue.use(Vuetify);
Vue.use(WalletCommon);

new Vue({
  el: '#WalletAdminApp',
  render: (h) => h(WalletAdminApp),
});
