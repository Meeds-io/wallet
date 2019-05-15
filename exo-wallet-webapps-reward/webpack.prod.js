const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

const config = merge(webpackCommonConfig, {
  mode: 'production',
  entry: {
    rewardApp: './src/main/webapp/vue-app/rewardApp.js',
  },
  output: {
    path: path.join(__dirname, 'target/exo-ethereum-wallet-reward/'),
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