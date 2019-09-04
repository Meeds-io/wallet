import WalletAdminApp from './components/admin/WalletAdminApp.vue';

Vue.use(Vuetify);
Vue.use(WalletCommon);

$(document).ready(() => {
  const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || 'en';
  const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.addon.Wallet-${lang}.json`;

  exoi18n.loadLanguageAsync(lang, url).then((i18n) => {
    new Vue({
      el: '#WalletAdminApp',
      render: (h) => h(WalletAdminApp),
      i18n,
    });
  });
});
