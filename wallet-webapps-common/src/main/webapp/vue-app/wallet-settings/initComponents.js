/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2021 Meeds Association
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
import WalletSettings from './components/WalletSettings.vue';
import WalletSettingsDetails from './components/WalletSettingsDetails.vue';
import WalletSettingsAlert from './components/WalletSettingsAlert.vue';
import WalletSettingsInternal from './components/WalletSettingsInternal.vue';
import WalletSettingsMetamask from './components/WalletSettingsMetamask.vue';

Vue.prototype.$applicationLoaded = function() {
  this.$root.$emit('application-loaded');
  document.dispatchEvent(new CustomEvent('vue-app-loading-end', {detail: {
    appName: this.appName,
    time: Date.now(),
  }}));
};

const components = {
  'wallet-settings': WalletSettings,
  'wallet-settings-details': WalletSettingsDetails,
  'wallet-settings-alert': WalletSettingsAlert,
  'wallet-settings-internal': WalletSettingsInternal,
  'wallet-settings-metamask': WalletSettingsMetamask,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
