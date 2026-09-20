import request from '@/utils/request'

export function list(query) {
  return request({
    url: '/oa/leave/list',
    method: 'get',
    params: query
  })
}

export function myList(query) {
  return request({
    url: '/oa/leave/myList',
    method: 'get',
    params: query
  })
}

export function getInfo(id) {
  return request({
    url: '/oa/leave/' + id,
    method: 'get'
  })
}

export function add(data) {
  return request({
    url: '/oa/leave',
    method: 'post',
    data: data
  })
}

export function edit(data) {
  return request({
    url: '/oa/leave',
    method: 'put',
    data: data
  })
}

export function remove(id) {
  return request({
    url: '/oa/leave/' + id,
    method: 'delete'
  })
}

export function submit(id) {
  return request({
    url: '/oa/leave/submit/' + id,
    method: 'put'
  })
}

export function withdraw(id) {
  return request({
    url: '/oa/leave/withdraw/' + id,
    method: 'put'
  })
}

export function approve(data) {
  return request({
    url: '/oa/leave/approve',
    method: 'put',
    data: data
  })
}
