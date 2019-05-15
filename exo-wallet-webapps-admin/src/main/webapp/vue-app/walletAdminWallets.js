import WalletAdminWalletsApp from './components/admin/WalletAdminWalletsApp.vue';

Vue.use(Vuetify);
Vue.use(WalletCommon);

new Vue({
  el: '#WalletAdminApp',
  render: (h) => h(WalletAdminWalletsApp),
});
