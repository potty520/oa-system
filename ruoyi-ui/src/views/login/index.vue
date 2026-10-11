<template>
  <div class="login-container">
    <el-form
      ref="loginForm"
      :model="loginForm"
      :rules="loginRules"
      class="login-form"
      autocomplete="on"
      label-position="left"
    >
      <div class="title-container">
        <h3 class="title">OA办公系统</h3>
      </div>

      <el-form-item prop="username">
        <span class="svg-container">
          <i class="el-icon-user" />
        </span>
        <el-input
          ref="username"
          v-model="loginForm.username"
          placeholder="用户名"
          name="username"
          type="text"
          tabindex="1"
          autocomplete="on"
        />
      </el-form-item>

      <el-form-item prop="password">
        <span class="svg-container">
          <i class="el-icon-lock" />
        </span>
        <el-input
          :key="passwordType"
          ref="password"
          v-model="loginForm.password"
          :type="passwordType"
          placeholder="密码"
          name="password"
          tabindex="2"
          autocomplete="on"
          @keyup.enter.native="handleLogin"
        />
        <span class="show-pwd" @click="showPwd">
          <i :class="passwordType === 'password' ? 'el-icon-view' : 'el-icon-hide'" />
        </span>
      </el-form-item>

      <div class="remember-row">
        <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
      </div>

      <el-button
        :loading="loading"
        type="primary"
        style="width: 100%; margin-bottom: 30px"
        @click.native.prevent="handleLogin"
      >
        登 录
      </el-button>
    </el-form>
  </div>
</template>

<script>
import Cookies from 'js-cookie'

export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: '',
        rememberMe: false
      },
      loginRules: {
        username: [{ required: true, trigger: 'blur', message: '请输入用户名' }],
        password: [{ required: true, trigger: 'blur', message: '请输入密码' }]
      },
      passwordType: 'password',
      loading: false,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCookie()
  },
  methods: {
    getCookie() {
      const username = Cookies.get('username')
      const rememberMe = Cookies.get('rememberMe')
      // 早期版本曾把明文口令存进 Cookie，这里主动清掉残留
      Cookies.remove('password')
      this.loginForm = {
        username: username === undefined ? '' : username,
        password: '',
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    safeRedirect() {
      const target = this.redirect
      // 只接受站内绝对路径，拒绝 //host 与 /\\host 这类协议相对地址，避免开放重定向
      if (typeof target === 'string' && /^\/(?![/\\])/.test(target)) {
        return target
      }
      return '/'
    },
    showPwd() {
      if (this.passwordType === 'password') {
        this.passwordType = ''
      } else {
        this.passwordType = 'password'
      }
      this.$nextTick(() => {
        this.$refs.password.focus()
      })
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          // “记住我”只记用户名：口令落 Cookie 等于把凭据交给任何能读到浏览器存储的一方
          if (this.loginForm.rememberMe) {
            Cookies.set('username', this.loginForm.username, { expires: 30, sameSite: 'Lax' })
            Cookies.set('rememberMe', true, { expires: 30, sameSite: 'Lax' })
          } else {
            Cookies.remove('username')
            Cookies.remove('rememberMe')
          }
          this.$store
            .dispatch('user/Login', this.loginForm)
            .then(() => {
              this.$router.push(this.safeRedirect())
              this.loading = false
            })
            .catch(() => {
              this.loading = false
            })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100%;
  width: 100%;
  /* 登录页背景插画（手写 SVG，与配色规范同源；矢量自适应任意分辨率）
     内容为大号柔和极光色块 + 漂浮的「公文卡片」装饰，中间留白保证白卡可读。 */
  background: url('~@/assets/login-bg.svg') center center / cover no-repeat, #f8fafc;
  display: flex;
  justify-content: center;
  align-items: center;

  .login-form {
    width: 520px;
    max-width: 100%;
    padding: 35px 35px 15px;
    margin: 0 auto;
    overflow: hidden;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 18px 46px rgba(15, 23, 42, 0.12);
  }

  .svg-container {
    padding: 6px 5px 6px 15px;
    color: #64748b;
    vertical-align: middle;
    width: 30px;
    display: inline-block;
  }

  .title-container {
    position: relative;

    .title {
      font-size: 26px;
      color: #0f172a;
      margin: 0px auto 30px auto;
      text-align: center;
      font-weight: bold;
    }
  }

  .show-pwd {
    position: absolute;
    right: 10px;
    top: 3px;
    font-size: 16px;
    color: #64748b;
    cursor: pointer;
    user-select: none;
  }

  .remember-row {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
  }
}
</style>
