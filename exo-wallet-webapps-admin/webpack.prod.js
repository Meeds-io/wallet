const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

const config = merge(webpackCommonConfig, {
  mode: 'production',
  entry: {
    walletAdminSettings: './src/main/webapp/vue-app/walletAdminSettings.js',
    walletAdminContracts: './src/main/webapp/vue-app/walletAdminContracts.js',
    walletAdminWallets: './src/main/webapp/vue-app/walletAdminWallets.js'
  },
  output: {
    path: path.join(__dirname, 'target/exo-ethereum-wallet-admin/'),
    filename: 'js/[name].bundle.js'
  },
  externals: {
    vue: 'Vue',
    vuetify: 'Vuetify',
    jquery: '$',
    web3: 'Web3'
  }
});

module.exports = config;