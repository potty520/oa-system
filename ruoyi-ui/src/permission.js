import router from './router'
import store from './store'

// 登录页与 404 不需要会话；其余路由均要先拿到 token + 用户信息
const whiteList = ['/login', '/404']

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
    next()
    return
  }

  try {
    await store.dispatch('user/GetInfo')
    // 权限到手后再生成可见菜单，避免普通用户看到无权访问的菜单
    await store.dispatch('permission/GenerateRoutes')
    next()
  } catch (error) {
    // token 已失效或账号被停用：清本地会话后回登录页
    await store.dispatch('user/ResetToken')
    next('/login?redirect=' + encodeURIComponent(to.fullPath))
  }
})
