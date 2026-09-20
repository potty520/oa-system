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

export function downloadUrl(id) {
  return `${process.env.VUE_APP_BASE_API}/oa/document/download/${id}`
}