import WalletAPIApp from './components/api/WalletAPIApp.vue';

Vue.use(Vuetify);

const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || 'en';
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.addon.Wallet-${lang}.json`;

document.addEventListener('exo-wallet-init', () => {
  if (!window.walletAPIInitialized) {
    exoi18n.loadLanguageAsync(lang, url).then(i18n => {
      window.require(['SHARED/Web3', 'SHARED/walletCommon'], (LocalWeb3, WalletCommon) => {
        Vue.use(WalletCommon);
        const vuetify = new Vuetify({
          dark: true,
          iconfont: 'mdi',
          theme: { disable: true },
        });

        window.LocalWeb3 = LocalWeb3;

        new Vue({
          render: (h) => h(WalletAPIApp),
          i18n,
          vuetify,
        }).$mount('#WalletAPIApp');
      });
    });
  }
});

window.walletAddonInstalled = true;

document.addEventListener('profile-extension-init', () => {
  document.dispatchEvent(new CustomEvent('exo-wallet-init'));
});
