/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import WalletAPIApp from './components/api/WalletAPIApp.vue';

Vue.use(Vuetify);

const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || 'en';
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.addon.Wallet-${lang}.json`;

document.addEventListener('exo-wallet-init', () => {
  if (!window.walletAPIInitialized) {
    exoi18n.loadLanguageAsync(lang, url).then(i18n => {
      window.require(['SHARED/Web3', 'SHARED/walletCommon'], (LocalWeb3, WalletCommon) => {
        Vue.use(WalletCommon);
        const vuetify = new Vuetify(Object.assign({}, eXo.env.portal.vuetifyPreset));
        vuetify.theme ={ disable: true };

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
