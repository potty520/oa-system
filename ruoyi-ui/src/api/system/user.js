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

export function updateUserPwd(data) {
  return request({
    url: '/system/user/profile/updatePwd',
    method: 'put',
    data: data
  })
}
