import { login, logout, getInfo } from '@/api/system/user'
import { getToken, setToken, removeToken } from '@/utils/auth'

const state = {
  token: getToken(),
  userInfo: {},
  permissions: []
}

const mutations = {
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_USER_INFO: (state, userInfo) => {
    state.userInfo = userInfo
  },
  SET_PERMISSIONS: (state, permissions) => {
    state.permissions = permissions
  },
  SET_MUST_CHANGE_PWD: (state, mustChangePwd) => {
    state.userInfo = { ...state.userInfo, mustChangePwd: mustChangePwd }
  }
}

const actions = {
  Login({ commit }, userInfo) {
    const { username, password } = userInfo
    return new Promise((resolve, reject) => {
      login({ username: username.trim(), password: password }).then(response => {
        const { data } = response
        commit('SET_TOKEN', data.token)
        setToken(data.token)
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  GetInfo({ commit }) {
    return new Promise((resolve, reject) => {
      getInfo().then(response => {
        const { data } = response
        commit('SET_USER_INFO', data.user)
        commit('SET_PERMISSIONS', data.permissions || [])
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  Logout({ commit }) {
    const clearLocalSession = () => {
      commit('SET_TOKEN', '')
      commit('SET_USER_INFO', {})
      commit('SET_PERMISSIONS', [])
      removeToken()
    }
    // 服务端注销失败（网络抖动 / 令牌已过期）也必须清理本地会话，
    // 否则调用方的 then 永远不会执行，用户会被卡在“已登录”状态里
    return logout().catch(() => null).then(() => {
      clearLocalSession()
    })
  },

  ResetToken({ commit }) {
    return new Promise(resolve => {
      commit('SET_TOKEN', '')
      commit('SET_USER_INFO', {})
      commit('SET_PERMISSIONS', [])
      removeToken()
      resolve()
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
