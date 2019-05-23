const path = require('path');
const merge = require('webpack-merge');

const webpackProductionConfig = require('./webpack.prod.js');

module.exports = merge(webpackProductionConfig, {
  output: {
    path: '/exo-server/webapps/wallet/',
    filename: 'js/[name].bundle.js'
  }
});