<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" class="filter-container">
      <el-form-item label="模块"><el-input v-model="queryParams.title" clearable /></el-form-item>
      <el-form-item label="状态"><el-select v-model="queryParams.status" clearable><el-option label="正常" value="0" /><el-option label="异常" value="1" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="getList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="模块" prop="title" width="170" />
      <el-table-column label="操作人" prop="operatorName" width="110" />
      <el-table-column label="请求方式" prop="requestMethod" width="90" align="center" />
      <el-table-column label="请求地址" prop="operUrl" min-width="220" show-overflow-tooltip />
      <el-table-column label="IP" prop="operIp" width="130" />
      <el-table-column label="状态" prop="status" width="80" align="center"><template slot-scope="scope"><el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">{{ scope.row.status === '0' ? '正常' : '异常' }}</el-tag></template></el-table-column>
      <el-table-column label="时间" prop="operTime" width="170" align="center" />
      <el-table-column label="异常" prop="errorMsg" min-width="160" show-overflow-tooltip />
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import request from '@/utils/request'
export default {
  name: 'OperLog',
  data() { return { loading: false, tableData: [], total: 0, queryParams: { pageNum: 1, pageSize: 10, title: undefined, status: undefined } } },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; request({ url: '/system/log/list', method: 'get', params: this.queryParams }).then(res => { this.tableData = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    resetQuery() { this.$refs.queryForm.resetFields(); this.getList() }
  }
}
</script>