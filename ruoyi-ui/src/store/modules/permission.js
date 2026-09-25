import { constantRoutes } from '@/router'

/**
 * 路由表完全由 constantRoutes 静态声明，能否看到/操作由后端接口鉴权 + 前端 v-hasPermi 控制。
 * 这里只保留 Sidebar 需要的路由快照，不再提供从未被调用的动态路由生成逻辑。
 */
const state = {
  routes: constantRoutes
}

export default {
  namespaced: true,
  state
}
