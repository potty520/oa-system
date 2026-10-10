<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="80px" class="filter-container">
      <el-form-item label="流程名称" prop="flowName"><el-input v-model="queryParams.flowName" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="流程类型" prop="flowType"><el-select v-model="queryParams.flowType" clearable><el-option label="收文" value="1" /><el-option label="发文" value="2" /></el-select></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" clearable><el-option label="启用" value="0" /><el-option label="停用" value="1" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button v-hasPermi="['oa:docFlow:add']" type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button></el-col></el-row>
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="流程名称" prop="flowName" min-width="160" />
      <el-table-column label="流程类型" prop="flowType" width="100" align="center"><template slot-scope="scope">{{ scope.row.flowType === '1' ? '收文' : '发文' }}</template></el-table-column>
      <el-table-column label="流程描述" prop="description" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" prop="status" width="90" align="center"><template slot-scope="scope"><el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
      <el-table-column label="操作" width="180" align="center">
        <template slot-scope="scope"><el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">查看</el-button><el-button v-hasPermi="['oa:docFlow:edit']" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">编辑</el-button><el-button v-hasPermi="['oa:docFlow:remove']" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="流程名称" prop="flowName"><el-input v-model="form.flowName" /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="流程类型" prop="flowType"><el-select v-model="form.flowType" style="width:100%"><el-option label="收文" value="1" /><el-option label="发文" value="2" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态" prop="status"><el-select v-model="form.status" style="width:100%"><el-option label="启用" value="0" /><el-option label="停用" value="1" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="流程描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-divider content-position="left">流程步骤</el-divider>
        <div v-for="(step, index) in form.steps" :key="index" class="step-row">
          <el-row :gutter="10">
            <el-col :span="8"><el-input v-model="step.stepName" placeholder="步骤名称" /></el-col>
            <el-col :span="5"><el-input-number v-model="step.stepOrder" :min="1" controls-position="right" style="width:100%" /></el-col>
            <el-col :span="6"><el-input-number v-model="step.roleId" :min="1" controls-position="right" style="width:100%" placeholder="角色ID" /></el-col>
            <el-col :span="3"><el-select v-model="step.isSign"><el-option label="签批" value="1" /><el-option label="会签" value="0" /></el-select></el-col>
            <el-col :span="2"><el-button type="danger" icon="el-icon-delete" circle @click="removeStep(index)" /></el-col>
          </el-row>
        </div>
        <el-button type="primary" plain icon="el-icon-plus" @click="addStep">添加步骤</el-button>
      </el-form>
      <div slot="footer"><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitForm">确定</el-button></div>
    </el-dialog>

    <el-dialog title="流程详情" :visible.sync="viewVisible" width="720px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="流程名称" :span="2">{{ viewData.flowName }}</el-descriptions-item>
        <el-descriptions-item label="流程类型">{{ viewData.flowType === '1' ? '收文' : '发文' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ viewData.status === '0' ? '启用' : '停用' }}</el-descriptions-item>
        <el-descriptions-item label="流程描述" :span="2">{{ viewData.description }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="viewData.steps || []" border style="margin-top:15px">
        <el-table-column label="步骤名称" prop="stepName" /><el-table-column label="顺序" prop="stepOrder" width="80" /><el-table-column label="角色ID" prop="roleId" width="100" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { list, getInfo, add, edit, remove } from '@/api/oa/docFlow'
const emptyStep = () => ({ stepName: '', stepOrder: 1, roleId: null, isSign: '1' })
const emptyForm = () => ({ flowId: undefined, flowName: '', flowType: '1', description: '', status: '0', steps: [emptyStep()] })
export default {
  name: 'DocFlow',
  data() {
    return {
      loading: false, tableData: [], total: 0,
      queryParams: { pageNum: 1, pageSize: 10, flowName: undefined, flowType: undefined, status: undefined },
      dialogVisible: false, dialogTitle: '', form: emptyForm(),
      rules: { flowName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }], flowType: [{ required: true, message: '请选择流程类型', trigger: 'change' }] },
      viewVisible: false, viewData: {}
    }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; list(this.queryParams).then(res => { this.tableData = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.$refs.queryForm.resetFields(); this.handleQuery() },
    handleAdd() { this.form = emptyForm(); this.dialogTitle = '新增流程'; this.dialogVisible = true },
    handleUpdate(row) { getInfo(row.flowId).then(res => { const data = res.data || {}; data.steps = data.steps && data.steps.length ? data.steps : [emptyStep()]; this.form = data; this.dialogTitle = '编辑流程'; this.dialogVisible = true }).catch(() => {}) },
    handleView(row) { getInfo(row.flowId).then(res => { this.viewData = res.data || {}; this.viewVisible = true }).catch(() => {}) },
    handleDelete(row) { this.$confirm('确认删除该流程吗？', '提示', { type: 'warning' }).then(() => remove(row.flowId)).then(() => { this.getList(); this.$message.success('删除成功') }).catch(() => {}) },
    addStep() { this.form.steps.push({ ...emptyStep(), stepOrder: this.form.steps.length + 1 }) },
    removeStep(index) { if (this.form.steps.length > 1) this.form.steps.splice(index, 1) },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.form.steps = this.form.steps.map((step, index) => ({ ...step, stepOrder: index + 1 }))
        const request = this.form.flowId ? edit(this.form) : add(this.form)
        request.then(() => { this.$message.success(this.form.flowId ? '修改成功' : '新增成功'); this.dialogVisible = false; this.getList() }).catch(() => {})
      })
    }
  }
}
</script>

<style scoped>
.step-row { padding: 8px; margin-bottom: 8px; background: #fafafa; border: 1px dashed #dcdfe6; border-radius: 4px; }
</style>