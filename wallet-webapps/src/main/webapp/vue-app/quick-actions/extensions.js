/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

extensionRegistry.registerExtension('QuickAction', 'Extension', {
  id: 'walletTransactions',
  icon: 'fa-wallet',
  name: 'quickActions.walletTransactions.name',
  description: 'quickActions.walletTransactions.description',
  click: () => new Promise(resolve => {
    window.require(['SHARED/eXoVueI18n', 'PORTLET/wallet/WalletOverview'], exoi18n => initWalletTransactionsDrawer(exoi18n, resolve));
  }),
});

async function initWalletTransactionsDrawer(exoi18n, callback) {
  const appId = 'wallet-transactions-actions';
  if (!document.querySelector(`#${appId}`)) {
    const parent = document.createElement('div');
    parent.id = appId;
    document.querySelector('#vuetify-apps').appendChild(parent);
    await initWalletTransactionsDrawerApp(appId, exoi18n);
    await Vue.prototype.$utils.importSkin('portal', 'wallet');
  }
  document.dispatchEvent(new CustomEvent('quick-action-wallet-transactions-drawer', {detail: callback}));
}

function initWalletTransactionsDrawerApp(appId, exoi18n) {
  const lang = eXo.env.portal.language;
  const url = `/wallet/i18n/locale.addon.Wallet?lang=${lang}`;
  return new Promise(resolve => exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => Vue.createApp({
      template: `
        <wallet-overview-drawer
          id="${appId}"
          ref="drawer" />
      `,
      created() {
        document.addEventListener('quick-action-wallet-transactions-drawer', this.openDrawer);
      },
      async mounted() {
        await Vue.use(WalletCommon);
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
        await this.walletUtils.initSettings(false, true, true);
        resolve();
      },
      beforeDestroy() {
        document.removeEventListener('quick-action-wallet-transactions-drawer', this.openDrawer);
      },
      methods: {
        openDrawer() {
          this.$refs.drawer.open();
        },
      },
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Wallet Transaction Quick Action')));
}
