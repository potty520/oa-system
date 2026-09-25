<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px" class="filter-container">
      <el-form-item label="部门名称" prop="deptName">
        <el-input v-model="queryParams.deptName" placeholder="请输入部门名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
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
        <el-button v-hasPermi="['system:dept:add']" type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-sort" @click="toggleExpandAll">展开/折叠</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="tableData"
      row-key="deptId"
      :default-expand-all="isExpandAll"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column label="部门名称" prop="deptName" min-width="200" show-overflow-tooltip />
      <el-table-column label="排序" prop="orderNum" width="100" align="center" />
      <el-table-column label="负责人" prop="leader" width="120" align="center" />
      <el-table-column label="联系电话" prop="phone" width="130" align="center" />
      <el-table-column label="邮箱" prop="email" min-width="150" show-overflow-tooltip />
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
      <el-table-column label="操作" min-width="180" align="center">
        <template slot-scope="scope">
          <el-button v-hasPermi="['system:dept:edit']" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button v-hasPermi="['system:dept:add']" size="mini" type="text" icon="el-icon-plus" @click="handleAdd(scope.row)">新增</el-button>
          <el-button v-if="scope.row.parentId !== 0" v-hasPermi="['system:dept:remove']" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="上级部门" prop="parentId">
          <treeselect v-model="form.parentId" :options="deptOptions" :normalizer="normalizer" placeholder="请选择上级部门" />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="显示排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="form.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
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
import request from '@/utils/request'
import { checkPermi } from '@/utils/permission'
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'

export default {
  name: 'Dept',
  components: { Treeselect },
  computed: {
    canEditStatus() { return checkPermi('system:dept:edit') }
  },
  data() {
    return {
      loading: false,
      tableData: [],
      deptOptions: [],
      isExpandAll: true,
      refreshTable: true,
      queryParams: {
        deptName: undefined,
        status: undefined
      },
      dialogVisible: false,
      dialogTitle: '',
      form: {
        deptId: undefined,
        parentId: 0,
        deptName: '',
        orderNum: 0,
        leader: '',
        phone: '',
        email: '',
        status: '0'
      },
      rules: {
        deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
        orderNum: [{ required: true, message: '请输入排序', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      request({ url: '/system/dept/list', method: 'get', params: this.queryParams }).then(res => {
        this.tableData = this.handleTree(res.data || [], 'deptId', 'parentId')
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleTree(data, id, parentId) {
      const config = {
        id: id || 'id',
        parentId: parentId || 'parentId',
        childrenList: 'children'
      }
      const childrenListName = config.childrenList
      const parentIdName = config.parentId
      const idName = config.id
      const map = {}
      const treeData = []
      const addList = (array) => {
        array.forEach(item => {
          item[childrenListName] = []
          map[item[idName]] = item
        })
      }
      const getTree = (array) => {
        array.forEach(item => {
          if (item[parentIdName] === 0) {
            treeData.push(item)
          }
          if (map[item[parentIdName]]) {
            map[item[parentIdName]][childrenListName].push(item)
          }
        })
      }
      addList(data)
      getTree(data)
      return treeData
    },
    handleQuery() {
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.handleQuery()
    },
    toggleExpandAll() {
      this.refreshTable = false
      this.isExpandAll = !this.isExpandAll
      this.$nextTick(() => {
        this.refreshTable = true
      })
    },
    handleAdd(row) {
      this.resetForm()
      this.loadDeptOptions()
      if (row && row.deptId) {
        this.form.parentId = row.deptId
      }
      this.dialogTitle = '新增部门'
      this.dialogVisible = true
    },
    handleUpdate(row) {
      this.resetForm()
      this.loadDeptOptions()
      request({ url: '/system/dept/' + row.deptId, method: 'get' }).then(res => {
        this.form = res.data || {}
        this.dialogTitle = '修改部门'
        this.dialogVisible = true
      })
    },
    handleDelete(row) {
      this.$confirm('确定删除该部门吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return request({ url: '/system/dept/' + row.deptId, method: 'delete' })
      }).then(() => {
        this.getList()
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '停用'
      this.$confirm('确认要' + text + '该部门吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return request({ url: '/system/dept/changeStatus', method: 'put', data: { deptId: row.deptId, status: row.status } })
      }).then(() => {
        this.$message.success(text + '成功')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    loadDeptOptions() {
      request({ url: '/system/dept/list', method: 'get' }).then(res => {
        this.deptOptions = []
        const data = [{ deptId: 0, deptName: '主类目', children: [] }]
        data[0].children = res.data || []
        this.deptOptions = data
      }).catch(() => {})
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
          if (this.form.deptId) {
            request({ url: '/system/dept', method: 'put', data: this.form }).then(() => {
              this.$message.success('修改成功')
              this.dialogVisible = false
              this.getList()
            })
          } else {
            request({ url: '/system/dept', method: 'post', data: this.form }).then(() => {
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
        deptId: undefined,
        parentId: 0,
        deptName: '',
        orderNum: 0,
        leader: '',
        phone: '',
        email: '',
        status: '0'
      }
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    }
  }
}
</script>
