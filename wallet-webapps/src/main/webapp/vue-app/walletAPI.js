import WalletAPIApp from './components/api/WalletAPIApp.vue';

Vue.use(Vuetify);
Vue.use(WalletCommon);

const vuetify = new Vuetify({
  dark: true,
  iconfont: 'mdi',
  theme: { disable: true },
});

const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || 'en';
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.addon.Wallet-${lang}.json`;

exoi18n.loadLanguageAsync(lang, url).then(i18n => {
  new Vue({
    render: (h) => h(WalletAPIApp),
    i18n,
    vuetify,
  }).$mount('#WalletAPIApp');
});
