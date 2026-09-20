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
        meta: { title: '公告管理', icon: 'el-icon-s-bell' }
      },
      {
        path: 'schedule',
        component: () => import('../views/oa/schedule/index'),
        name: 'Schedule',
        meta: { title: '日程管理', icon: 'el-icon-date' }
      },
      {
        path: 'leave',
        component: () => import('../views/oa/leave/index'),
        name: 'Leave',
        meta: { title: '请假管理', icon: 'el-icon-s-promotion' }
      },
      {
        path: 'reimbursement',
        component: () => import('../views/oa/reimbursement/index'),
        name: 'Reimbursement',
        meta: { title: '报销管理', icon: 'el-icon-money' }
      },
      {
        path: 'incoming-doc',
        component: () => import('../views/oa/incoming-doc/index'),
        name: 'IncomingDoc',
        meta: { title: '收文管理', icon: 'el-icon-download' }
      },
      {
        path: 'outgoing-doc',
        component: () => import('../views/oa/outgoing-doc/index'),
        name: 'OutgoingDoc',
        meta: { title: '发文管理', icon: 'el-icon-upload' }
      },
      {
        path: 'doc-flow',
        component: () => import('../views/oa/doc-flow/index'),
        name: 'DocFlow',
        meta: { title: '公文流转', icon: 'el-icon-sort' }
      },
      {
        path: 'todo',
        component: () => import('../views/oa/todo/index'),
        name: 'Todo',
        meta: { title: '待办事项', icon: 'el-icon-s-claim' }
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
        meta: { title: '用户管理', icon: 'el-icon-user' }
      },
      {
        path: 'role',
        component: () => import('../views/system/role/index'),
        name: 'Role',
        meta: { title: '角色管理', icon: 'el-icon-s-custom' }
      },
      {
        path: 'dept',
        component: () => import('../views/system/dept/index'),
        name: 'Dept',
        meta: { title: '部门管理', icon: 'el-icon-s-management' }
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
