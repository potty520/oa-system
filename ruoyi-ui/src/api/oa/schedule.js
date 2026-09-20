import request from '@/utils/request'

export function list(query) {
  return request({
    url: '/oa/schedule/list',
    method: 'get',
    params: query
  })
}

export function myList(query) {
  return request({
    url: '/oa/schedule/myList',
    method: 'get',
    params: query
  })
}

export function getInfo(id) {
  return request({
    url: '/oa/schedule/' + id,
    method: 'get'
  })
}

export function add(data) {
  return request({
    url: '/oa/schedule',
    method: 'post',
    data: data
  })
}

export function edit(data) {
  return request({
    url: '/oa/schedule',
    method: 'put',
    data: data
  })
}

export function remove(id) {
  return request({
    url: '/oa/schedule/' + id,
    method: 'delete'
  })
}

export function cancel(id) {
  return request({
    url: '/oa/schedule/cancel/' + id,
    method: 'put'
  })
}
