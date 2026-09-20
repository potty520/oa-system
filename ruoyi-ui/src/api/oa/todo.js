import request from '@/utils/request'

export function list(query) {
  return request({
    url: '/oa/todo/list',
    method: 'get',
    params: query
  })
}

export function count() {
  return request({
    url: '/oa/todo/count',
    method: 'get'
  })
}

export function complete(todoType, relatedId) {
  return request({
    url: '/oa/todo/complete',
    method: 'put',
    params: { todoType, relatedId }
  })
}
