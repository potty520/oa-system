import { constantRoutes } from '@/router'

const ALL_PERMISSION = '*:*:*'
// 与 utils/permission.js、后端 SecurityUtils.isAdmin 同一口径
const SUPER_ADMIN_USER_ID = 1

/**
 * 单个路由的可见性：没有声明 meta.permissions 的路由保持可见，
 * 声明了则要求用户持有其中任意一个权限（`*:*:*` 表示全量授权）。
 */
function hasRoutePermission(route, permissions) {
  const required = route.meta && route.meta.permissions
  if (!required || required.length === 0) {
    return true
  }
  return required.some(permission => permissions.indexOf(permission) !== -1)
}

function filterRoutes(routes, permissions) {
  const result = []
  routes.forEach(route => {
    // 登录页、404、/redirect、通配兜底等内部路由不参与过滤，仍靠 hidden 隐藏
    if (route.hidden) {
      result.push(route)
      return
    }
    const item = { ...route }
    if (item.children && item.children.length > 0) {
      item.children = filterRoutes(item.children, permissions)
      // 子菜单被全部过滤掉时，父菜单不应再作为空壳出现
      if (item.children.length === 0) {
        return
      }
    }
    if (hasRoutePermission(item, permissions)) {
      result.push(item)
    }
  })
  return result
}

/**
 * 路由表由 constantRoutes 静态声明，能否“看到”由 meta.permissions 与当前用户权限决定，
 * 能否“操作”仍由后端接口鉴权 + 前端 v-hasPermi 控制。
 * 这里保存 Sidebar 需要的路由快照，待 GetInfo 拿到权限后由 GenerateRoutes 重新生成。
 */
const state = {
  routes: constantRoutes
}

const mutations = {
  SET_ROUTES: (state, routes) => {
    state.routes = routes
  }
}

const actions = {
  GenerateRoutes({ commit, rootState }) {
    const user = (rootState.user && rootState.user.userInfo) || {}
    const permissions = (rootState.user && rootState.user.permissions) || []
    if (user.userId === SUPER_ADMIN_USER_ID || permissions.indexOf(ALL_PERMISSION) !== -1) {
      commit('SET_ROUTES', constantRoutes)
      return constantRoutes
    }
    const routes = filterRoutes(constantRoutes, permissions)
    commit('SET_ROUTES', routes)
    return routes
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
