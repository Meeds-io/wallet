import WalletApp from './components/WalletApp.vue';

Vue.use(Vuetify);
Vue.use(WalletCommon);

const vueInstance = new Vue({
  el: '#WalletApp',
  render: (h) => h(WalletApp),
});
