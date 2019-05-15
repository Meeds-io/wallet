import SpaceWalletApp from './components/SpaceWalletApp.vue';
import {toFixed} from './js/WalletUtils.js';
import './../css/main.less';

Vue.prototype.isMaximized = 'true';
Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#SpaceWalletApp',
  render: (h) => h(SpaceWalletApp),
});
