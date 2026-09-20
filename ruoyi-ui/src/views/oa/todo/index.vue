<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px" class="filter-container">
      <el-form-item label="任务标题" prop="todoTitle"><el-input v-model="queryParams.todoTitle" placeholder="请输入任务标题" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="任务状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option label="未完成" value="0" /><el-option label="已完成" value="1" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="任务标题" prop="todoTitle" min-width="160" show-overflow-tooltip />
      <el-table-column label="任务类型" prop="todoType" width="110" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.todoType === 'leave'" type="primary">请假审批</el-tag>
          <el-tag v-else-if="scope.row.todoType === 'reimbursement'" type="warning">报销审批</el-tag>
          <el-tag v-else type="info">普通</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="任务状态" prop="status" width="100" align="center"><template slot-scope="scope"><el-tag v-if="scope.row.status === '0'" type="warning">未完成</el-tag><el-tag v-else type="success">已完成</el-tag></template></el-table-column>
      <el-table-column label="申请人" prop="applyUser" min-width="120" show-overflow-tooltip />
      <el-table-column label="申请时间" prop="applyTime" width="160" align="center" />
      <el-table-column label="操作" width="120" align="center"><template slot-scope="scope"><el-button v-if="scope.row.status === '0'" size="mini" type="text" icon="el-icon-check" @click="handleComplete(scope.row)">完成</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { list, complete } from '@/api/oa/todo'
export default {
  name: 'Todo',
  data() {
    return { loading: false, tableData: [], total: 0, queryParams: { pageNum: 1, pageSize: 10, todoTitle: undefined, status: undefined } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; list(this.queryParams).then(res => { this.tableData = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.$refs.queryForm.resetFields(); this.handleQuery() },
    handleComplete(row) {
      this.$confirm('确定标记该任务为已完成吗？', '提示', { type: 'warning' }).then(() => complete(row.todoType, row.relatedId)).then(() => {
        this.getList(); this.$message.success('操作成功')
      }).catch(() => {})
    }
  }
}
</script>