export function resolvePath(basePath = '/', routePath = '') {
  if (!routePath) {
    return basePath || '/'
  }
  if (/^(https?:|mailto:|tel:)/.test(routePath)) {
    return routePath
  }
  if (/^(https?:|mailto:|tel:)/.test(basePath)) {
    return basePath
  }
  if (routePath.charAt(0) === '/') {
    return routePath
  }
  const normalizedBase = !basePath || basePath === '/' ? '' : basePath.replace(/\/+$/, '')
  return `${normalizedBase}/${routePath.replace(/^\/+/, '')}` || '/'
}