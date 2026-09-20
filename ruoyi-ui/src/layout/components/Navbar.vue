<template>
  <div class="navbar">
    <hamburger :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />
    <breadcrumb class="breadcrumb-container" />

    <div class="right-menu">
      <span class="user-name">{{ name }}</span>
      <el-button type="text" class="menu-action" @click="$router.push('/')">首页</el-button>
      <el-button type="text" class="menu-action danger" @click="logout">退出登录</el-button>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'

export default {
  components: {
    Breadcrumb,
    Hamburger
  },
  computed: {
    ...mapGetters(['sidebar', 'userInfo']),
    name() {
      return this.userInfo.nickName || this.userInfo.userName || 'Admin'
    }
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/ToggleSideBar')
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