<script>
/**
 * TagsView“刷新页面”的落脚点：立刻用目标地址替换掉 /redirect/:path，
 * 既重建了目标页面的组件，又不在浏览历史里留下多余记录。
 */
export default {
  name: 'Redirect',
  beforeCreate() {
    const { params, query } = this.$route
    const { path } = params
    this.$router.replace({ path: '/' + path, query })
  },
  render: h => h()
}
</script>
