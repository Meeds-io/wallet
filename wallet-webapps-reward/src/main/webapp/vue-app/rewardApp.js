import RewardApp from './components/reward/RewardApp.vue';

Vue.use(Vuetify);
Vue.use(WalletCommon);

const vueInstance = new Vue({
  el: '#RewardApp',
  render: (h) => h(RewardApp),
});
