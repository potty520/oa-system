<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px" class="filter-container">
      <el-form-item label="公告标题" prop="annTitle">
        <el-input v-model="queryParams.annTitle" placeholder="请输入公告标题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="annType">
        <el-select v-model="queryParams.annType" placeholder="请选择类型" clearable>
          <el-option label="通知" value="1" />
          <el-option label="公告" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button v-hasPermi="['oa:announcement:add']" type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="公告标题" prop="annTitle" min-width="150" show-overflow-tooltip />
      <el-table-column label="类型" prop="annType" width="100" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.annType === '1'" type="warning">通知</el-tag>
          <el-tag v-else-if="scope.row.annType === '2'" type="primary">公告</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="级别" prop="annLevel" width="100" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.annLevel === '1'" type="info">普通</el-tag>
          <el-tag v-else-if="scope.row.annLevel === '2'" type="warning">重要</el-tag>
          <el-tag v-else-if="scope.row.annLevel === '3'" type="danger">紧急</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="浏览次数" prop="viewCount" width="100" align="center" />
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            :disabled="!canEditStatus"
            @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
      <el-table-column label="操作" width="180" align="center">
        <template slot-scope="scope">
          <el-button v-hasPermi="['oa:announcement:edit']" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button v-hasPermi="['oa:announcement:remove']" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="公告标题" prop="annTitle">
          <el-input v-model="form.annTitle" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="类型" prop="annType">
          <el-radio-group v-model="form.annType">
            <el-radio :label="1">通知</el-radio>
            <el-radio :label="2">公告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="级别" prop="annLevel">
          <el-radio-group v-model="form.annLevel">
            <el-radio :label="1">普通</el-radio>
            <el-radio :label="2">重要</el-radio>
            <el-radio :label="3">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公告内容" prop="annContent">
          <el-input v-model="form.annContent" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="是否置顶">
          <el-switch v-model="form.isTop" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item label="是否公开">
          <el-switch v-model="form.isPublic" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="form.publishTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="yyyy-MM-dd HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { list, getInfo, add, edit, remove } from '@/api/oa/announcement'
import { checkPermi } from '@/utils/permission'

export default {
  name: 'Announcement',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        annTitle: undefined,
        annType: undefined,
        status: undefined
      },
      dialogVisible: false,
      dialogTitle: '',
      form: {
        annId: undefined,
        annTitle: '',
        annType: '1',
        annLevel: '1',
        annContent: '',
        isTop: '0',
        isPublic: '1',
        publishTimeRange: []
      },
      rules: {
        annTitle: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
        annType: [{ required: true, message: '请选择类型', trigger: 'change' }],
        annLevel: [{ required: true, message: '请选择级别', trigger: 'change' }],
        annContent: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
      }
    }
  },
  computed: {
    // 状态开关调用的编辑接口需要 oa:announcement:edit，无权限时只读展示（与系统用户页同一写法）
    canEditStatus() {
      return checkPermi('oa:announcement:edit')
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      list(this.queryParams).then(res => {
        this.tableData = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.handleQuery()
    },
    handleAdd() {
      this.resetForm()
      this.dialogTitle = '新增公告'
      this.dialogVisible = true
    },
    handleUpdate(row) {
      this.resetForm()
      getInfo(row.annId).then(res => {
        const data = res.data || {}
        data.publishTimeRange = data.publishStartTime && data.publishEndTime
          ? [data.publishStartTime, data.publishEndTime]
          : []
        this.form = data
        this.dialogTitle = '编辑公告'
        this.dialogVisible = true
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$confirm('确定删除该公告吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return remove(row.annId)
      }).then(() => {
        this.getList()
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '停用'
      this.$confirm('确认要"' + text + '"该公告吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return edit({ annId: row.annId, status: row.status })
      }).then(() => {
        this.$message.success(text + '成功')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          const submitData = { ...this.form }
          delete submitData.publishTimeRange
          if (this.form.publishTimeRange && this.form.publishTimeRange.length === 2) {
            submitData.publishStartTime = this.form.publishTimeRange[0]
            submitData.publishEndTime = this.form.publishTimeRange[1]
          }
          if (this.form.annId) {
            edit(submitData).then(() => {
              this.$message.success('修改成功')
              this.dialogVisible = false
              this.getList()
            }).catch(() => {})
          } else {
            add(submitData).then(() => {
              this.$message.success('新增成功')
              this.dialogVisible = false
              this.getList()
            }).catch(() => {})
          }
        }
      })
    },
    resetForm() {
      this.form = {
        annId: undefined,
        annTitle: '',
        annType: '1',
        annLevel: '1',
        annContent: '',
        isTop: '0',
        isPublic: '1',
        publishTimeRange: []
      }
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    }
  }
}
</script>
