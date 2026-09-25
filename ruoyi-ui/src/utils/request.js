import axios from 'axios'
import { Message, MessageBox } from 'element-ui'
import store from '@/store'
import { getToken } from './auth'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 30000
})

// 同一时刻只允许一个“登录已失效”弹窗，避免并发请求同时 401 时刷屏
let reloginPending = false

function goLogin() {
  const { pathname, search } = window.location
  const redirect = pathname === '/login' ? '' : '?redirect=' + encodeURIComponent(pathname + search)
  window.location.href = '/login' + redirect
}

function handleUnauthorized() {
  if (reloginPending) {
    return
  }
  reloginPending = true
  MessageBox.confirm('登录状态已失效，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '稍后再说',
    type: 'warning'
  }).then(() => {
    reloginPending = false
    store.dispatch('user/ResetToken').then(() => goLogin())
  }).catch(() => {
    // 取消同样要清掉本地令牌：服务端已经不认这个会话，
    // 留着只会让后续每一个请求继续 401，下次路由跳转会由守卫送回登录页
    reloginPending = false
    store.dispatch('user/ResetToken')
  })
}

function describeHttpError(status, fallback) {
  switch (status) {
    case 400:
      return '请求参数有误'
    case 403:
      return '没有操作权限'
    case 404:
      return '请求的资源不存在'
    case 405:
      return '请求方法不被允许'
    case 429:
      return '操作过于频繁，请稍后再试'
    case 500:
    case 502:
    case 503:
    case 504:
      return '服务暂时不可用，请稍后再试'
    default:
      return fallback || '请求失败'
  }
}

service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => {
    const res = response.data
    // 附件下载等二进制响应没有业务码，直接透传
    if (!res || typeof res !== 'object' || !('code' in res)) {
      return res
    }
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(res.msg || '登录状态已失效'))
    }
    Message({
      message: res.msg || '操作失败',
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(new Error(res.msg || 'Error'))
  },
  error => {
    const status = error.response ? error.response.status : 0
    if (status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }
    Message({
      message: describeHttpError(status, error.message),
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service
