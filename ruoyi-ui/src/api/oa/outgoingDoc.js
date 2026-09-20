import request from '@/utils/request'

export function list(query) {
  return request({
    url: '/oa/outgoingDoc/list',
    method: 'get',
    params: query
  })
}

export function getInfo(id) {
  return request({
    url: '/oa/outgoingDoc/' + id,
    method: 'get'
  })
}

export function add(data) {
  return request({
    url: '/oa/outgoingDoc',
    method: 'post',
    data: data
  })
}

export function edit(data) {
  return request({
    url: '/oa/outgoingDoc',
    method: 'put',
    data: data
  })
}

export function remove(id) {
  return request({
    url: '/oa/outgoingDoc/' + id,
    method: 'delete'
  })
}

export function submit(id) {
  return request({
    url: '/oa/outgoingDoc/submit/' + id,
    method: 'put'
  })
}

export function sign(id) {
  return request({
    url: '/oa/outgoingDoc/sign/' + id,
    method: 'put'
  })
}
