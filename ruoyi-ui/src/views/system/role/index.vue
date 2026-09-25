<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px" class="filter-container">
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="权限字符" prop="roleKey">
        <el-input v-model="queryParams.roleKey" placeholder="请输入权限字符" clearable @keyup.enter.native="handleQuery" />
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
        <el-button v-hasPermi="['system:role:add']" type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button v-hasPermi="['system:role:remove']" type="danger" plain icon="el-icon-delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="角色编号" prop="roleId" width="100" align="center" />
      <el-table-column label="角色名称" prop="roleName" min-width="120" show-overflow-tooltip />
      <el-table-column label="权限字符" prop="roleKey" min-width="120" show-overflow-tooltip />
      <el-table-column label="显示顺序" prop="roleSort" width="100" align="center" />
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
      <el-table-column label="操作" min-width="160" align="center">
        <template slot-scope="scope">
          <el-button v-hasPermi="['system:role:edit']" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button v-hasPermi="['system:role:remove']" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="620px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="请输入权限字符" />
        </el-form-item>
        <el-form-item label="角色顺序" prop="roleSort">
          <el-input-number v-model="form.roleSort" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width:100%">
            <el-option label="全部数据" value="1" />
            <el-option label="自定义数据" value="2" />
            <el-option label="本部门数据" value="3" />
            <el-option label="本部门及以下" value="4" />
            <el-option label="仅本人数据" value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.dataScope === '2'" label="自定义部门">
          <el-input v-model="form.deptIdsText" placeholder="部门ID，多个用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="菜单权限">
          <div class="menu-tree-toolbar">
            <el-checkbox v-model="menuExpandAll" @change="handleMenuExpandAll">展开/折叠</el-checkbox>
            <el-checkbox v-model="menuCheckAll" @change="handleMenuCheckAll">全选/全不选</el-checkbox>
            <span class="menu-tree-tip">勾选到按钮级才会授予对应操作权限</span>
          </div>
          <el-tree
            ref="menuTree"
            :data="menuOptions"
            :props="menuProps"
            node-key="menuId"
            show-checkbox
            check-strictly
            empty-text="暂无菜单数据"
            class="menu-tree"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
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

export default {
  computed: {
    canEditStatus() { return checkPermi('system:role:edit') }
  },
  name: 'Role',
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
        roleName: undefined,
        roleKey: undefined,
        status: undefined
      },
      dialogVisible: false,
      dialogTitle: '',
      menuOptions: [],
      menuProps: { label: 'menuName', children: 'children' },
      menuExpandAll: false,
      menuCheckAll: false,
      form: {
        roleId: undefined,
        roleName: '',
        roleKey: '',
        roleSort: 0,
        dataScope: '1',
        deptIds: [],
        deptIdsText: '',
        menuIds: [],
        status: '0',
        remark: ''
      },
      rules: {
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      request({ url: '/system/role/list', method: 'get', params: this.queryParams }).then(res => {
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
      this.ids = selection.map(item => item.roleId)
      this.multiple = !selection.length
    },
    handleAdd() {
      this.resetForm()
      this.dialogTitle = '新增角色'
      this.dialogVisible = true
      this.loadMenuTree([])
    },
    handleUpdate(row) {
      this.resetForm()
      const roleId = row.roleId || this.ids[0]
      request({ url: '/system/role/' + roleId, method: 'get' }).then(res => {
        this.form = res.data || {}
        this.form.deptIdsText = (this.form.deptIds || []).join(',')
        this.dialogTitle = '修改角色'
        this.dialogVisible = true
        this.loadMenuTree(this.form.menuIds || [])
      })
    },
    loadMenuTree(checkedKeys) {
      this.menuExpandAll = false
      this.menuCheckAll = false
      return request({ url: '/system/menu/tree', method: 'get' }).then(res => {
        this.menuOptions = res.data || []
        this.$nextTick(() => {
          if (this.$refs.menuTree) {
            this.$refs.menuTree.setCheckedKeys(checkedKeys || [])
          }
        })
      }).catch(() => {
        this.menuOptions = []
      })
    },
    handleMenuExpandAll(value) {
      if (!this.$refs.menuTree) {
        return
      }
      const nodesMap = this.$refs.menuTree.store.nodesMap
      Object.keys(nodesMap).forEach(key => {
        nodesMap[key].expanded = value
      })
    },
    handleMenuCheckAll(value) {
      if (!this.$refs.menuTree) {
        return
      }
      this.$refs.menuTree.setCheckedKeys(value ? this.collectMenuIds(this.menuOptions) : [])
    },
    collectMenuIds(nodes) {
      return (nodes || []).reduce((acc, node) => {
        acc.push(node.menuId)
        return acc.concat(this.collectMenuIds(node.children))
      }, [])
    },
    handleDelete(row) {
      const roleIds = row.roleId ? [row.roleId] : this.ids
      this.$confirm('确定删除选中的角色吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return request({ url: '/system/role/' + roleIds.join(','), method: 'delete' })
      }).then(() => {
        this.getList()
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '停用'
      this.$confirm('确认要' + text + '角色"' + row.roleName + '"吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return request({ url: '/system/role/changeStatus', method: 'put', data: { roleId: row.roleId, status: row.status } })
      }).then(() => {
        this.$message.success(text + '成功')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          const submitData = {
            ...this.form,
            deptIds: this.form.deptIdsText ? this.form.deptIdsText.split(',').map(value => Number(value.trim())).filter(Boolean) : [],
            menuIds: this.$refs.menuTree ? this.$refs.menuTree.getCheckedKeys() : (this.form.menuIds || [])
          }
          delete submitData.deptIdsText
          delete submitData.children
          if (this.form.roleId) {
            request({ url: '/system/role', method: 'put', data: submitData }).then(() => {
              this.$message.success('修改成功')
              this.dialogVisible = false
              this.getList()
            })
          } else {
            request({ url: '/system/role', method: 'post', data: submitData }).then(() => {
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
        roleId: undefined,
        roleName: '',
        roleKey: '',
        roleSort: 0,
        dataScope: '1',
        deptIds: [],
        deptIdsText: '',
        menuIds: [],
        status: '0',
        remark: ''
      }
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
      if (this.$refs.menuTree) {
        this.$refs.menuTree.setCheckedKeys([])
      }
    }
  }
}
</script>

<style scoped>
.menu-tree-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 6px;
}
.menu-tree-tip {
  color: #909399;
  font-size: 12px;
}
.menu-tree {
  max-height: 300px;
  overflow: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 4px;
}
</style>
