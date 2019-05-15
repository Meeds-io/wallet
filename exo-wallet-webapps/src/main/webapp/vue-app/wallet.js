import WalletApp from './components/WalletApp.vue';
import {toFixed} from './js/WalletUtils.js';
import './../css/main.less';

Vue.prototype.isMaximized = window.walletAppMaximize === 'true';
Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#WalletApp',
  render: (h) => h(WalletApp),
});
