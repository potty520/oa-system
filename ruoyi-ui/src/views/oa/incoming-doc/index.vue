<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="80px" class="filter-container">
      <el-form-item label="文件标题" prop="docTitle"><el-input v-model="queryParams.docTitle" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="收文编号" prop="docNumber"><el-input v-model="queryParams.docNumber" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="处理状态" prop="status">
        <el-select v-model="queryParams.status" clearable>
          <el-option label="待签收" value="0" /><el-option label="办理中" value="1" /><el-option label="已办结" value="2" /><el-option label="已归档" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button v-if="hasPermi('oa:incomingDoc:add')" type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button></el-col></el-row>
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="收文标题" prop="docTitle" min-width="160" show-overflow-tooltip />
      <el-table-column label="收文编号" prop="docNumber" width="140" />
      <el-table-column label="公文类型" prop="docTypeId" width="110" align="center"><template slot-scope="scope">{{ docTypeLabel(scope.row.docTypeId) }}</template></el-table-column>
      <el-table-column label="来文单位" prop="sourceUnit" min-width="130" show-overflow-tooltip />
      <el-table-column label="紧急程度" prop="docLevel" width="90" align="center"><template slot-scope="scope">{{ levelLabel(scope.row.docLevel) }}</template></el-table-column>
      <el-table-column label="密级" prop="docSecret" width="80" align="center"><template slot-scope="scope">{{ secretLabel(scope.row.docSecret) }}</template></el-table-column>
      <el-table-column label="状态" prop="status" width="90" align="center"><template slot-scope="scope">{{ statusLabel(scope.row.status) }}</template></el-table-column>
      <el-table-column label="收文日期" prop="receiveDate" width="120" align="center" />
      <el-table-column label="办理人" prop="handlerName" width="100" align="center" />
      <el-table-column label="操作" min-width="220" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">查看</el-button><el-button v-if="hasPermi('oa:doc:action')" size="mini" type="text" icon="el-icon-sort" @click="openAction(scope.row)">流程</el-button>
          <el-button v-if="hasPermi('oa:incomingDoc:edit') && scope.row.status === '0'" size="mini" type="text" icon="el-icon-check" @click="handleSign(scope.row)">签收</el-button>
          <el-button v-if="hasPermi('oa:incomingDoc:edit') && scope.row.status === '1'" size="mini" type="text" icon="el-icon-finished" @click="handleComplete(scope.row)">办结</el-button>
          <el-button v-if="hasPermi('oa:incomingDoc:edit') && scope.row.status === '0'" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button v-if="hasPermi('oa:incomingDoc:remove') && scope.row.status === '0'" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="收文标题" prop="docTitle"><el-input v-model="form.docTitle" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="收文编号" prop="docNumber"><el-input v-model="form.docNumber" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="公文类型" prop="docTypeId"><el-select v-model="form.docTypeId" filterable clearable placeholder="请选择公文类型" style="width:100%"><el-option v-for="item in docTypeOptions" :key="item.docTypeId" :label="item.typeName" :value="item.docTypeId" :disabled="item.status !== '0'" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="审批流程"><el-select v-model="form.flowId" clearable placeholder="仅一个流程时自动匹配" style="width:100%"><el-option v-for="item in flowOptions" :key="item.flowId" :label="item.flowName" :value="item.flowId" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="来文单位" prop="sourceUnit"><el-input v-model="form.sourceUnit" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="收文日期" prop="receiveDate"><el-date-picker v-model="form.receiveDate" type="date" value-format="yyyy-MM-dd" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8"><el-form-item label="紧急程度" prop="docLevel"><el-select v-model="form.docLevel" style="width:100%"><el-option label="普通" value="1" /><el-option label="重要" value="2" /><el-option label="紧急" value="3" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="密级" prop="docSecret"><el-select v-model="form.docSecret" style="width:100%"><el-option label="普通" value="1" /><el-option label="秘密" value="2" /><el-option label="机密" value="3" /><el-option label="绝密" value="4" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="办理人"><el-input v-model="form.handlerName" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="内容摘要" prop="summary"><el-input v-model="form.summary" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="附件地址"><el-input v-model="form.attachmentUrl" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitForm">确定</el-button></div>
    </el-dialog>

    <el-dialog title="收文详情" :visible.sync="viewVisible" width="720px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="收文标题" :span="2">{{ viewData.docTitle }}</el-descriptions-item>
        <el-descriptions-item label="收文编号">{{ viewData.docNumber }}</el-descriptions-item>
        <el-descriptions-item label="公文类型">{{ docTypeLabel(viewData.docTypeId) }}</el-descriptions-item>
        <el-descriptions-item label="来文单位">{{ viewData.sourceUnit }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">{{ levelLabel(viewData.docLevel) }}</el-descriptions-item>
        <el-descriptions-item label="密级">{{ secretLabel(viewData.docSecret) }}</el-descriptions-item>
        <el-descriptions-item label="收文日期">{{ viewData.receiveDate }}</el-descriptions-item>
        <el-descriptions-item label="办理人">{{ viewData.handlerName }}</el-descriptions-item>
        <el-descriptions-item label="内容摘要" :span="2">{{ viewData.summary }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
    <doc-action-dialog :doc-type="'1'" :doc-id="actionDocId" :visible.sync="actionVisible" @success="getList" />
  </div>
</template>

<script>
import { list, getInfo, add, edit, remove, sign, complete } from '@/api/oa/incomingDoc'
import { docTypeList } from '@/api/oa/docProcess'
import { list as flowList } from '@/api/oa/docFlow'
import DocActionDialog from '../components/DocActionDialog'
const emptyForm = () => ({ docId: undefined, docTitle: '', docNumber: '', docTypeId: null, sourceUnit: '', receiveDate: '', docLevel: '1', docSecret: '1', summary: '', attachmentUrl: '', handlerId: null, handlerName: '', remark: '' })
export default {
  name: 'IncomingDoc',
  components: { DocActionDialog },
  data() {
    return {
      loading: false, tableData: [], total: 0, docTypeOptions: [], flowOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, docTitle: undefined, docNumber: undefined, status: undefined },
      dialogVisible: false, dialogTitle: '', form: emptyForm(),
      rules: { docTitle: [{ required: true, message: '请输入收文标题', trigger: 'blur' }], docNumber: [{ required: true, message: '请输入收文编号', trigger: 'blur' }] },
      viewVisible: false, viewData: {}, actionVisible: false, actionDocId: null
    }
  },
  created() { this.getDocTypes(); this.getFlows(); this.getList() },
  methods: {
    hasPermi(permission) { const permissions = this.$store.getters.permissions || []; const user = this.$store.getters.userInfo || {}; return user.userId === 1 || permissions.includes('*:*:*') || permissions.includes(permission) },
    levelLabel(value) { return { '1': '普通', '2': '重要', '3': '紧急' }[value] || '普通' },
    secretLabel(value) { return { '1': '普通', '2': '秘密', '3': '机密', '4': '绝密' }[value] || '普通' },
    statusLabel(value) { return { '0': '待签收', '1': '办理中', '2': '已办结', '3': '已归档' }[value] || '未知' },
    docTypeLabel(id) { const item = this.docTypeOptions.find(option => String(option.docTypeId) === String(id)); return item ? item.typeName : '未选择' },
    getDocTypes() { docTypeList({}).then(res => { this.docTypeOptions = res.data || [] }) },
    getFlows() { flowList({ pageNum: 1, pageSize: 100, flowType: '1', status: '0' }).then(res => { this.flowOptions = res.rows || [] }) },
    getList() { this.loading = true; list(this.queryParams).then(res => { this.tableData = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.$refs.queryForm.resetFields(); this.handleQuery() },
    handleAdd() { this.form = emptyForm(); if (this.flowOptions.length === 1) { this.form.flowId = this.flowOptions[0].flowId } this.dialogTitle = '新增收文'; this.dialogVisible = true },
    handleUpdate(row) { getInfo(row.docId).then(res => { this.form = Object.assign(emptyForm(), res.data || {}); this.dialogTitle = '编辑收文'; this.dialogVisible = true }) },
    handleView(row) { this.viewData = { ...row }; this.viewVisible = true },
    openAction(row) { this.actionDocId = row.docId; this.actionVisible = true },
    handleSign(row) { this.$confirm('确认签收该文件吗？', '提示', { type: 'warning' }).then(() => sign(row.docId)).then(() => { this.getList(); this.$message.success('签收成功') }).catch(() => {}) },
    handleComplete(row) { this.$confirm('确认办结该文件吗？', '提示', { type: 'warning' }).then(() => complete(row.docId)).then(() => { this.getList(); this.$message.success('办结成功') }).catch(() => {}) },
    handleDelete(row) { this.$confirm('确认删除该收文吗？', '提示', { type: 'warning' }).then(() => remove(row.docId)).then(() => { this.getList(); this.$message.success('删除成功') }).catch(() => {}) },
    submitForm() { this.$refs.form.validate(valid => { if (!valid) return; const request = this.form.docId ? edit(this.form) : add(this.form); request.then(() => { this.$message.success(this.form.docId ? '修改成功' : '新增成功'); this.dialogVisible = false; this.getList() }) }) },
    resetForm() { this.form = emptyForm(); if (this.$refs.form) this.$refs.form.resetFields() }
  }
}
</script>