import Vue from 'vue'
import ElementUI from 'element-ui'
// 引入自定义主题（内含 Element 源码），主色见 assets/styles/element-variables.scss
import './assets/styles/element-variables.scss'
import App from './App.vue'
import Pagination from '@/components/Pagination'
import directive from './directive'
import router from './router'
import store from './store'
import './permission'
import './assets/styles/index.scss'

Vue.use(ElementUI, { size: 'medium' })
Vue.use(directive)
Vue.component('Pagination', Pagination)

Vue.config.productionTip = false

new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
