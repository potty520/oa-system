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

// 当前生效且可见的公告数（后端未记录逐人已读状态，不是未读数）
export function getEffectiveCount() {
  return request({
    url: '/oa/announcement/effectiveCount',
    method: 'get'
  })
}
