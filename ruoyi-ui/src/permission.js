import router from './router'
import store from './store'

const whiteList = ['/login']

router.beforeEach(async (to, from, next) => {
  const token = store.getters.token
  
  if (token) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      const userInfo = store.getters.userInfo
      if (userInfo && userInfo.userId) {
        next()
      } else {
        try {
          await store.dispatch('user/GetInfo')
          next()
        } catch (error) {
          await store.dispatch('user/Logout')
          next(`/login?redirect=${to.fullPath}`)
        }
      }
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${to.fullPath}`)
    }
  }
})