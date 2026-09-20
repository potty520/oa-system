import request from '@/utils/request'

export function list(query) {
  return request({
    url: '/oa/reimbursement/list',
    method: 'get',
    params: query
  })
}

export function myList(query) {
  return request({
    url: '/oa/reimbursement/myList',
    method: 'get',
    params: query
  })
}

export function getInfo(id) {
  return request({
    url: '/oa/reimbursement/' + id,
    method: 'get'
  })
}

export function saveWithDetails(data) {
  return request({
    url: '/oa/reimbursement',
    method: 'post',
    data: data
  })
}

export function edit(data) {
  return request({
    url: '/oa/reimbursement',
    method: 'put',
    data: data
  })
}

export function remove(id) {
  return request({
    url: '/oa/reimbursement/' + id,
    method: 'delete'
  })
}

export function submit(id) {
  return request({
    url: '/oa/reimbursement/submit/' + id,
    method: 'put'
  })
}

export function withdraw(id) {
  return request({
    url: '/oa/reimbursement/withdraw/' + id,
    method: 'put'
  })
}

export function approve(data) {
  return request({
    url: '/oa/reimbursement/approve',
    method: 'put',
    data: data
  })
}

export function getDetails(id) {
  return request({
    url: '/oa/reimbursement/details/' + id,
    method: 'get'
  })
}

export function getApproveLogs(id) {
  return request({
    url: '/oa/reimbursement/approveLogs/' + id,
    method: 'get'
  })
}
