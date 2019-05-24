const path = require('path');
const merge = require('webpack-merge');

const webpackProductionConfig = require('./webpack.prod.js');

module.exports = merge(webpackProductionConfig, {
  output: {
    path: '/home/exo/server/platform-5.3-wallet/webapps/wallet-common/',
    filename: 'js/[name].bundle.js'
  }
});