import Vue from 'vue'
import Router from 'vue-router'
import Layout from '../layout/index'

Vue.use(Router)

export const constantRoutes = [
  {
    path: '/login',
    component: () => import('../views/login/index'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('../views/error/404'),
    hidden: true
  },
  {
    // 直接输入地址访问无权限页面时由路由守卫重定向到这里（侧边栏本就不显示这些菜单）
    path: '/403',
    component: () => import('../views/error/403'),
    hidden: true
  },
  {
    // TagsView“刷新页面”跳到这里，由组件立刻 replace 回原地址，用于重建页面组件
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('../views/redirect/index')
      }
    ]
  },
  {
    path: '',
    component: Layout,
    redirect: 'dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('../views/dashboard/index'),
        name: 'Dashboard',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/oa',
    component: Layout,
    redirect: '/oa/announcement',
    name: 'OA',
    meta: { title: '办公管理', icon: 'el-icon-s-order' },
    children: [
      {
        path: 'announcement',
        component: () => import('../views/oa/announcement/index'),
        name: 'Announcement',
        meta: { title: '公告管理', icon: 'el-icon-s-bell', permissions: ['oa:announcement:list'] }
      },
      {
        path: 'schedule',
        component: () => import('../views/oa/schedule/index'),
        name: 'Schedule',
        meta: { title: '日程管理', icon: 'el-icon-date', permissions: ['oa:schedule:list'] }
      },
      {
        path: 'leave',
        component: () => import('../views/oa/leave/index'),
        name: 'Leave',
        meta: { title: '请假管理', icon: 'el-icon-s-promotion', permissions: ['oa:leave:list'] }
      },
      {
        path: 'reimbursement',
        component: () => import('../views/oa/reimbursement/index'),
        name: 'Reimbursement',
        meta: { title: '报销管理', icon: 'el-icon-money', permissions: ['oa:reimbursement:list'] }
      },
      {
        path: 'incoming-doc',
        component: () => import('../views/oa/incoming-doc/index'),
        name: 'IncomingDoc',
        meta: { title: '收文管理', icon: 'el-icon-download', permissions: ['oa:incomingDoc:list'] }
      },
      {
        path: 'outgoing-doc',
        component: () => import('../views/oa/outgoing-doc/index'),
        name: 'OutgoingDoc',
        meta: { title: '发文管理', icon: 'el-icon-upload', permissions: ['oa:outgoingDoc:list'] }
      },
      {
        path: 'doc-flow',
        component: () => import('../views/oa/doc-flow/index'),
        name: 'DocFlow',
        meta: { title: '公文流转', icon: 'el-icon-sort', permissions: ['oa:docFlow:list'] }
      },
      {
        path: 'doc-center',
        component: () => import('../views/oa/doc-center/index'),
        name: 'DocCenter',
        meta: { title: '公文中心', icon: 'el-icon-document', permissions: ['oa:doc:list'] }
      },
      {
        path: 'todo',
        component: () => import('../views/oa/todo/index'),
        name: 'Todo',
        meta: { title: '待办事项', icon: 'el-icon-s-claim', permissions: ['oa:todo:list'] }
      },
      {
        path: 'notice',
        component: () => import('../views/oa/notice/index'),
        name: 'Notice',
        meta: { title: '通知公告', icon: 'el-icon-message' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/user',
    name: 'System',
    meta: { title: '系统管理', icon: 'el-icon-setting' },
    children: [
      {
        path: 'user',
        component: () => import('../views/system/user/index'),
        name: 'User',
        meta: { title: '用户管理', icon: 'el-icon-user', permissions: ['system:user:list'] }
      },
      {
        path: 'role',
        component: () => import('../views/system/role/index'),
        name: 'Role',
        meta: { title: '角色管理', icon: 'el-icon-s-custom', permissions: ['system:role:list'] }
      },
      {
        path: 'dept',
        component: () => import('../views/system/dept/index'),
        name: 'Dept',
        meta: { title: '部门管理', icon: 'el-icon-s-management', permissions: ['system:dept:list'] }
      },
      {
        path: 'post',
        component: () => import('../views/system/post/index'),
        name: 'Post',
        meta: { title: '岗位管理', icon: 'el-icon-postcard', permissions: ['system:post:list'] }
      },
      {
        path: 'log',
        component: () => import('../views/system/log/index'),
        name: 'OperLog',
        meta: { title: '操作日志', icon: 'el-icon-document-checked', permissions: ['system:log:list'] }
      }
    ]
  },
  {
    path: '*',
    redirect: '/404',
    hidden: true
  }
]

const router = new Router({
  mode: 'history',
  routes: constantRoutes
})

export default router
