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
const path = require('path');
const { merge } = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

const config = merge(webpackCommonConfig, {
  mode: 'production',
  entry: {
    wallet: './src/main/webapp/vue-app/wallet-app/wallet.js',
    walletAPI: './src/main/webapp/vue-app/wallet-app/walletAPI.js',
    spaceWallet: './src/main/webapp/vue-app/wallet-app/spaceWallet.js',
    walletAdmin : './src/main/webapp/vue-app/wallet-admin/main.js',
    walletSetupAdmin : './src/main/webapp/vue-app/wallet-setup-admin/main.js',
    walletCommon: './src/main/webapp/vue-app/wallet-common/walletCommon.js',
    walletSettings: './src/main/webapp/vue-app/wallet-common/wallet-settings/main.js',
    walletOverview: './src/main/webapp/vue-app/wallet-common/wallet-overview/main.js',
    rewardApp: './src/main/webapp/vue-app/wallet-reward/main.js',
    engagementCenterExtensions: './src/main/webapp/vue-app/engagementCenterExtensions/extensions.js',
    notificationExtension: './src/main/webapp/vue-app/notification-extension/main.js',
  },
  output: {
    path: path.join(__dirname, 'target/wallet/'),
    filename: 'js/[name].bundle.js',
    libraryTarget: 'amd'
  },
  externals: {
    vue: 'Vue',
    vuetify: 'Vuetify',
    jquery: '$',
    web3: 'Web3'
  }
});

module.exports = config;
