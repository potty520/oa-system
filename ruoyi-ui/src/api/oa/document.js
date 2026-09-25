import request from '@/utils/request'

export function list(query) {
  return request({ url: '/oa/document/list', method: 'get', params: query })
}

export function upload(data) {
  return request({ url: '/oa/document/upload', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } })
}

export function remove(id) {
  return request({ url: `/oa/document/${id}`, method: 'delete' })
}

// 附件下载必须走 axios：后端只从 Authorization 头取令牌，
// 直接 window.location 导航不会携带该头，会被判成未登录
export function download(id) {
  return request({ url: `/oa/document/download/${id}`, method: 'get', responseType: 'blob' })
}