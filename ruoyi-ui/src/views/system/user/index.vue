<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px" class="filter-container">
      <el-form-item label="用户名称" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号码" prop="phonenumber">
        <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="用户状态" clearable>
          <el-option label="正常" value="0" />
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
        <el-button v-hasPermi="['system:user:add']" type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button v-hasPermi="['system:user:remove']" type="danger" plain icon="el-icon-delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户编号" prop="userId" width="100" align="center" />
      <el-table-column label="用户名称" prop="userName" min-width="120" show-overflow-tooltip />
      <el-table-column label="用户昵称" prop="nickName" min-width="120" show-overflow-tooltip />
      <el-table-column label="部门" prop="dept.deptName" min-width="120" show-overflow-tooltip />
      <el-table-column label="手机号码" prop="phonenumber" width="130" align="center" />
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
      <el-table-column label="操作" min-width="200" align="center">
        <template slot-scope="scope">
          <el-button v-hasPermi="['system:user:edit']" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button v-hasPermi="['system:user:remove']" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
          <el-button v-hasPermi="['system:user:resetPwd']" size="mini" type="text" icon="el-icon-key" @click="handleCommand('resetPwd', scope.row)">重置密码</el-button>
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
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名称" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户名称" :disabled="!!form.userId" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="!form.userId">
          <el-col :span="12">
            <el-form-item label="用户密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入用户密码" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <treeselect v-model="form.deptId" :options="deptOptions" :normalizer="normalizer" placeholder="请选择部门" />
            </el-form-item>
          </el-col>

        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="岗位" prop="postIds">
              <el-select v-model="form.postIds" multiple placeholder="请选择岗位" style="width:100%">
                <el-option v-for="item in postOptions" :key="item.postId" :label="item.postName" :value="item.postId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色">
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { checkPermi } from '@/utils/permission'
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'

export default {
  components: { Treeselect },
  computed: {
    // 没有修改权限时只展示状态，不允许拨动开关
    canEditStatus() { return checkPermi('system:user:edit') }
  },
  name: 'User',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      ids: [],
      multiple: true,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        phonenumber: undefined,
        status: undefined
      },
      dialogVisible: false,
      dialogTitle: '',
      form: {
        userId: undefined,
        nickName: '',
        userName: '',
        password: '',
        phonenumber: '',
        email: '',
        status: '0',
        deptId: undefined,
        postIds: [],
        roleIds: []
      },
      rules: {
        userName: [{ required: true, message: '请输入用户名称', trigger: 'blur' }],
        nickName: [{ required: true, message: '请输入用户昵称', trigger: 'blur' }],
        password: [{ required: true, message: '请输入用户密码', trigger: 'blur' }]
      },
      deptOptions: [],
      postOptions: [],
      roleOptions: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      request({ url: '/system/user/list', method: 'get', params: this.queryParams }).then(res => {
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
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.userId)
      this.multiple = !selection.length
    },
    handleAdd() {
      this.resetForm()
      this.loadDictData()
      this.dialogTitle = '新增用户'
      this.dialogVisible = true
    },
    handleUpdate(row) {
      this.resetForm()
      this.loadDictData()
      const userId = row.userId || this.ids[0]
      request({ url: '/system/user/' + userId, method: 'get' }).then(res => {
        this.form = res.data || {}
        this.dialogTitle = '修改用户'
        this.dialogVisible = true
      })
    },
    handleDelete(row) {
      const userIds = row.userId ? [row.userId] : this.ids
      this.$confirm('确定删除选中的用户吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return request({ url: '/system/user/' + userIds.join(','), method: 'delete' })
      }).then(() => {
        this.getList()
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '停用'
      this.$confirm('确认要' + text + '用户"' + row.userName + '"吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return request({ url: '/system/user/changeStatus', method: 'put', data: { userId: row.userId, status: row.status } })
      }).then(() => {
        this.$message.success(text + '成功')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    handleCommand(command, row) {
      if (command === 'resetPwd') {
        this.$prompt('请输入"' + row.userName + '"的新密码', '重置密码', {
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        }).then(({ value }) => {
          return request({ url: '/system/user/resetPwd', method: 'put', data: { userId: row.userId, password: value } })
        }).then(() => {
          this.$message.success('密码重置成功')
        }).catch(() => {})
      }
    },
    loadDictData() {
      request({ url: '/system/dept/list', method: 'get' }).then(res => {
        this.deptOptions = this.buildDeptTree(res.data || [])
      }).catch(() => {})

      request({ url: '/system/post/list', method: 'get', params: { pageSize: 100 } }).then(res => {
        this.postOptions = res.rows || []
      }).catch(() => {})

      request({ url: '/system/role/list', method: 'get', params: { pageSize: 100 } }).then(res => {
        this.roleOptions = res.rows || []
      }).catch(() => {})
    },
    buildDeptTree(data) {
      const map = {}
      const roots = []
      data.forEach(item => { map[item.deptId] = { ...item, children: [] } })
      data.forEach(item => {
        const node = map[item.deptId]
        if (item.parentId === 0 || !map[item.parentId]) {
          roots.push(node)
        } else {
          map[item.parentId].children.push(node)
        }
      })
      return roots
    },
    normalizer(node) {
      return {
        id: node.deptId,
        label: node.deptName,
        children: node.children
      }
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (this.form.userId) {
            request({ url: '/system/user', method: 'put', data: this.form }).then(() => {
              this.$message.success('修改成功')
              this.dialogVisible = false
              this.getList()
            })
          } else {
            request({ url: '/system/user', method: 'post', data: this.form }).then(() => {
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
        userId: undefined,
        nickName: '',
        userName: '',
        password: '',
        phonenumber: '',
        email: '',
        status: '0',
        deptId: undefined,
        postIds: [],
        roleIds: []
      }
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    }
  }
}
</script>
