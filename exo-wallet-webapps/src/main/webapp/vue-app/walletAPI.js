import WalletAPIApp from './components/api/WalletAPIApp.vue';
import {toFixed} from './js/WalletUtils.js';
import './../css/main.less';

Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#WalletAPIApp',
  render: (h) => h(WalletAPIApp),
});
