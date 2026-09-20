import request from '@/utils/request'

export function list(query) {
  return request({
    url: '/oa/announcement/list',
    method: 'get',
    params: query
  })
}

export function publishedList(query) {
  return request({
    url: '/oa/announcement/published',
    method: 'get',
    params: query
  })
}

export function getInfo(id) {
  return request({
    url: '/oa/announcement/' + id,
    method: 'get'
  })
}

export function add(data) {
  return request({
    url: '/oa/announcement',
    method: 'post',
    data: data
  })
}

export function edit(data) {
  return request({
    url: '/oa/announcement',
    method: 'put',
    data: data
  })
}

export function remove(id) {
  return request({
    url: '/oa/announcement/' + id,
    method: 'delete'
  })
}

export function getUnreadCount() {
  return request({
    url: '/oa/announcement/unreadCount',
    method: 'get'
  })
}
