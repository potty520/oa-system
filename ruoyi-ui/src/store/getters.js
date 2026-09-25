export default {
  token: state => state.user.token,
  userInfo: state => state.user.userInfo,
  permissions: state => state.user.permissions,
  sidebar: state => state.app.sidebar,
  device: state => state.app.device,
  visitedViews: state => state.tagsView.visitedViews,
  cachedViews: state => state.tagsView.cachedViews,
  routes: state => state.permission.routes
}
