<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="80px" class="filter-container">
      <el-form-item label="报销类型" prop="reimbType">
        <el-select v-model="queryParams.reimbType" placeholder="请选择类型" clearable>
          <el-option label="差旅" value="1" />
          <el-option label="办公" value="2" />
          <el-option label="招待" value="3" />
          <el-option label="其他" value="4" />
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button v-hasPermi="['oa:reimbursement:add']" type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column label="报销类型" prop="reimbType" width="100" align="center">
        <template slot-scope="scope">{{ typeLabel(scope.row.reimbType) }}</template>
      </el-table-column>
      <el-table-column label="报销事由" prop="reimbReason" min-width="180" show-overflow-tooltip />
      <el-table-column label="报销金额" prop="totalAmount" width="120" align="right">
        <template slot-scope="scope"><span style="color:#f56c6c;font-weight:bold;">¥{{ scope.row.totalAmount }}</span></template>
      </el-table-column>
      <el-table-column label="审批状态" prop="approvalStatus" width="100" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.approvalStatus === '0'" type="info">草稿</el-tag>
          <el-tag v-else-if="scope.row.approvalStatus === '1'" type="warning">待审批</el-tag>
          <el-tag v-else-if="scope.row.approvalStatus === '2'" type="success">已通过</el-tag>
          <el-tag v-else-if="scope.row.approvalStatus === '3'" type="danger">已拒绝</el-tag>
          <el-tag v-else type="info">已撤回</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付款状态" prop="paymentStatus" width="100" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.paymentStatus === '1'" type="success">已付款</el-tag>
          <el-tag v-else type="info">未付款</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付款时间" prop="paymentTime" width="160" align="center" />
      <el-table-column label="申请时间" prop="applyTime" width="160" align="center" />
      <el-table-column label="操作" min-width="340" align="center">
        <template slot-scope="scope">
          <el-button v-if="scope.row.approvalStatus === '0'" v-hasPermi="['oa:reimbursement:edit']" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button v-if="scope.row.approvalStatus === '0'" v-hasPermi="['oa:reimbursement:remove']" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
          <el-button v-if="scope.row.approvalStatus === '0'" v-hasPermi="['oa:reimbursement:submit']" size="mini" type="text" icon="el-icon-s-promotion" @click="handleSubmit(scope.row)">提交</el-button>
          <el-button v-if="scope.row.approvalStatus === '1'" v-hasPermi="['oa:reimbursement:withdraw']" size="mini" type="text" icon="el-icon-refresh-left" @click="handleWithdraw(scope.row)">撤回</el-button>
          <el-button v-if="scope.row.approvalStatus === '1'" v-hasPermi="['oa:reimbursement:approve']" size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)">审批</el-button>
          <el-button v-if="scope.row.approvalStatus === '2' && scope.row.paymentStatus !== '1'" v-hasPermi="['oa:reimbursement:pay']" size="mini" type="text" icon="el-icon-money" @click="handlePay(scope.row)">付款</el-button>
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="900px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="报销类型" prop="reimbType">
              <el-select v-model="form.reimbType" placeholder="请选择类型" style="width:100%">
                <el-option label="差旅" value="1" />
                <el-option label="办公" value="2" />
                <el-option label="招待" value="3" />
                <el-option label="其他" value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报销总额">
              <el-input :value="computedTotal" readonly />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="报销事由" prop="reimbReason">
          <el-input v-model="form.reimbReason" type="textarea" :rows="2" placeholder="请输入报销事由" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>

        <el-divider content-position="left">报销明细</el-divider>
        <div v-for="(item, index) in form.details" :key="index" class="detail-row">
          <el-row :gutter="10">
            <el-col :span="6">
              <el-form-item label="费用类型" :prop="'details.' + index + '.expenseType'" :rules="rules.expenseType">
                <el-input v-model="item.expenseType" placeholder="例如：交通费" />
              </el-form-item>
            </el-col>
            <el-col :span="5">
              <el-form-item label="费用日期">
                <el-date-picker v-model="item.expenseDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="4">
              <el-form-item label="金额" :prop="'details.' + index + '.expenseAmount'" :rules="rules.expenseAmount">
                <el-input-number v-model="item.expenseAmount" :min="0" :precision="2" controls-position="right" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="7">
              <el-form-item label="说明">
                <el-input v-model="item.expenseDesc" placeholder="费用说明" />
              </el-form-item>
            </el-col>
            <el-col :span="2">
              <el-button type="danger" icon="el-icon-delete" circle @click="removeDetail(index)" />
            </el-col>
          </el-row>
        </div>
        <el-button type="primary" plain icon="el-icon-plus" @click="addDetail">添加明细</el-button>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="报销详情" :visible.sync="viewVisible" width="900px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报销类型">{{ typeLabel(viewData.reimbType) }}</el-descriptions-item>
        <el-descriptions-item label="报销金额">¥{{ viewData.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="报销事由" :span="2">{{ viewData.reimbReason }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ viewData.applyTime }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ viewData.approveBy }}</el-descriptions-item>
        <el-descriptions-item label="付款状态">{{ viewData.paymentStatus === '1' ? '已付款' : '未付款' }}</el-descriptions-item>
        <el-descriptions-item label="付款时间">{{ viewData.paymentTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewData.remark }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="viewData.details || []" border style="margin-top:15px">
        <el-table-column label="费用类型" prop="expenseType" />
        <el-table-column label="费用日期" prop="expenseDate" width="120" />
        <el-table-column label="金额" prop="expenseAmount" width="120" />
        <el-table-column label="说明" prop="expenseDesc" />
      </el-table>
    </el-dialog>

    <el-dialog title="报销审批" :visible.sync="approveVisible" width="500px" append-to-body>
      <el-form ref="approveForm" :model="approveForm" :rules="approveRules" label-width="90px">
        <el-form-item label="审批结果" prop="approvalStatus">
          <el-radio-group v-model="approveForm.approvalStatus">
            <el-radio label="2">通过</el-radio>
            <el-radio label="3">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" prop="approveRemark">
          <el-input v-model="approveForm.approveRemark" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApprove">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { list, getInfo, saveWithDetails, edit, remove, submit, withdraw, approve, pay, getDetails } from '@/api/oa/reimbursement'

const emptyDetail = () => ({ expenseType: '', expenseDate: '', expenseAmount: 0, expenseDesc: '' })

export default {
  name: 'Reimbursement',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, reimbType: undefined, approvalStatus: undefined },
      dialogVisible: false,
      dialogTitle: '',
      form: { reimbId: undefined, reimbType: '1', reimbReason: '', remark: '', details: [emptyDetail()] },
      rules: {
        reimbType: [{ required: true, message: '请选择报销类型', trigger: 'change' }],
        reimbReason: [{ required: true, message: '请输入报销事由', trigger: 'blur' }],
        expenseType: [{ required: true, message: '请输入费用类型', trigger: 'blur' }],
        expenseAmount: [{ required: true, message: '请输入费用金额', trigger: 'blur' }]
      },
      viewVisible: false,
      viewData: {},
      approveVisible: false,
      approveForm: { reimbId: undefined, approvalStatus: '2', approveRemark: '' },
      approveRules: { approvalStatus: [{ required: true, message: '请选择审批结果', trigger: 'change' }] }
    }
  },
  computed: {
    computedTotal() {
      return (this.form.details || []).reduce((sum, item) => sum + Number(item.expenseAmount || 0), 0).toFixed(2)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    typeLabel(type) {
      return { '1': '差旅', '2': '办公', '3': '招待', '4': '其他' }[type] || '其他'
    },
    getList() {
      this.loading = true
      list(this.queryParams).then(res => {
        this.tableData = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
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
      this.dialogTitle = '新增报销'
      this.dialogVisible = true
    },
    handleUpdate(row) {
      this.resetForm()
      Promise.all([getInfo(row.reimbId), getDetails(row.reimbId)]).then(([infoRes, detailRes]) => {
        const data = infoRes.data || {}
        data.details = (detailRes.data && detailRes.data.length) ? detailRes.data : [emptyDetail()]
        this.form = data
        this.dialogTitle = '编辑报销'
        this.dialogVisible = true
      })
    },
    handleView(row) {
      Promise.all([getInfo(row.reimbId), getDetails(row.reimbId)]).then(([infoRes, detailRes]) => {
        this.viewData = infoRes.data || {}
        this.viewData.details = detailRes.data || []
        this.viewVisible = true
      })
    },
    handleDelete(row) {
      this.$confirm('确定删除该报销记录吗？', '提示', { type: 'warning' }).then(() => remove(row.reimbId)).then(() => {
        this.getList()
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    handleSubmit(row) {
      this.$confirm('确定提交该报销申请吗？', '提示', { type: 'warning' }).then(() => submit(row.reimbId)).then(() => {
        this.getList()
        this.$message.success('提交成功')
      }).catch(() => {})
    },
    handleWithdraw(row) {
      this.$confirm('确定撤回该报销申请吗？', '提示', { type: 'warning' }).then(() => withdraw(row.reimbId)).then(() => {
        this.getList()
        this.$message.success('撤回成功')
      }).catch(() => {})
    },
    handleApprove(row) {
      this.approveForm = { reimbId: row.reimbId, approvalStatus: '2', approveRemark: '' }
      this.approveVisible = true
    },
    handlePay(row) {
      this.$confirm('确定将该报销单标记为已付款吗？', '提示', { type: 'warning' }).then(() => pay(row.reimbId)).then(() => {
        this.getList()
        this.$message.success('付款成功')
      }).catch(() => {})
    },
    submitApprove() {
      this.$refs.approveForm.validate(valid => {
        if (!valid) return
        approve(this.approveForm).then(() => {
          this.$message.success('审批成功')
          this.approveVisible = false
          this.getList()
        })
      })
    },
    addDetail() {
      this.form.details.push(emptyDetail())
    },
    removeDetail(index) {
      if (this.form.details.length > 1) {
        this.form.details.splice(index, 1)
      } else {
        this.$message.warning('至少保留一项明细')
      }
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const submitData = {
          ...this.form,
          totalAmount: Number(this.computedTotal),
          details: this.form.details.map(item => ({
            ...item,
            expenseAmount: Number(item.expenseAmount || 0)
          }))
        }
        const request = submitData.reimbId ? edit(submitData) : saveWithDetails(submitData)
        request.then(() => {
          this.$message.success(submitData.reimbId ? '修改成功' : '新增成功')
          this.dialogVisible = false
          this.getList()
        })
      })
    },
    resetForm() {
      this.form = { reimbId: undefined, reimbType: '1', reimbReason: '', remark: '', details: [emptyDetail()] }
      if (this.$refs.form) this.$refs.form.resetFields()
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-row {
  padding: 8px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  margin-bottom: 10px;
  background: #fafafa;
}
</style>