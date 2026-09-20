import request from '@/utils/request'

export function list(query) {
  return request({
    url: '/oa/docFlow/list',
    method: 'get',
    params: query
  })
}

export function getInfo(id) {
  return request({
    url: '/oa/docFlow/' + id,
    method: 'get'
  })
}

export function add(data) {
  return request({
    url: '/oa/docFlow',
    method: 'post',
    data: data
  })
}

export function edit(data) {
  return request({
    url: '/oa/docFlow',
    method: 'put',
    data: data
  })
}

export function remove(id) {
  return request({
    url: '/oa/docFlow/' + id,
    method: 'delete'
  })
}
