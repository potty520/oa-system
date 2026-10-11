<template>
  <div class="errPage-container">
    <div class="err-content">
      <h1 class="err-title">403</h1>
      <p class="err-desc">抱歉，你没有访问该页面的权限。</p>
      <p class="err-hint">如需访问，请联系管理员为你的角色分配对应菜单权限。</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      <el-button @click="$router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Page403'
}
</script>

<style scoped>
.errPage-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  text-align: center;
}

.err-content {
  padding: 20px;
}

.err-title {
  font-size: 120px;
  font-weight: bold;
  color: #409eff;
  margin: 0;
  line-height: 1;
}

.err-desc {
  font-size: 18px;
  color: #606266;
  margin: 20px 0 8px;
}

.err-hint {
  font-size: 13px;
  color: #909399;
  margin: 0 0 26px;
}
</style>
