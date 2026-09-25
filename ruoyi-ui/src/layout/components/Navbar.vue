<template>
  <div class="navbar">
    <hamburger :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />
    <breadcrumb class="breadcrumb-container" />

    <div class="right-menu">
      <span class="user-name">{{ name }}</span>
      <el-button type="text" class="menu-action" @click="$router.push('/')">首页</el-button>
      <el-button type="text" class="menu-action" @click="openChangePwd">修改密码</el-button>
      <el-button type="text" class="menu-action danger" @click="logout">退出登录</el-button>
    </div>

    <el-dialog
      :title="forced ? '首次登录请修改密码' : '修改密码'"
      :visible.sync="pwdDialogVisible"
      :close-on-click-modal="false"
      :close-on-press-escape="!forced"
      :show-close="!forced"
      width="440px"
      append-to-body
    >
      <el-form ref="pwdForm" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" autocomplete="current-password" placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" autocomplete="new-password" placeholder="8-64 位，需同时包含字母和数字" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="请再次输入新密码"
            @keyup.enter.native="submitPwd"
          />
        </el-form-item>
      </el-form>
      <div v-if="forced" class="pwd-tip">为了账号安全，初始密码必须修改后才能继续使用系统。</div>
      <div slot="footer">
        <el-button v-if="!forced" @click="pwdDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitPwd">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'
import { updateUserPwd } from '@/api/system/user'

export default {
  components: {
    Breadcrumb,
    Hamburger
  },
  data() {
    const validateConfirm = (rule, value, callback) => {
      if (value !== this.pwdForm.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
      } else {
        callback()
      }
    }
    return {
      pwdDialogVisible: false,
      pwdLoading: false,
      pwdForm: { oldPassword: '', newPassword: '', confirmPassword: '' },
      pwdRules: {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 8, max: 64, message: '密码长度需为 8-64 位', trigger: 'blur' },
          { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码需同时包含字母和数字', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: validateConfirm, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters(['sidebar', 'userInfo']),
    name() {
      return this.userInfo.nickName || this.userInfo.userName || 'Admin'
    },
    forced() {
      return this.userInfo.mustChangePwd === true || this.userInfo.mustChangePwd === '1'
    }
  },
  watch: {
    forced: {
      handler(value) {
        if (value) {
          this.openChangePwd()
        }
      },
      immediate: true
    }
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/ToggleSideBar')
    },
    openChangePwd() {
      this.pwdForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
      this.pwdDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.pwdForm) {
          this.$refs.pwdForm.clearValidate()
        }
      })
    },
    submitPwd() {
      this.$refs.pwdForm.validate(valid => {
        if (!valid) {
          return
        }
        this.pwdLoading = true
        updateUserPwd({
          oldPassword: this.pwdForm.oldPassword,
          newPassword: this.pwdForm.newPassword
        }).then(() => {
          this.pwdLoading = false
          this.pwdDialogVisible = false
          this.$store.commit('user/SET_MUST_CHANGE_PWD', false)
          this.$message.success('密码修改成功')
        }).catch(() => {
          this.pwdLoading = false
        })
      })
    },
    logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('user/Logout').then(() => {
          location.href = '/'
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
// 对话框使用 append-to-body，样式必须写在顶层作用域才能命中
.pwd-tip {
  color: #e6a23c;
  font-size: 12px;
  line-height: 1.6;
}

.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px 0 rgba(0, 21, 41, 0.08);

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background 0.3s;
    -webkit-tap-highlight-color: transparent;

    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .right-menu {
    float: right;
    display: flex;
    align-items: center;
    height: 100%;
    padding-right: 12px;

    .user-name {
      margin-right: 12px;
      font-size: 14px;
      color: #5a5e66;
    }

    .menu-action {
      height: 50px;
      margin-left: 4px;
      color: #5a5e66;
      font-size: 14px;

      &:hover {
        color: #409eff;
        background: rgba(0, 0, 0, 0.025);
      }

      &.danger:hover {
        color: #f56c6c;
      }
    }
  }
}
</style>