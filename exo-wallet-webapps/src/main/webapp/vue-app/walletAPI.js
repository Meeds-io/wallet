import WalletAPIApp from './components/api/WalletAPIApp.vue';

Vue.use(Vuetify);
Vue.use(WalletCommon);

const vueInstance = new Vue({
  el: '#WalletAPIApp',
  render: (h) => h(WalletAPIApp),
});
