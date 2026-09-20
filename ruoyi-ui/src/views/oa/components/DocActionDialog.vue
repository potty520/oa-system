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
      <el-form-item v-if="['transfer','urge','delegate','agent'].includes(form.action)" label="目标用户ID">
        <el-input-number v-model="form.targetUserId" :min="1" controls-position="right" style="width:100%" />
      </el-form-item>
      <el-form-item v-if="['transfer','urge','delegate','agent'].includes(form.action)" label="目标用户"><el-input v-model="form.targetUserName" placeholder="姓名" /></el-form-item>
      <el-form-item v-if="['countersign','add_sign','circulate'].includes(form.action)" label="目标用户ID"><el-input v-model="form.targetUserIdsText" placeholder="多个用户使用英文逗号分隔" /></el-form-item>
      <el-form-item v-if="form.action === 'cancel_sign'" label="签批记录ID"><el-input-number v-model="form.approvalId" :min="1" controls-position="right" style="width:100%" /></el-form-item>
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
export default {
  name: 'DocActionDialog',
  props: { docType: { type: String, required: true }, docId: { type: Number, default: null }, visible: { type: Boolean, default: false } },
  data() { return { form: { action: '', opinion: '', approvalId: undefined, targetUserId: undefined, targetUserName: '', targetUserIdsText: '', archiveCode: '', archiveLocation: '' }, timeline: {} } },
  computed: {
    dialogVisible: { get() { return this.visible }, set(value) { this.$emit('update:visible', value) } }
  },
  methods: {
    loadTimeline() {
      this.form = { action: '', opinion: '', approvalId: undefined, targetUserId: undefined, targetUserName: '', targetUserIdsText: '', archiveCode: '', archiveLocation: '' }
      if (this.docId) timeline(this.docType, this.docId).then(res => { this.timeline = res.data || {} })
    },
    submit() {
      if (!this.form.action) { this.$message.warning('请选择操作类型'); return }
      const targetUserIds = this.form.targetUserIdsText ? this.form.targetUserIdsText.split(',').map(value => Number(value.trim())).filter(Boolean) : (this.form.targetUserId ? [this.form.targetUserId] : [])
      const data = { ...this.form, targetUserIds }
      action(this.docType, this.docId, data).then(() => { this.$message.success('操作成功'); this.dialogVisible = false; this.$emit('success') })
    }
  }
}
</script>