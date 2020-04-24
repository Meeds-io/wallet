const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

const config = merge(webpackCommonConfig, {
  mode: 'production',
  entry: {
    wallet: './src/main/webapp/vue-app/wallet.js',
    walletAPI: './src/main/webapp/vue-app/walletAPI.js',
    spaceWallet: './src/main/webapp/vue-app/spaceWallet.js',
    walletBalance: './src/main/webapp/vue-app/WalletBalance.js',
    walletOverview: './src/main/webapp/vue-app/wallet-overview/main.js',
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
