import { checkPermi } from '@/utils/permission'

/**
 * v-hasPermi="['oa:announcement:add']"：无权限时直接把元素从 DOM 摘掉。
 * 权限集合在路由守卫里已 await GetInfo 拿到，inserted 阶段即可安全判定。
 */
export default {
  inserted(el, binding) {
    if (!checkPermi(binding.value)) {
      if (el.parentNode) {
        el.parentNode.removeChild(el)
      }
    }
  }
}
