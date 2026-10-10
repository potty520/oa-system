<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="loadAll">
      <el-tab-pane label="我的传阅" name="circulation">
        <el-table v-loading="loading" :data="circulationList" border>
          <el-table-column label="文号" prop="docNumber" width="150" />
          <el-table-column label="标题" prop="docTitle" min-width="220" show-overflow-tooltip />
          <el-table-column label="类型" prop="docType" width="100" align="center">
            <template slot-scope="scope">{{ scope.row.docType === '1' ? '收文' : '发文' }}</template>
          </el-table-column>
          <el-table-column label="状态" prop="readStatus" width="90" align="center">
            <template slot-scope="scope"><el-tag :type="scope.row.readStatus === '1' ? 'success' : 'warning'">{{ scope.row.readStatus === '1' ? '已读' : '未读' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="传阅时间" prop="createTime" width="170" align="center" />
          <el-table-column label="操作" width="120" align="center">
            <template slot-scope="scope"><el-button v-if="scope.row.readStatus === '0'" size="mini" type="text" @click="handleRead(scope.row)">标记已读</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="我的已办" name="done">
        <el-table v-loading="loading" :data="doneList" border>
          <el-table-column label="公文ID" prop="docId" width="100" align="center" />
          <el-table-column label="类型" prop="docType" width="100" align="center"><template slot-scope="scope">{{ scope.row.docType === '1' ? '收文' : '发文' }}</template></el-table-column>
          <el-table-column label="办理结果" prop="approveResult" width="110" align="center" />
          <el-table-column label="办理意见" prop="approveRemark" min-width="220" show-overflow-tooltip />
          <el-table-column label="办理时间" prop="approveTime" width="170" align="center" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="canManageArchive" label="归档台账" name="archive">
        <el-table v-loading="loading" :data="archiveRows" border>
          <el-table-column label="归档编号" prop="archiveCode" width="160" />
          <el-table-column label="公文ID" prop="docId" width="100" align="center" />
          <el-table-column label="类型" prop="docType" width="100" align="center"><template slot-scope="scope">{{ scope.row.docType === '1' ? '收文' : '发文' }}</template></el-table-column>
          <el-table-column label="归档位置" prop="archiveLocation" min-width="160" />
          <el-table-column label="归档人" prop="archiveUserName" width="120" />
          <el-table-column label="归档时间" prop="archiveTime" width="170" align="center" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="canManageDocType" label="公文类型" name="type">
        <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button v-hasPermi="['oa:docType:add']" type="primary" plain icon="el-icon-plus" @click="openTypeDialog()">新增类型</el-button></el-col></el-row>
        <el-table v-loading="loading" :data="typeList" border>
          <el-table-column label="类型名称" prop="typeName" />
          <el-table-column label="类型编码" prop="typeCode" width="160" />
          <el-table-column label="排序" prop="sortOrder" width="90" align="center" />
          <el-table-column label="状态" prop="status" width="90" align="center"><template slot-scope="scope">{{ scope.row.status === '0' ? '启用' : '停用' }}</template></el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template slot-scope="scope"><el-button v-hasPermi="['oa:docType:edit']" size="mini" type="text" @click="openTypeDialog(scope.row)">编辑</el-button><el-button v-hasPermi="['oa:docType:remove']" size="mini" type="text" @click="deleteType(scope.row)">删除</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="canManageTemplate" label="模板与套红" name="template">
        <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button v-hasPermi="['oa:docTemplate:add']" type="primary" plain icon="el-icon-plus" @click="openTemplateDialog()">新增模板</el-button></el-col></el-row>
        <el-table v-loading="loading" :data="templateList" border>
          <el-table-column label="模板名称" prop="templateName" />
          <el-table-column label="公文类型" prop="docTypeId" width="120" align="center"><template slot-scope="scope">{{ docTypeLabel(scope.row.docTypeId) }}</template></el-table-column>
          <el-table-column label="文书方向" prop="docType" width="100" align="center"><template slot-scope="scope">{{ scope.row.docType === '1' ? '收文' : '发文' }}</template></el-table-column>
          <el-table-column label="红头文字" prop="redHeaderText" min-width="180" />
          <el-table-column label="标题字号" prop="titleSize" width="90" align="center" />
          <el-table-column label="状态" prop="status" width="90" align="center"><template slot-scope="scope">{{ scope.row.status === '0' ? '启用' : '停用' }}</template></el-table-column>
          <el-table-column label="操作" width="180" align="center"><template slot-scope="scope"><el-button size="mini" type="text" @click="previewTemplate(scope.row)">套红预览</el-button><el-button v-hasPermi="['oa:docTemplate:edit']" size="mini" type="text" @click="openTemplateDialog(scope.row)">编辑</el-button><el-button v-hasPermi="['oa:docTemplate:remove']" size="mini" type="text" @click="deleteTemplate(scope.row)">删除</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="canManageDocument" label="附件管理" name="document">
        <el-upload v-hasPermi="['oa:document:upload']" :http-request="uploadDocument" :show-file-list="false" action="#">
          <el-button type="primary" icon="el-icon-upload2">上传附件</el-button>
        </el-upload>
        <el-table v-loading="loading" :data="documentList" border style="margin-top:12px">
          <el-table-column label="文件名" prop="documentName" min-width="220" show-overflow-tooltip />
          <el-table-column label="类型" prop="documentType" width="120" />
          <el-table-column label="大小" width="110" align="right"><template slot-scope="scope">{{ formatSize(scope.row.fileSize) }}</template></el-table-column>
          <el-table-column label="上传人" prop="userName" width="110" />
          <el-table-column label="上传时间" prop="createTime" width="170" align="center" />
          <el-table-column label="操作" width="160" align="center"><template slot-scope="scope"><el-button v-hasPermi="['oa:document:query']" size="mini" type="text" @click="downloadDocument(scope.row)">下载</el-button><el-button v-hasPermi="['oa:document:remove']" size="mini" type="text" @click="deleteDocument(scope.row)">删除</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

    </el-tabs>

    <el-dialog title="公文类型" :visible.sync="typeDialogVisible" width="520px" append-to-body>
      <el-form ref="typeForm" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="类型名称" prop="typeName"><el-input v-model="typeForm.typeName" /></el-form-item>
        <el-form-item label="类型编码" prop="typeCode"><el-input v-model="typeForm.typeCode" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="typeForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="typeForm.status"><el-radio label="0">启用</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="typeDialogVisible = false">取消</el-button><el-button type="primary" @click="submitType">确定</el-button></div>
    </el-dialog>

    <el-dialog title="模板与套红" :visible.sync="templateDialogVisible" width="760px" append-to-body>
      <el-form ref="templateForm" :model="templateForm" :rules="templateRules" label-width="100px">
        <el-form-item label="模板名称" prop="templateName"><el-input v-model="templateForm.templateName" placeholder="请输入模板名称" /></el-form-item>
        <el-form-item label="文书方向"><el-select v-model="templateForm.docType" style="width:100%"><el-option label="收文" value="1" /><el-option label="发文" value="2" /></el-select></el-form-item>
        <el-form-item label="公文类型" prop="docTypeId"><el-select v-model="templateForm.docTypeId" filterable clearable placeholder="请选择公文类型" style="width:100%"><el-option v-for="item in docTypeOptions" :key="item.docTypeId" :label="item.typeName" :value="item.docTypeId" :disabled="item.status !== '0'" /></el-select></el-form-item>
        <el-form-item label="红头文字"><el-input v-model="templateForm.redHeaderText" placeholder="请输入红头文字" @input="templatePreview = { ...templateForm }" /></el-form-item>
        <el-form-item label="标题字体/字号">
          <el-input v-model="templateForm.titleFont" placeholder="字体" style="width:180px;margin-right:10px" />
          <el-input-number v-model="templateForm.titleSize" :min="8" :max="72" />
        </el-form-item>
        <el-form-item label="正文字体/字号">
          <el-input v-model="templateForm.bodyFont" placeholder="字体" style="width:180px;margin-right:10px" />
          <el-input-number v-model="templateForm.bodySize" :min="8" :max="72" />
        </el-form-item>
        <el-form-item label="模板内容"><el-input v-model="templateForm.templateContent" type="textarea" :rows="3" placeholder="请输入模板内容" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="templateForm.status"><el-radio label="0">启用</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div class="red-header-preview">
        <h1 :style="{ fontFamily: templatePreview.titleFont, fontSize: templatePreview.titleSize + 'px' }">{{ templatePreview.redHeaderText || '红头文字预览' }}</h1>
        <p :style="{ fontFamily: templatePreview.bodyFont, fontSize: templatePreview.bodySize + 'px' }">{{ templatePreview.templateContent || '模板内容预览' }}</p>
      </div>
      <div slot="footer"><el-button @click="templateDialogVisible = false">取消</el-button><el-button type="primary" @click="submitTemplate">保存并预览</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { myCirculation, markRead, myDone, archiveList, docTypeList, addDocType, editDocType, removeDocType, docTemplateList, addDocTemplate, editDocTemplate, removeDocTemplate } from '@/api/oa/docProcess'
import { list as documentListApi, upload as uploadDocumentApi, remove as removeDocumentApi, download as downloadDocumentApi } from '@/api/oa/document'
import { checkPermi } from '@/utils/permission'

export default {
  name: 'DocCenter',
  data() {
    return {
      activeTab: 'circulation',
      loading: false,
      circulationList: [],
      doneList: [],
      archiveRows: [],
      typeList: [],
      templateList: [],
      docTypeOptions: [],
      documentList: [],
      typeDialogVisible: false,
      typeForm: {},
      templateDialogVisible: false,
      templateForm: {},
      templatePreview: {},
      typeRules: {  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }], typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }] },
      templateRules: { templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }], docTypeId: [{ required: true, message: '请选择公文类型', trigger: 'change' }] }
    }
  },
  computed: {
    // 归档台账是全单位的公文清单，属管理口径：后端用专用权限 oa:archive:list 控制，
    // 只授予超级管理员与部门经理。此前这里判断的是 oa:incomingDoc:list（普通用户也有），
    // 与后端注释所称的"只开放给管理岗"不一致。
    canManageArchive() { return checkPermi('oa:archive:list') },
    // 下面三个页签同理：没有列表权限就不渲染，否则 loadAll 会拉回一串 403 提示
    canManageDocType() { return checkPermi('oa:docType:list') },
    canManageTemplate() { return checkPermi('oa:docTemplate:list') },
    canManageDocument() { return checkPermi('oa:document:list') }
  },
  created() { this.loadAll() },
  methods: {
    loadAll() {
      this.loading = true
      const emptyRows = Promise.resolve({ rows: [] })
      const emptyData = Promise.resolve({ data: [] })
      Promise.all([
        myCirculation(),
        myDone(),
        this.canManageArchive ? archiveList({ pageNum: 1, pageSize: 100 }) : emptyRows,
        this.canManageDocType ? docTypeList({}) : emptyData,
        this.canManageTemplate ? docTemplateList({}) : emptyData,
        this.canManageDocument ? documentListApi({ pageNum: 1, pageSize: 100 }) : emptyRows
      ]).then(([circulation, done, archives, types, templates, documents]) => {
        this.circulationList = circulation.data || []
        this.doneList = done.data || []
        this.archiveRows = archives.rows || []
        this.typeList = types.data || []
        this.docTypeOptions = types.data || []
        this.templateList = templates.data || []
        this.documentList = documents.rows || []
      }).catch(() => {}).finally(() => { this.loading = false })
    },
    docTypeLabel(id) { const item = this.docTypeOptions.find(option => String(option.docTypeId) === String(id)); return item ? item.typeName : '未选择' },
    openTemplateDialog(row) { this.templateForm = row ? { ...row } : { templateName: '', docType: '2', docTypeId: null, redHeaderText: '', titleFont: 'SimSun', titleSize: 22, bodyFont: 'FangSong', bodySize: 16, templateContent: '', status: '0' }; this.templatePreview = { ...this.templateForm }; this.templateDialogVisible = true },
    previewTemplate(row) { this.templatePreview = { ...row }; this.templateForm = { ...row }; this.templateDialogVisible = true },
    submitTemplate() { this.$refs.templateForm.validate(valid => { if (!valid) return; const request = this.templateForm.templateId ? editDocTemplate(this.templateForm) : addDocTemplate(this.templateForm); request.then(() => { this.$message.success('保存成功'); this.templateDialogVisible = false; this.loadAll() }).catch(() => {}) }) },
    deleteTemplate(row) { this.$confirm('确认删除该模板吗？', '提示', { type: 'warning' }).then(() => removeDocTemplate(row.templateId)).then(() => { this.$message.success('删除成功'); this.loadAll() }).catch(() => {}) },
    uploadDocument(request) { const data = new FormData(); data.append('file', request.file); uploadDocumentApi(data).then(() => { this.$message.success('上传成功'); this.loadAll() }).catch(() => request.onError()) },
    downloadDocument(row) {
      downloadDocumentApi(row.documentId).then(blob => {
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = row.documentName || ('document-' + row.documentId)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      }).catch(() => {})
    },
    deleteDocument(row) { this.$confirm('确认删除该附件吗？', '提示', { type: 'warning' }).then(() => removeDocumentApi(row.documentId)).then(() => { this.$message.success('删除成功'); this.loadAll() }).catch(() => {}) },
    formatSize(size) { if (!size) return '0 B'; if (size < 1024) return size + ' B'; if (size < 1048576) return (size / 1024).toFixed(1) + ' KB'; return (size / 1048576).toFixed(1) + ' MB' },
    handleRead(row) { markRead(row.circulationId).then(() => { this.$message.success('已标记为已读'); this.loadAll() }).catch(() => {}) },
    openTypeDialog(row) {
      this.typeForm = row ? { ...row } : { typeName: '', typeCode: '', sortOrder: 0, status: '0' }
      this.typeDialogVisible = true
    },
    submitType() {
      this.$refs.typeForm.validate(valid => {
        if (!valid) return
        const request = this.typeForm.docTypeId ? editDocType(this.typeForm) : addDocType(this.typeForm)
        request.then(() => { this.$message.success('保存成功'); this.typeDialogVisible = false; this.loadAll() }).catch(() => {})
      })
    },
    deleteType(row) {
      this.$confirm('确认删除该公文类型吗？', '提示', { type: 'warning' }).then(() => removeDocType(row.docTypeId)).then(() => { this.$message.success('删除成功'); this.loadAll() }).catch(() => {})
    }
  }
}
</script>
<style scoped>
.red-header-preview { width: 100%; min-height: 180px; padding: 24px; background: #fff; border: 1px solid #dcdfe6; text-align: center; }
.red-header-preview h1 { color: #c00; border-bottom: 3px solid #c00; padding-bottom: 12px; }
</style>