import SpaceWalletApp from './components/SpaceWalletApp.vue';

Vue.use(Vuetify);
Vue.use(WalletCommon);

const vueInstance = new Vue({
  el: '#SpaceWalletApp',
  render: (h) => h(SpaceWalletApp),
});
