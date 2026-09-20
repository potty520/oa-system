import request from '@/utils/request'

export function timeline(docType, docId) {
  return request({ url: `/oa/doc/timeline/${docType}/${docId}`, method: 'get' })
}

export function action(docType, docId, data) {
  return request({ url: `/oa/doc/action/${docType}/${docId}`, method: 'post', data })
}

export function myCirculation() {
  return request({ url: '/oa/doc/my/circulation', method: 'get' })
}

export function markRead(circulationId) {
  return request({ url: `/oa/doc/circulation/read/${circulationId}`, method: 'put' })
}

export function myDone() {
  return request({ url: '/oa/doc/my/done', method: 'get' })
}

export function archiveList(query) {
  return request({ url: '/oa/doc/archive/list', method: 'get', params: query })
}

export function docTypeList(query) {
  return request({ url: '/oa/docType/list', method: 'get', params: query })
}

export function addDocType(data) {
  return request({ url: '/oa/docType', method: 'post', data })
}

export function editDocType(data) {
  return request({ url: '/oa/docType', method: 'put', data })
}

export function removeDocType(id) {
  return request({ url: `/oa/docType/${id}`, method: 'delete' })
}

export function dashboardStats() {
  return request({ url: '/oa/dashboard/stats', method: 'get' })
}
export function docTemplateList(query) {
  return request({ url: '/oa/docTemplate/list', method: 'get', params: query })
}

export function addDocTemplate(data) {
  return request({ url: '/oa/docTemplate', method: 'post', data })
}

export function editDocTemplate(data) {
  return request({ url: '/oa/docTemplate', method: 'put', data })
}

export function removeDocTemplate(id) {
  return request({ url: `/oa/docTemplate/${id}`, method: 'delete' })
}