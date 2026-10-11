import router from './router'
import store from './store'

// 登录页、404、403 不需要会话；其余路由均要先拿到 token + 用户信息
const whiteList = ['/login', '/404', '/403']

/**
 * 校验目标路由的可访问性。
 *
 * <p>侧边栏已按 meta.permissions 过滤，但用户仍可直接在地址栏输入无权限的地址。
 * 此前这种情况会渲染出「完整页面框架 + 暂无数据」的空壳，既不跳 404 也不给提示，
 * 容易被误判为系统故障。这里统一拦到 /403。</p>
 *
 * @returns {boolean} 是否已经调用过 next()（true 表示本次导航已处理完毕）
 */
function checkRoutePermission(to, next) {
  const required = (to.meta && to.meta.permissions) || []
  if (!required.length) {
    return false
  }
  const userInfo = store.getters.userInfo || {}
  // 与 store/modules/permission.js 口径一致：内置超级管理员拥有全部权限
  if (userInfo.userId === 1) {
    return false
  }
  const permissions = store.getters.permissions || []
  if (permissions.indexOf('*:*:*') !== -1) {
    return false
  }
  if (required.some(permission => permissions.indexOf(permission) !== -1)) {
    return false
  }
  next('/403')
  return true
}

router.beforeEach(async (to, from, next) => {
  const token = store.getters.token

  if (!token) {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next('/login?redirect=' + encodeURIComponent(to.fullPath))
    }
    return
  }

  if (to.path === '/login') {
    next({ path: '/' })
    return
  }

  const userInfo = store.getters.userInfo
  if (userInfo && userInfo.userId) {
    if (!checkRoutePermission(to, next)) {
      next()
    }
    return
  }

  try {
    await store.dispatch('user/GetInfo')
    // 权限到手后再生成可见菜单，避免普通用户看到无权访问的菜单
    await store.dispatch('permission/GenerateRoutes')
    if (!checkRoutePermission(to, next)) {
      next()
    }
  } catch (error) {
    // token 已失效或账号被停用：清本地会话后回登录页
    await store.dispatch('user/ResetToken')
    next('/login?redirect=' + encodeURIComponent(to.fullPath))
  }
})
