import request from '@/utils/request'

export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

export function login(data) {
  return request({
    url: '/login',
    method: 'post',
    data: data
  })
}

export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 用户下拉选项：只含 userId/userName/nickName，供各处选人控件使用
export function listUserOptions() {
  return request({
    url: '/system/user/options',
    method: 'get'
  })
}

export function updateUserPwd(data) {
  return request({
    url: '/system/user/profile/updatePwd',
    method: 'put',
    data: data
  })
}
