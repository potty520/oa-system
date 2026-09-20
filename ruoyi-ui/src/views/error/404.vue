<template>
  <div class="errPage-container">
    <div class="err-content">
      <h1 class="err-title">404</h1>
      <p class="err-desc">抱歉，你访问的页面不存在。</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Page404'
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
  margin: 20px 0 30px;
}
</style>
