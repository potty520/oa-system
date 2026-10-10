<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="80px" class="filter-container">
      <el-form-item label="日程标题" prop="scheduleTitle">
        <el-input v-model="queryParams.scheduleTitle" placeholder="请输入日程标题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="日程类型" prop="scheduleType">
        <el-select v-model="queryParams.scheduleType" placeholder="请选择类型" clearable>
          <el-option label="工作" value="1" />
          <el-option label="会议" value="2" />
          <el-option label="出差" value="3" />
          <el-option label="其他" value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="进行中" value="0" />
          <el-option label="已完成" value="1" />
          <el-option label="已取消" value="3" />
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
        <el-button v-hasPermi="['oa:schedule:add']" type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="日程标题" prop="scheduleTitle" min-width="150" show-overflow-tooltip />
      <el-table-column label="日程类型" prop="scheduleType" width="100" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.scheduleType === '1'">工作</el-tag>
          <el-tag v-else-if="scope.row.scheduleType === '2'" type="success">会议</el-tag>
          <el-tag v-else-if="scope.row.scheduleType === '3'" type="warning">出差</el-tag>
          <el-tag v-else type="info">其他</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" prop="startTime" width="160" align="center" />
      <el-table-column label="结束时间" prop="endTime" width="160" align="center" />
      <el-table-column label="地点" prop="location" min-width="120" show-overflow-tooltip />
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="primary">进行中</el-tag>
          <el-tag v-else-if="scope.row.status === '1'" type="success">已完成</el-tag>
          <el-tag v-else type="info">已取消</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template slot-scope="scope">
          <el-button v-hasPermi="['oa:schedule:edit']" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button v-hasPermi="['oa:schedule:remove']" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="日程标题" prop="scheduleTitle">
          <el-input v-model="form.scheduleTitle" placeholder="请输入日程标题" />
        </el-form-item>
        <el-form-item label="日程类型" prop="scheduleType">
          <el-select v-model="form.scheduleType" placeholder="请选择类型">
            <el-option label="工作" value="1" />
            <el-option label="会议" value="2" />
            <el-option label="出差" value="3" />
            <el-option label="其他" value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" value-format="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" value-format="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入地点" />
        </el-form-item>
        <el-form-item label="备注" prop="scheduleContent">
          <el-input v-model="form.scheduleContent" type="textarea" :rows="3" placeholder="请输入备注" />
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
import { list, getInfo, add, edit, remove } from '@/api/oa/schedule'

export default {
  name: 'Schedule',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        scheduleTitle: undefined,
        scheduleType: undefined,
        status: undefined
      },
      dialogVisible: false,
      dialogTitle: '',
      form: {
        scheduleId: undefined,
        scheduleTitle: '',
        scheduleType: '1',
        startTime: '',
        endTime: '',
        location: '',
        scheduleContent: ''
      },
      rules: {
        scheduleTitle: [{ required: true, message: '请输入日程标题', trigger: 'blur' }],
        scheduleType: [{ required: true, message: '请选择日程类型', trigger: 'change' }],
        startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
        endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
      }
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
      this.dialogTitle = '新增日程'
      this.dialogVisible = true
    },
    handleUpdate(row) {
      this.resetForm()
      getInfo(row.scheduleId).then(res => {
        this.form = res.data || {}
        this.dialogTitle = '编辑日程'
        this.dialogVisible = true
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$confirm('确定删除该日程吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return remove(row.scheduleId)
      }).then(() => {
        this.getList()
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (this.form.scheduleId) {
            edit(this.form).then(() => {
              this.$message.success('修改成功')
              this.dialogVisible = false
              this.getList()
            }).catch(() => {})
          } else {
            add(this.form).then(() => {
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
        scheduleId: undefined,
        scheduleTitle: '',
        scheduleType: '1',
        startTime: '',
        endTime: '',
        location: '',
        scheduleContent: ''
      }
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    }
  }
}
</script>
