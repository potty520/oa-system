import request from '@/utils/request'

export function list(query) {
  return request({
    url: '/oa/incomingDoc/list',
    method: 'get',
    params: query
  })
}

export function getInfo(id) {
  return request({
    url: '/oa/incomingDoc/' + id,
    method: 'get'
  })
}

export function add(data) {
  return request({
    url: '/oa/incomingDoc',
    method: 'post',
    data: data
  })
}

export function edit(data) {
  return request({
    url: '/oa/incomingDoc',
    method: 'put',
    data: data
  })
}

export function remove(id) {
  return request({
    url: '/oa/incomingDoc/' + id,
    method: 'delete'
  })
}

export function sign(id) {
  return request({
    url: '/oa/incomingDoc/sign/' + id,
    method: 'put'
  })
}

export function complete(id) {
  return request({
    url: '/oa/incomingDoc/complete/' + id,
    method: 'put'
  })
}
