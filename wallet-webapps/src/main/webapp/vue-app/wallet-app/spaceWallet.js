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
import SpaceWalletApp from './components/SpaceWalletApp.vue';
import './initComponents.js';

Vue.use(WalletCommon);

const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || 'en';
const url = `/wallet/i18n/locale.addon.Wallet?lang=${lang}`;

export function init() {
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    Vue.createApp({
      render: (h) => h(SpaceWalletApp),
      vuetify: Vue.prototype.vuetifyOptions,
      i18n
    }, '#SpaceWalletApp', 'Space Wallet Application');
  });
}