'use strict'
const path = require('path')

function resolve(dir) {
  return path.join(__dirname, dir)
}

const name = 'OA办公系统'
const port = process.env.port || process.env.npm_config_port || 18081
// 开发服务默认只监听回环地址，需要对外联调时用 HOST 环境变量显式打开
const host = process.env.HOST || '127.0.0.1'

module.exports = {
  publicPath: '/',
  outputDir: 'dist',
  assetsDir: 'static',
  lintOnSave: false,
  productionSourceMap: false,
  devServer: {
    host: host,
    port: port,
    open: false,
    // 'all' 会关掉 Host 校验，配合 0.0.0.0 监听等于把开发服务暴露给局域网并放开 DNS rebinding；
    // 默认交给 webpack-dev-server 的 'auto'，自定义域名用 VUE_APP_DEV_ALLOWED_HOSTS 声明
    allowedHosts: process.env.VUE_APP_DEV_ALLOWED_HOSTS
      ? process.env.VUE_APP_DEV_ALLOWED_HOSTS.split(',')
      : 'auto',
    client: {
      overlay: { warnings: false, errors: true }
    },
    proxy: {
      [process.env.VUE_APP_BASE_API]: {
        target: process.env.VUE_APP_BACKEND_URL || 'http://localhost:18080',
        changeOrigin: true,
        pathRewrite: {
          ['^' + process.env.VUE_APP_BASE_API]: ''
        }
      }
    }
  },
  configureWebpack: {
    name: name,
    resolve: {
      alias: {
        '@': resolve('src')
      }
    }
  },
  chainWebpack(config) {
    config.plugins.delete('prefetch')

    config.when(process.env.NODE_ENV !== 'development', config => {
      config.optimization.splitChunks({
        chunks: 'all',
        cacheGroups: {
          libs: {
            name: 'chunk-libs',
            test: /[\\/]node_modules[\\/]/,
            priority: 10,
            chunks: 'initial'
          },
          elementUI: {
            name: 'chunk-elementUI',
            priority: 20,
            test: /[\\/]node_modules[\\/]_?element-ui(.*)/
          },
          commons: {
            name: 'chunk-commons',
            test: resolve('src/components'),
            minChunks: 3,
            priority: 5,
            reuseExistingChunk: true
          }
        }
      })
      config.optimization.runtimeChunk('single')
    })
  }
}