import Cookies from 'js-cookie'

const TokenKey = 'OA-Token'

/**
 * 会话 Cookie 的安全属性：
 * - SameSite=Lax 阻断跨站请求自动携带令牌
 * - Secure 只在 HTTPS 下下发，避免开发环境 http://localhost 登录失败
 * - 写入与删除必须用同一组属性，否则登出时清不掉旧 Cookie
 */
function cookieAttributes() {
  return {
    path: '/',
    sameSite: 'Lax',
    secure: window.location.protocol === 'https:'
  }
}

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token, cookieAttributes())
}

export function removeToken() {
  return Cookies.remove(TokenKey, cookieAttributes())
}
