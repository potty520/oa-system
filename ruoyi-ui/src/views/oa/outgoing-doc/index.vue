<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="80px" class="filter-container">
      <el-form-item label="文件标题" prop="docTitle"><el-input v-model="queryParams.docTitle" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="发文编号" prop="docNumber"><el-input v-model="queryParams.docNumber" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable><el-option label="草稿" value="0" /><el-option label="审核中" value="1" /><el-option label="已签发" value="2" /><el-option label="已归档" value="3" /></el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button></el-col></el-row>
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="发文标题" prop="docTitle" min-width="160" show-overflow-tooltip />
      <el-table-column label="发文编号" prop="docNumber" width="140" />
      <el-table-column label="拟稿单位" prop="draftUnit" min-width="120" />
      <el-table-column label="拟稿日期" prop="draftDate" width="120" align="center" />
      <el-table-column label="紧急程度" prop="docLevel" width="90" align="center"><template slot-scope="scope">{{ levelLabel(scope.row.docLevel) }}</template></el-table-column>
      <el-table-column label="状态" prop="status" width="90" align="center"><template slot-scope="scope">{{ statusLabel(scope.row.status) }}</template></el-table-column>
      <el-table-column label="签发人" prop="signUserName" width="100" align="center" />
      <el-table-column label="操作" min-width="220" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">查看</el-button>
          <el-button v-if="scope.row.status === '0'" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button v-if="scope.row.status === '0'" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
          <el-button v-if="scope.row.status === '0'" size="mini" type="text" icon="el-icon-s-promotion" @click="handleSubmit(scope.row)">提交审核</el-button>
          <el-button v-if="scope.row.status === '1'" size="mini" type="text" icon="el-icon-edit-outline" @click="handleSign(scope.row)">签发</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="760px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="发文标题" prop="docTitle"><el-input v-model="form.docTitle" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="发文编号" prop="docNumber"><el-input v-model="form.docNumber" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="拟稿单位"><el-input v-model="form.draftUnit" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="拟稿日期"><el-date-picker v-model="form.draftDate" type="date" value-format="yyyy-MM-dd" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8"><el-form-item label="紧急程度"><el-select v-model="form.docLevel" style="width:100%"><el-option label="普通" value="1" /><el-option label="重要" value="2" /><el-option label="紧急" value="3" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="密级"><el-select v-model="form.docSecret" style="width:100%"><el-option label="普通" value="1" /><el-option label="秘密" value="2" /><el-option label="机密" value="3" /><el-option label="绝密" value="4" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="草稿" value="0" /><el-option label="审核中" value="1" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="主送单位"><el-input v-model="form.mainSend" /></el-form-item>
        <el-form-item label="抄送单位"><el-input v-model="form.copySend" /></el-form-item>
        <el-form-item label="内容摘要"><el-input v-model="form.summary" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="附件地址"><el-input v-model="form.attachmentUrl" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitForm">确定</el-button></div>
    </el-dialog>

    <el-dialog title="发文详情" :visible.sync="viewVisible" width="720px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="发文标题" :span="2">{{ viewData.docTitle }}</el-descriptions-item>
        <el-descriptions-item label="发文编号">{{ viewData.docNumber }}</el-descriptions-item>
        <el-descriptions-item label="拟稿单位">{{ viewData.draftUnit }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">{{ levelLabel(viewData.docLevel) }}</el-descriptions-item>
        <el-descriptions-item label="密级">{{ secretLabel(viewData.docSecret) }}</el-descriptions-item>
        <el-descriptions-item label="主送单位">{{ viewData.mainSend }}</el-descriptions-item>
        <el-descriptions-item label="抄送单位">{{ viewData.copySend }}</el-descriptions-item>
        <el-descriptions-item label="内容摘要" :span="2">{{ viewData.summary }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { list, getInfo, add, edit, remove, submit, sign } from '@/api/oa/outgoingDoc'
const emptyForm = () => ({ docId: undefined, docTitle: '', docNumber: '', docTypeId: null, draftUnit: '', draftDate: '', docLevel: '1', docSecret: '1', mainSend: '', copySend: '', summary: '', attachmentUrl: '', signUserId: null, signUserName: '', status: '0', remark: '' })
export default {
  name: 'OutgoingDoc',
  data() {
    return {
      loading: false, tableData: [], total: 0,
      queryParams: { pageNum: 1, pageSize: 10, docTitle: undefined, docNumber: undefined, status: undefined },
      dialogVisible: false, dialogTitle: '', form: emptyForm(),
      rules: { docTitle: [{ required: true, message: '请输入发文标题', trigger: 'blur' }], docNumber: [{ required: true, message: '请输入发文编号', trigger: 'blur' }] },
      viewVisible: false, viewData: {}
    }
  },
  created() { this.getList() },
  methods: {
    levelLabel(value) { return { '1': '普通', '2': '重要', '3': '紧急' }[value] || '普通' },
    secretLabel(value) { return { '1': '普通', '2': '秘密', '3': '机密', '4': '绝密' }[value] || '普通' },
    statusLabel(value) { return { '0': '草稿', '1': '审核中', '2': '已签发', '3': '已归档' }[value] || '未知' },
    getList() { this.loading = true; list(this.queryParams).then(res => { this.tableData = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.$refs.queryForm.resetFields(); this.handleQuery() },
    handleAdd() { this.form = emptyForm(); this.dialogTitle = '新增发文'; this.dialogVisible = true },
    handleUpdate(row) { getInfo(row.docId).then(res => { this.form = Object.assign(emptyForm(), res.data || {}); this.dialogTitle = '编辑发文'; this.dialogVisible = true }) },
    handleView(row) { this.viewData = { ...row }; this.viewVisible = true },
    handleSubmit(row) {
      this.$confirm('确认提交审核吗？', '提示', { type: 'warning' }).then(() => submit(row.docId)).then(() => { this.getList(); this.$message.success('提交成功') }).catch(() => {})
    },
    handleSign(row) { this.$confirm('确认签发该文件吗？', '提示', { type: 'warning' }).then(() => sign(row.docId)).then(() => { this.getList(); this.$message.success('签发成功') }).catch(() => {}) },
    handleDelete(row) { this.$confirm('确认删除该发文吗？', '提示', { type: 'warning' }).then(() => remove(row.docId)).then(() => { this.getList(); this.$message.success('删除成功') }).catch(() => {}) },
    submitForm() { this.$refs.form.validate(valid => { if (!valid) return; const request = this.form.docId ? edit(this.form) : add(this.form); request.then(() => { this.$message.success(this.form.docId ? '修改成功' : '新增成功'); this.dialogVisible = false; this.getList() }) }) },
    resetForm() { this.form = emptyForm(); if (this.$refs.form) this.$refs.form.resetFields() }
  }
}
</script>