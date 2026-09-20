<template>
  <div class="dashboard-container">
    <div class="welcome-section">
      <h2>欢迎使用OA办公系统</h2>
      <p>当前用户：{{ userInfo.nickName || userInfo.userName || 'Admin' }}</p>
    </div>
    <el-row :gutter="20" class="card-row">
      <el-col :span="8"><el-card shadow="hover"><div class="stat"><div class="number">{{ stats.todoCount }}</div><div class="label">待办事项</div></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover"><div class="stat"><div class="number">{{ stats.announcementCount }}</div><div class="label">未读公告</div></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover"><div class="stat"><div class="number">{{ stats.unreadCirculation }}</div><div class="label">未读传阅</div></div></el-card></el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="8"><el-card shadow="hover"><div class="stat"><div class="number">{{ stats.incomingPending }}</div><div class="label">待办收文</div></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover"><div class="stat"><div class="number">{{ stats.outgoingPending }}</div><div class="label">办理中发文</div></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover"><div class="stat"><div class="number">{{ archivedCount }}</div><div class="label">已归档公文</div></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { dashboardStats } from '@/api/oa/docProcess'
import { getUnreadCount } from '@/api/oa/announcement'

export default {
  name: 'Dashboard',
  data() {
    return {
      stats: { todoCount: 0, announcementCount: 0, incomingPending: 0, outgoingPending: 0, unreadCirculation: 0, archivedIncoming: 0, archivedOutgoing: 0 }
    }
  },
  computed: {
    ...mapGetters(['userInfo']),
    archivedCount() { return Number(this.stats.archivedIncoming || 0) + Number(this.stats.archivedOutgoing || 0) }
  },
  created() { this.loadStats() },
  methods: {
    loadStats() {
      dashboardStats().then(res => { this.stats = { ...this.stats, ...(res.data || {}) } }).catch(() => {})
      getUnreadCount().then(res => { this.stats.announcementCount = res.data || 0 }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container { padding: 20px; }
.welcome-section { background: #fff; padding: 20px; border-radius: 4px; margin-bottom: 20px; box-shadow: 0 2px 12px rgba(0,0,0,.1); h2 { margin: 0 0 10px; } p { margin: 0; color: #606266; } }
.card-row { margin-bottom: 20px; }
.stat { text-align: center; padding: 12px 0; .number { font-size: 30px; font-weight: bold; color: #303133; } .label { margin-top: 8px; color: #909399; } }
</style>