<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" class="filter-container">
      <el-form-item label="岗位编码"><el-input v-model="queryParams.postCode" clearable @keyup.enter.native="getList" /></el-form-item>
      <el-form-item label="岗位名称"><el-input v-model="queryParams.postName" clearable @keyup.enter.native="getList" /></el-form-item>
      <el-form-item><el-button type="primary" @click="getList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button></el-col></el-row>
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="岗位编码" prop="postCode" width="160" />
      <el-table-column label="岗位名称" prop="postName" />
      <el-table-column label="排序" prop="postSort" width="90" align="center" />
      <el-table-column label="状态" prop="status" width="90" align="center"><template slot-scope="scope">{{ scope.row.status === '0' ? '正常' : '停用' }}</template></el-table-column>
      <el-table-column label="操作" width="160" align="center"><template slot-scope="scope"><el-button size="mini" type="text" @click="handleUpdate(scope.row)">编辑</el-button><el-button size="mini" type="text" @click="handleDelete(scope.row)">删除</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    <el-dialog :title="title" :visible.sync="visible" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="岗位编码" prop="postCode"><el-input v-model="form.postCode" /></el-form-item>
        <el-form-item label="岗位名称" prop="postName"><el-input v-model="form.postName" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.postSort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio label="0">正常</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="submitForm">确定</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
export default {
  name: 'Post',
  data() {
    return { loading: false, tableData: [], total: 0, visible: false, title: '', queryParams: { pageNum: 1, pageSize: 10, postCode: undefined, postName: undefined }, form: {}, rules: { postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }], postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; request({ url: '/system/post/list', method: 'get', params: this.queryParams }).then(res => { this.tableData = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    resetQuery() { this.$refs.queryForm.resetFields(); this.getList() },
    handleAdd() { this.form = { postCode: '', postName: '', postSort: 0, status: '0' }; this.title = '新增岗位'; this.visible = true },
    handleUpdate(row) { this.form = { ...row }; this.title = '修改岗位'; this.visible = true },
    submitForm() { this.$refs.form.validate(valid => { if (!valid) return; const method = this.form.postId ? 'put' : 'post'; request({ url: '/system/post', method, data: this.form }).then(() => { this.$message.success('保存成功'); this.visible = false; this.getList() }) }) },
    handleDelete(row) { this.$confirm('确认删除该岗位吗？', '提示', { type: 'warning' }).then(() => request({ url: '/system/post/' + row.postId, method: 'delete' })).then(() => { this.$message.success('删除成功'); this.getList() }).catch(() => {}) }
  }
}
</script>