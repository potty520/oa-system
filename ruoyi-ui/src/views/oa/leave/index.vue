<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="80px" class="filter-container">
      <el-form-item label="请假类型" prop="leaveType">
        <el-select v-model="queryParams.leaveType" placeholder="请选择类型" clearable>
          <el-option label="事假" value="1" />
          <el-option label="病假" value="2" />
          <el-option label="年假" value="3" />
          <el-option label="婚假" value="4" />
          <el-option label="产假" value="5" />
          <el-option label="其他" value="6" />
        </el-select>
      </el-form-item>
      <el-form-item label="审批状态" prop="approvalStatus">
        <el-select v-model="queryParams.approvalStatus" placeholder="请选择状态" clearable>
          <el-option label="草稿" value="0" />
          <el-option label="待审批" value="1" />
          <el-option label="已通过" value="2" />
          <el-option label="已拒绝" value="3" />
          <el-option label="已撤回" value="4" />
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
        <el-button v-hasPermi="['oa:leave:add']" type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="请假类型" prop="leaveType" width="100" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.leaveType === '1'">事假</el-tag>
          <el-tag v-else-if="scope.row.leaveType === '2'" type="warning">病假</el-tag>
          <el-tag v-else-if="scope.row.leaveType === '3'" type="success">年假</el-tag>
          <el-tag v-else-if="scope.row.leaveType === '4'" type="primary">婚假</el-tag>
          <el-tag v-else-if="scope.row.leaveType === '5'" type="danger">产假</el-tag>
          <el-tag v-else type="info">其他</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" prop="startTime" width="160" align="center" />
      <el-table-column label="结束时间" prop="endTime" width="160" align="center" />
      <el-table-column label="请假天数" prop="leaveDays" width="100" align="center" />
      <el-table-column label="审批状态" prop="approvalStatus" width="100" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.approvalStatus === '0'" type="info">草稿</el-tag>
          <el-tag v-else-if="scope.row.approvalStatus === '1'" type="warning">待审批</el-tag>
          <el-tag v-else-if="scope.row.approvalStatus === '2'" type="success">已通过</el-tag>
          <el-tag v-else-if="scope.row.approvalStatus === '3'" type="danger">已拒绝</el-tag>
          <el-tag v-else-if="scope.row.approvalStatus === '4'" type="info">已撤回</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" prop="applyTime" width="160" align="center" />
      <el-table-column label="操作" min-width="200" align="center">
        <template slot-scope="scope">
          <el-button v-if="scope.row.approvalStatus === '0'" v-hasPermi="['oa:leave:edit']" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button v-if="scope.row.approvalStatus === '0'" v-hasPermi="['oa:leave:remove']" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
          <el-button v-if="scope.row.approvalStatus === '0'" v-hasPermi="['oa:leave:submit']" size="mini" type="text" icon="el-icon-s-promotion" @click="handleSubmit(scope.row)">提交</el-button>
          <el-button v-if="scope.row.approvalStatus === '1'" v-hasPermi="['oa:leave:withdraw']" size="mini" type="text" icon="el-icon-refresh-left" @click="handleWithdraw(scope.row)">撤回</el-button>
          <el-button v-if="scope.row.approvalStatus === '1'" v-hasPermi="['oa:leave:approve']" size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)">审批</el-button>
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
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="form.leaveType" placeholder="请选择请假类型">
            <el-option label="事假" value="1" />
            <el-option label="病假" value="2" />
            <el-option label="年假" value="3" />
            <el-option label="婚假" value="4" />
            <el-option label="产假" value="5" />
            <el-option label="其他" value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" value-format="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" value-format="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="请假天数" prop="leaveDays">
          <el-input-number v-model="form.leaveDays" :min="0.5" :step="0.5" />
        </el-form-item>
        <el-form-item label="请假原因" prop="leaveReason">
          <el-input v-model="form.leaveReason" type="textarea" :rows="4" placeholder="请输入请假原因" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 审批对话框 -->
    <el-dialog title="审批" :visible.sync="approveVisible" width="500px" append-to-body>
      <el-form ref="approveForm" :model="approveForm" :rules="approveRules" label-width="80px">
        <el-form-item label="审批结果" prop="approvalStatus">
          <el-radio-group v-model="approveForm.approvalStatus">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" prop="approveRemark">
          <el-input v-model="approveForm.remark" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="approveVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitApprove">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { list, getInfo, add, edit, remove, submit, withdraw, approve } from '@/api/oa/leave'

export default {
  name: 'Leave',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        leaveType: undefined,
        approvalStatus: undefined
      },
      dialogVisible: false,
      dialogTitle: '',
      form: {
        leaveId: undefined,
        leaveType: undefined,
        startTime: '',
        endTime: '',
        leaveDays: 1,
        leaveReason: ''
      },
      rules: {
        leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
        startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
        endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
        leaveDays: [{ required: true, message: '请输入请假天数', trigger: 'blur' }],
        leaveReason: [{ required: true, message: '请输入请假原因', trigger: 'blur' }]
      },
      approveVisible: false,
      approveForm: {
        leaveId: undefined,
        approvalStatus: '2',
        remark: ''
      },
      approveRules: {
        approvalStatus: [{ required: true, message: '请选择审批结果', trigger: 'change' }]
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
      this.dialogTitle = '新增请假'
      this.dialogVisible = true
    },
    handleUpdate(row) {
      this.resetForm()
      getInfo(row.leaveId).then(res => {
        this.form = res.data || {}
        this.dialogTitle = '编辑请假'
        this.dialogVisible = true
      })
    },
    handleDelete(row) {
      this.$confirm('确定删除该请假记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return remove(row.leaveId)
      }).then(() => {
        this.getList()
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    handleSubmit(row) {
      this.$confirm('确定提交该请假申请吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return submit(row.leaveId)
      }).then(() => {
        this.getList()
        this.$message.success('提交成功')
      }).catch(() => {})
    },
    handleWithdraw(row) {
      this.$confirm('确定撤回该请假申请吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return withdraw(row.leaveId)
      }).then(() => {
        this.getList()
        this.$message.success('撤回成功')
      }).catch(() => {})
    },
    handleApprove(row) {
      this.approveForm = {
        leaveId: row.leaveId,
        approvalStatus: '2',
        remark: ''
      }
      this.approveVisible = true
    },
    submitApprove() {
      this.$refs.approveForm.validate(valid => {
        if (valid) {
          approve(this.approveForm).then(() => {
            this.$message.success('审批成功')
            this.approveVisible = false
            this.getList()
          })
        }
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (this.form.leaveId) {
            edit(this.form).then(() => {
              this.$message.success('修改成功')
              this.dialogVisible = false
              this.getList()
            })
          } else {
            add(this.form).then(() => {
              this.$message.success('新增成功')
              this.dialogVisible = false
              this.getList()
            })
          }
        }
      })
    },
    resetForm() {
      this.form = {
        leaveId: undefined,
        leaveType: undefined,
        startTime: '',
        endTime: '',
        leaveDays: 1,
        leaveReason: ''
      }
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    }
  }
}
</script>
