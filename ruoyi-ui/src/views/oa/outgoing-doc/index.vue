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
    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button v-hasPermi="['oa:outgoingDoc:add']" type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button></el-col></el-row>
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="发文标题" prop="docTitle" min-width="160" show-overflow-tooltip />
      <el-table-column label="发文编号" prop="docNumber" width="140" />
      <el-table-column label="公文类型" prop="docTypeId" width="110" align="center"><template slot-scope="scope">{{ docTypeLabel(scope.row.docTypeId) }}</template></el-table-column>
      <el-table-column label="拟稿单位" prop="draftUnit" min-width="120" />
      <el-table-column label="拟稿日期" prop="draftDate" width="120" align="center" />
      <el-table-column label="紧急程度" prop="docLevel" width="90" align="center"><template slot-scope="scope">{{ levelLabel(scope.row.docLevel) }}</template></el-table-column>
      <el-table-column label="状态" prop="status" width="90" align="center"><template slot-scope="scope">{{ statusLabel(scope.row.status) }}</template></el-table-column>
      <el-table-column label="签发人" prop="signUserName" width="100" align="center" />
      <el-table-column label="操作" min-width="220" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">查看</el-button><el-button v-hasPermi="['oa:doc:action']" size="mini" type="text" icon="el-icon-sort" @click="openAction(scope.row)">流程</el-button>
          <el-button v-if="scope.row.status === '0'" v-hasPermi="['oa:outgoingDoc:edit']" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button v-if="scope.row.status === '0'" v-hasPermi="['oa:outgoingDoc:remove']" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
          <el-button v-if="scope.row.status === '0'" v-hasPermi="['oa:outgoingDoc:edit']" size="mini" type="text" icon="el-icon-s-promotion" @click="handleSubmit(scope.row)">提交审核</el-button>
          <el-button v-if="scope.row.status === '1'" v-hasPermi="['oa:outgoingDoc:edit']" size="mini" type="text" icon="el-icon-edit-outline" @click="handleSign(scope.row)">签发</el-button>
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
          <el-col :span="12"><el-form-item label="公文类型" prop="docTypeId"><el-select v-model="form.docTypeId" filterable clearable placeholder="请选择公文类型" style="width:100%"><el-option v-for="item in docTypeOptions" :key="item.docTypeId" :label="item.typeName" :value="item.docTypeId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="审批流程"><el-select v-model="form.flowId" clearable placeholder="仅一个流程时自动匹配" style="width:100%"><el-option v-for="item in flowOptions" :key="item.flowId" :label="item.flowName" :value="item.flowId" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="拟稿单位"><el-input v-model="form.draftUnit" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="拟稿日期"><el-date-picker v-model="form.draftDate" type="date" value-format="yyyy-MM-dd" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8"><el-form-item label="紧急程度"><el-select v-model="form.docLevel" style="width:100%"><el-option label="普通" value="1" /><el-option label="重要" value="2" /><el-option label="紧急" value="3" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="密级"><el-select v-model="form.docSecret" style="width:100%"><el-option label="普通" value="1" /><el-option label="秘密" value="2" /><el-option label="机密" value="3" /><el-option label="绝密" value="4" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="签发人" prop="signUserId"><el-select v-model="form.signUserId" filterable clearable placeholder="请选择签发人" style="width:100%"><el-option v-for="item in userOptions" :key="item.userId" :label="userLabel(item)" :value="item.userId" /></el-select></el-form-item></el-col>
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
        <el-descriptions-item label="公文类型">{{ docTypeLabel(viewData.docTypeId) }}</el-descriptions-item>
        <el-descriptions-item label="拟稿单位">{{ viewData.draftUnit }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">{{ levelLabel(viewData.docLevel) }}</el-descriptions-item>
        <el-descriptions-item label="密级">{{ secretLabel(viewData.docSecret) }}</el-descriptions-item>
        <el-descriptions-item label="主送单位">{{ viewData.mainSend }}</el-descriptions-item>
        <el-descriptions-item label="抄送单位">{{ viewData.copySend }}</el-descriptions-item>
        <el-descriptions-item label="内容摘要" :span="2">{{ viewData.summary }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
    <doc-action-dialog :doc-type="'2'" :doc-id="actionDocId" :visible.sync="actionVisible" @success="getList" />
  </div>
</template>

<script>
import { list, getInfo, add, edit, remove, submit, sign } from '@/api/oa/outgoingDoc'
import { listUserOptions } from '@/api/system/user'
import { listDocTypeOptions } from '@/api/oa/docProcess'
import { list as flowList } from '@/api/oa/docFlow'
import { checkPermi } from '@/utils/permission'
import DocActionDialog from '../components/DocActionDialog'
const emptyForm = () => ({ docId: undefined, docTitle: '', docNumber: '', docTypeId: null, flowId: null, draftUnit: '', draftDate: '', docLevel: '1', docSecret: '1', mainSend: '', copySend: '', summary: '', attachmentUrl: '', signUserId: null, signUserName: '', remark: '' })
export default {
  name: 'OutgoingDoc',
  components: { DocActionDialog },
  data() {
    return {
      loading: false, tableData: [], total: 0, docTypeOptions: [], flowOptions: [], userOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, docTitle: undefined, docNumber: undefined, status: undefined },
      dialogVisible: false, dialogTitle: '', form: emptyForm(),
      // 后端不允许起草人自签，所以送审前必须先指定签发人
      rules: { docTitle: [{ required: true, message: '请输入发文标题', trigger: 'blur' }], docNumber: [{ required: true, message: '请输入发文编号', trigger: 'blur' }], signUserId: [{ required: true, message: '请选择签发人', trigger: 'change' }] },
      viewVisible: false, viewData: {}, actionVisible: false, actionDocId: null
    }
  },
  created() { this.getDocTypes(); this.getFlows(); this.getUsers(); this.getList() },
  methods: {
    userLabel(item) { return item.nickName ? item.nickName + '（' + item.userName + '）' : item.userName },
    levelLabel(value) { return { '1': '普通', '2': '重要', '3': '紧急' }[value] || '普通' },
    secretLabel(value) { return { '1': '普通', '2': '秘密', '3': '机密', '4': '绝密' }[value] || '普通' },
    statusLabel(value) { return { '0': '草稿', '1': '审核中', '2': '已签发', '3': '已归档' }[value] || '未知' },
    // 公文类型走登录即可读的 /oa/docType/options，普通用户也能把 docTypeId 显示成名称；
    // 流程下拉仍需 oa:docFlow:list，没有权限就跳过，否则必然 403
    docTypeLabel(id) { if (id === null || id === undefined || id === '') return '未选择'; const item = this.docTypeOptions.find(option => String(option.docTypeId) === String(id)); return item ? item.typeName : '未知类型' },
    getDocTypes() { listDocTypeOptions().then(res => { this.docTypeOptions = res.data || [] }).catch(() => {}) },
    getFlows() { if (!checkPermi('oa:docFlow:list')) return; flowList({ pageNum: 1, pageSize: 100, flowType: '2', status: '0' }).then(res => { this.flowOptions = res.rows || [] }).catch(() => {}) },
    getUsers() { listUserOptions().then(res => { this.userOptions = res.data || [] }).catch(() => {}) },
    getList() { this.loading = true; list(this.queryParams).then(res => { this.tableData = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.$refs.queryForm.resetFields(); this.handleQuery() },
    handleAdd() { this.form = emptyForm(); if (this.flowOptions.length === 1) { this.form.flowId = this.flowOptions[0].flowId } this.dialogTitle = '新增发文'; this.dialogVisible = true },
    handleUpdate(row) { getInfo(row.docId).then(res => { this.form = Object.assign(emptyForm(), res.data || {}); this.dialogTitle = '编辑发文'; this.dialogVisible = true }).catch(() => {}) },
    handleView(row) { this.viewData = { ...row }; this.viewVisible = true },
    openAction(row) { this.actionDocId = row.docId; this.actionVisible = true },
    handleSubmit(row) {
      this.$confirm('确认提交审核吗？', '提示', { type: 'warning' }).then(() => submit(row.docId)).then(() => { this.getList(); this.$message.success('提交成功') }).catch(() => {})
    },
    handleSign(row) { this.$confirm('确认签发该文件吗？', '提示', { type: 'warning' }).then(() => sign(row.docId)).then(() => { this.getList(); this.$message.success('签发成功') }).catch(() => {}) },
    handleDelete(row) { this.$confirm('确认删除该发文吗？', '提示', { type: 'warning' }).then(() => remove(row.docId)).then(() => { this.getList(); this.$message.success('删除成功') }).catch(() => {}) },
    submitForm() { this.$refs.form.validate(valid => { if (!valid) return; const request = this.form.docId ? edit(this.form) : add(this.form); request.then(() => { this.$message.success(this.form.docId ? '修改成功' : '新增成功'); this.dialogVisible = false; this.getList() }).catch(() => {}) }) },
    resetForm() { this.form = emptyForm(); if (this.$refs.form) this.$refs.form.resetFields() }
  }
}
</script>