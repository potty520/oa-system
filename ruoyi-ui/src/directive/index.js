import hasPermi from './permission/hasPermi'

export default {
  install(Vue) {
    Vue.directive('hasPermi', hasPermi)
  }
}
