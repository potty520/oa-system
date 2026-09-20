<template>
  <div class="dashboard-container">
    <div class="welcome-section">
      <h2>欢迎使用OA办公系统</h2>
      <p>当前用户：{{ userInfo.nickName || userInfo.userName || 'Admin' }}</p>
    </div>
    <el-row :gutter="20" class="card-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #40c9c6">
              <i class="el-icon-s-claim" />
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ todoCount }}</div>
              <div class="stat-label">待办事项</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #36a3f7">
              <i class="el-icon-s-bell" />
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ announcementCount }}</div>
              <div class="stat-label">未读公告</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f4516c">
              <i class="el-icon-s-order" />
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ pendingApproval }}</div>
              <div class="stat-label">待审批</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { count as getTodoCount } from '@/api/oa/todo'
import { getUnreadCount } from '@/api/oa/announcement'

export default {
  name: 'Dashboard',
  data() {
    return {
      todoCount: 0,
      announcementCount: 0,
      pendingApproval: 0
    }
  },
  computed: {
    ...mapGetters(['userInfo'])
  },
  created() {
    this.loadStats()
  },
  methods: {
    loadStats() {
      getTodoCount().then(res => {
        this.todoCount = res.data || 0
      }).catch(() => {})
      getUnreadCount().then(res => {
        this.announcementCount = res.data || 0
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 20px;

  .welcome-section {
    background: #fff;
    padding: 20px;
    border-radius: 4px;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

    h2 {
      margin: 0 0 10px 0;
      color: #303133;
    }

    p {
      margin: 0;
      color: #606266;
    }
  }

  .card-row {
    margin-bottom: 20px;
  }

  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
    }

    .stat-icon {
      width: 60px;
      height: 60px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      color: #fff;
      margin-right: 16px;
    }

    .stat-info {
      .stat-number {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }
}
</style>
