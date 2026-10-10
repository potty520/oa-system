<template>
  <div class="app-container">
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
      <el-table-column label="发布时间" prop="publishStartTime" width="160" align="center" />
      <el-table-column label="操作" width="100" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 查看对话框 -->
    <el-dialog title="公告详情" :visible.sync="viewVisible" width="650px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="公告标题" :span="2">{{ viewData.annTitle }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag v-if="viewData.annType === '1'" type="warning">通知</el-tag>
          <el-tag v-else type="primary">公告</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="级别">
          <el-tag v-if="viewData.annLevel === '1'" type="info">普通</el-tag>
          <el-tag v-else-if="viewData.annLevel === '2'" type="warning">重要</el-tag>
          <el-tag v-else-if="viewData.annLevel === '3'" type="danger">紧急</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="浏览次数">{{ viewData.viewCount }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ viewData.publishStartTime }}</el-descriptions-item>
        <el-descriptions-item label="公告内容" :span="2">{{ viewData.annContent }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer">
        <el-button @click="viewVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { publishedList, getInfo } from '@/api/oa/announcement'

export default {
  name: 'Notice',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      viewVisible: false,
      viewData: {}
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      publishedList({ pageNum: 1, pageSize: 20 }).then(res => {
        this.tableData = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleView(row) {
      getInfo(row.annId).then(res => {
        this.viewData = res.data || {}
        this.viewVisible = true
      }).catch(() => {})
    }
  }
}
</script>
