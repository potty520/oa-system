import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import Pagination from '@/components/Pagination'
import router from './router'
import store from './store'
import './permission'
import './assets/styles/index.scss'

Vue.use(ElementUI, { size: 'medium' })
Vue.component('Pagination', Pagination)

Vue.config.productionTip = false

new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
