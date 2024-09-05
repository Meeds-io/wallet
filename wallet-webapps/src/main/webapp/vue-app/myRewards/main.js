/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
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
import './initComponents.js';

const lang = eXo?.env?.portal?.language || 'en';
const urls = [
  `/gamification-portlets/i18n/locale.addon.Gamification?lang=${lang}`,
  `/gamification-portlets/i18n/locale.portlet.Challenges?lang=${lang}`
];
const appId = 'myRewards';

export function init() {
  exoi18n.loadLanguageAsync(lang, urls)
    .then(i18n => {
      Vue.createApp({
        template: `<gamification-overview-rewards id="${appId}" />`,
        i18n,
        vuetify: Vue.prototype.vuetifyOptions,
      }, `#${appId}`, 'My Rewards');
    }).finally(() => {
      Vue.prototype.$utils.includeExtensions('WalletAPIBundle');
      Vue.prototype.$utils.includeExtensions('PerkStoreOverviewAPI');
    });
}