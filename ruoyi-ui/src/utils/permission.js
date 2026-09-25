import store from '@/store'

const ALL_PERMISSION = '*:*:*'
const SUPER_ADMIN_USER_ID = 1

function isSuperAdmin() {
  const user = store.getters.userInfo || {}
  return user.userId === SUPER_ADMIN_USER_ID
}

/**
 * 与后端 PermissionService 保持同一口径：内置超管直接放行，
 * 其余用户按已授权限集合匹配。传入数组表示“满足其一”。
 */
export function checkPermi(value) {
  if (!value) {
    return false
  }
  if (isSuperAdmin()) {
    return true
  }
  const owned = store.getters.permissions || []
  if (owned.indexOf(ALL_PERMISSION) !== -1) {
    return true
  }
  const required = Array.isArray(value) ? value : [value]
  if (required.length === 0) {
    return false
  }
  return required.some(permission => owned.indexOf(permission) !== -1)
}
