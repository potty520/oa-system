<template>
  <el-dialog title="公文流程操作" :visible.sync="dialogVisible" width="760px" append-to-body @open="loadTimeline">
    <el-form :model="form" label-width="90px">
      <el-form-item label="操作类型" required>
        <el-select v-model="form.action" placeholder="请选择操作" style="width:100%">
          <el-option label="签收" value="receive" />
          <el-option label="提交审核" value="submit" />
          <el-option label="审批通过" value="approve" />
          <el-option label="会签" value="countersign" />
          <el-option label="加签" value="add_sign" />
          <el-option label="撤签" value="cancel_sign" />
          <el-option label="退回" value="return" />
          <el-option label="撤回" value="withdraw" />
          <el-option label="转办" value="transfer" />
          <el-option label="委托" value="delegate" />
          <el-option label="代理" value="agent" />
          <el-option label="传阅" value="circulate" />
          <el-option label="催办" value="urge" />
          <el-option label="办结" value="complete" />
          <el-option label="签发" value="sign" />
          <el-option label="归档" value="archive" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="singleTargetActions.includes(form.action)" label="目标用户">
        <el-select v-model="form.targetUserId" filterable clearable placeholder="请选择目标用户" style="width:100%">
          <el-option v-for="item in userOptions" :key="item.userId" :label="userLabel(item)" :value="item.userId" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="multiTargetActions.includes(form.action)" label="目标用户">
        <el-select v-model="form.targetUserIds" multiple filterable clearable placeholder="可多选" style="width:100%">
          <el-option v-for="item in userOptions" :key="item.userId" :label="userLabel(item)" :value="item.userId" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.action === 'cancel_sign'" label="签批记录">
        <el-select v-model="form.approvalId" clearable placeholder="请选择要撤回的签批" style="width:100%">
          <el-option v-for="item in cancellableApprovals" :key="item.approvalId" :label="approvalLabel(item)" :value="item.approvalId" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.action === 'archive'" label="归档编号"><el-input v-model="form.archiveCode" placeholder="留空自动生成" /></el-form-item>
      <el-form-item v-if="form.action === 'archive'" label="归档位置"><el-input v-model="form.archiveLocation" /></el-form-item>
      <el-form-item label="办理意见"><el-input v-model="form.opinion" type="textarea" :rows="3" /></el-form-item>
    </el-form>

    <el-divider content-position="left">流程记录</el-divider>
    <el-table :data="timeline.approvals || []" size="mini" border>
      <el-table-column label="办理人" prop="userName" width="110" />
      <el-table-column label="结果" prop="approveResult" width="80" align="center" />
      <el-table-column label="意见" prop="approveRemark" min-width="180" />
      <el-table-column label="时间" prop="approveTime" width="160" />
    </el-table>

    <div slot="footer"><el-button @click="dialogVisible = false">关闭</el-button><el-button type="primary" @click="submit">执行操作</el-button></div>
  </el-dialog>
</template>

<script>
import { timeline, action } from '@/api/oa/docProcess'
import { listUserOptions } from '@/api/system/user'

// 目标用户一律从后端下拉接口选，不再让用户手输 ID/姓名：
// 手输的姓名会被写进办理轨迹，等于可以随意冒名。
const emptyForm = () => ({
  action: '',
  opinion: '',
  approvalId: undefined,
  targetUserId: undefined,
  targetUserIds: [],
  archiveCode: '',
  archiveLocation: ''
})

export default {
  name: 'DocActionDialog',
  props: { docType: { type: String, required: true }, docId: { type: Number, default: null }, visible: { type: Boolean, default: false } },
  data() {
    return {
      form: emptyForm(),
      timeline: {},
      userOptions: [],
      singleTargetActions: ['transfer', 'urge', 'delegate', 'agent'],
      multiTargetActions: ['countersign', 'add_sign', 'circulate']
    }
  },
  computed: {
    dialogVisible: { get() { return this.visible }, set(value) { this.$emit('update:visible', value) } },
    currentUserId() { return (this.$store.getters.userInfo || {}).userId },
    isAdmin() { return this.currentUserId === 1 },
    cancellableApprovals() {
      // 后端只允许撤回分配给自己的待办签批，这里同步过滤掉选了也会报错的项
      return (this.timeline.approvals || []).filter(item => item.approveResult === '0' && (this.isAdmin || item.userId === this.currentUserId))
    }
  },
  methods: {
    userLabel(item) { return item.nickName ? item.nickName + '（' + item.userName + '）' : item.userName },
    approvalLabel(item) { return (item.userName || '未知') + ' / ' + (item.approveRemark || '待办') },
    loadTimeline() {
      this.form = emptyForm()
      if (this.userOptions.length === 0) {
        listUserOptions().then(res => { this.userOptions = res.data || [] }).catch(() => {})
      }
      if (this.docId) timeline(this.docType, this.docId).then(res => { this.timeline = res.data || {} })
    },
    submit() {
      if (!this.form.action) { this.$message.warning('请选择操作类型'); return }
      action(this.docType, this.docId, { ...this.form }).then(() => {
        this.$message.success('操作成功')
        this.dialogVisible = false
        this.$emit('success')
      })
    }
  }
}
</script>
