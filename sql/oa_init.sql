-- ============================================================
-- OA办公系统数据库初始化脚本
-- ============================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS oa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE oa_system;

-- ============================================================
-- 2. 系统管理表
-- ============================================================

-- 部门信息表
CREATE TABLE sys_dept (
  dept_id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  parent_id    BIGINT DEFAULT 0 COMMENT '父部门ID',
  ancestors    VARCHAR(200) DEFAULT '' COMMENT '祖级列表',
  dept_name    VARCHAR(50) NOT NULL COMMENT '部门名称',
  order_num    INT DEFAULT 0 COMMENT '显示顺序',
  leader       VARCHAR(30) DEFAULT '' COMMENT '负责人',
  phone        VARCHAR(11) DEFAULT '' COMMENT '联系电话',
  email        VARCHAR(50) DEFAULT '' COMMENT '邮箱',
  status       CHAR(1) DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  create_by    VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time  DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by    VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time  DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag     CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (dept_id),
  KEY idx_parent_id (parent_id)
) ENGINE=InnoDB COMMENT='部门信息表';
-- 用户信息表
CREATE TABLE sys_user (
  user_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username      VARCHAR(30) NOT NULL COMMENT '用户名',
  password      VARCHAR(200) NOT NULL COMMENT '密码',
  nickname      VARCHAR(30) DEFAULT '' COMMENT '真实姓名',
  email         VARCHAR(50) DEFAULT '' COMMENT '邮箱',
  phone         VARCHAR(11) DEFAULT '' COMMENT '手机号',
  avatar        VARCHAR(200) DEFAULT '' COMMENT '头像',
  sex           CHAR(1) DEFAULT '0' COMMENT '性别（0男 1女 2未知）',
  dept_id       BIGINT DEFAULT NULL COMMENT '部门ID',
  status        CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  must_change_pwd CHAR(1) DEFAULT '0' COMMENT '首次登录必须改密（0否 1是）',
  login_ip      VARCHAR(128) DEFAULT '' COMMENT '最后登录IP',
  login_date    DATETIME DEFAULT NULL COMMENT '最后登录时间',
  create_by     VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time   DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by     VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time   DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag      CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='用户信息表';

-- 角色信息表
CREATE TABLE sys_role (
  role_id     BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_name   VARCHAR(30) NOT NULL COMMENT '角色名称',
  role_key    VARCHAR(100) NOT NULL COMMENT '角色权限字符串',
  role_sort   INT DEFAULT 0 COMMENT '显示顺序',
  data_scope  CHAR(1) DEFAULT '1' COMMENT '数据范围（1全部 2自定义 3本部门 4本部门及以下 5本人）',
  status      CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  create_by   VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by   VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag    CHAR(1) DEFAULT '0' COMMENT '删除标志',
  remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (role_id)
) ENGINE=InnoDB COMMENT='角色信息表';

-- 菜单权限表
CREATE TABLE sys_menu (
  menu_id     BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  menu_name   VARCHAR(50) NOT NULL COMMENT '菜单名称',
  parent_id   BIGINT DEFAULT 0 COMMENT '父菜单ID',
  order_num   INT DEFAULT 0 COMMENT '显示顺序',
  path        VARCHAR(200) DEFAULT '' COMMENT '路由地址',
  component   VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  menu_type   CHAR(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  visible     CHAR(1) DEFAULT '0' COMMENT '是否显示（0显示 1隐藏）',
  status      CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  perms       VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  icon        VARCHAR(100) DEFAULT '#' COMMENT '菜单图标',
  create_by   VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by   VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag    CHAR(1) DEFAULT '0' COMMENT '删除标志',
  remark      VARCHAR(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (menu_id)
) ENGINE=InnoDB COMMENT='菜单权限表';

-- 用户和角色关联表
CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB COMMENT='用户和角色关联表';

-- 角色和菜单关联表
CREATE TABLE sys_role_menu (
  role_id BIGINT NOT NULL COMMENT '角色ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB COMMENT='角色和菜单关联表';

-- 岗位信息表
CREATE TABLE sys_post (
  post_id     BIGINT NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  post_code   VARCHAR(64) NOT NULL COMMENT '岗位编码',
  post_name   VARCHAR(50) NOT NULL COMMENT '岗位名称',
  post_sort   INT DEFAULT 0 COMMENT '显示顺序',
  status      CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  create_by   VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by   VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag    CHAR(1) DEFAULT '0' COMMENT '删除标志',
  remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (post_id),
  UNIQUE KEY uk_post_code (post_code)
) ENGINE=InnoDB COMMENT='岗位信息表';

-- 用户和岗位关联表
CREATE TABLE sys_user_post (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  post_id BIGINT NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (user_id, post_id)
) ENGINE=InnoDB COMMENT='用户和岗位关联表';

-- 角色和部门关联表
CREATE TABLE sys_role_dept (
  role_id BIGINT NOT NULL COMMENT '角色ID',
  dept_id BIGINT NOT NULL COMMENT '部门ID',
  PRIMARY KEY (role_id, dept_id)
) ENGINE=InnoDB COMMENT='角色和部门关联表';

-- 操作日志表
CREATE TABLE sys_oper_log (
  log_id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  title          VARCHAR(100) DEFAULT '' COMMENT '模块标题',
  business_type  VARCHAR(20) DEFAULT 'OTHER' COMMENT '业务类型',
  method         VARCHAR(500) DEFAULT '' COMMENT '方法名称',
  request_method VARCHAR(10) DEFAULT '' COMMENT '请求方式',
  operator_name  VARCHAR(64) DEFAULT '' COMMENT '操作人员',
  oper_url       VARCHAR(500) DEFAULT '' COMMENT '请求URL',
  oper_ip        VARCHAR(128) DEFAULT '' COMMENT '主机地址',
  oper_param     TEXT COMMENT '请求参数',
  json_result    TEXT COMMENT '返回参数',
  status         CHAR(1) DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  error_msg      TEXT COMMENT '错误消息',
  oper_time      DATETIME DEFAULT NULL COMMENT '操作时间',
  create_by      VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time    DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by      VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time    DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag       CHAR(1) DEFAULT '0' COMMENT '删除标志',
  remark         VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (log_id),
  KEY idx_oper_time (oper_time),
  KEY idx_operator_name (operator_name)
) ENGINE=InnoDB COMMENT='操作日志表';

-- ============================================================
-- 3. OA业务表
-- ============================================================

-- 系统公告表
CREATE TABLE oa_announcement (
  ann_id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  ann_title           VARCHAR(200) NOT NULL COMMENT '公告标题',
  ann_type            CHAR(1) DEFAULT '1' COMMENT '公告类型（1通知 2公告）',
  ann_content         TEXT COMMENT '公告内容',
  ann_level           CHAR(1) DEFAULT '1' COMMENT '公告级别（1普通 2重要 3紧急）',
  attachment_url      VARCHAR(500) DEFAULT '' COMMENT '附件地址',
  publish_start_time  DATETIME DEFAULT NULL COMMENT '发布开始时间',
  publish_end_time    DATETIME DEFAULT NULL COMMENT '发布结束时间',
  is_top              CHAR(1) DEFAULT '0' COMMENT '是否置顶（0否 1是）',
  is_public           CHAR(1) DEFAULT '0' COMMENT '是否公开（0否 1是）',
  view_count          INT DEFAULT 0 COMMENT '查看次数',
  status              CHAR(1) DEFAULT '0' COMMENT '状态（0草稿 1已发布 2已撤回）',
  create_by           VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time         DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by           VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time         DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag            CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark              VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (ann_id)
) ENGINE=InnoDB COMMENT='系统公告表';

-- 日程安排表
CREATE TABLE oa_schedule (
  schedule_id     BIGINT NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  schedule_title  VARCHAR(200) NOT NULL COMMENT '日程标题',
  schedule_type   CHAR(1) DEFAULT '1' COMMENT '日程类型（1工作 2个人 3会议 4其他）',
  schedule_color  VARCHAR(20) DEFAULT '' COMMENT '日程颜色',
  start_time      DATETIME NOT NULL COMMENT '开始时间',
  end_time        DATETIME NOT NULL COMMENT '结束时间',
  schedule_content TEXT COMMENT '日程内容',
  location        VARCHAR(200) DEFAULT '' COMMENT '地点',
  remind_flag     CHAR(1) DEFAULT '0' COMMENT '是否提醒（0否 1是）',
  remind_time     DATETIME DEFAULT NULL COMMENT '提醒时间',
  status          CHAR(1) DEFAULT '0' COMMENT '状态（0进行中 1已完成 2已取消）',
  user_id         BIGINT DEFAULT NULL COMMENT '用户ID',
  create_by       VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time     DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by       VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time     DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag        CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (schedule_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='日程安排表';

-- 请假申请表
CREATE TABLE oa_leave (
  leave_id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '请假ID',
  leave_type        CHAR(1) DEFAULT '1' COMMENT '请假类型（1事假 2病假 3年假 4婚假 5产假 6其他）',
  start_time        DATETIME NOT NULL COMMENT '开始时间',
  end_time          DATETIME NOT NULL COMMENT '结束时间',
  leave_days        DECIMAL(5,1) NOT NULL COMMENT '请假天数',
  leave_reason      TEXT COMMENT '请假原因',
  apply_time        DATETIME DEFAULT NULL COMMENT '申请时间',
  approval_status   CHAR(1) DEFAULT '0' COMMENT '审批状态（0草稿 1待审批 2已通过 3已拒绝 4已撤回）',
  current_step      INT DEFAULT 0 COMMENT '当前审批步骤',
  user_id           BIGINT DEFAULT NULL COMMENT '用户ID',
  user_name         VARCHAR(50) DEFAULT '' COMMENT '用户姓名',
  dept_id           BIGINT DEFAULT NULL COMMENT '部门ID',
  dept_name         VARCHAR(100) DEFAULT '' COMMENT '部门名称',
  approve_by         VARCHAR(50) DEFAULT '' COMMENT '审批人',
  approve_time       DATETIME DEFAULT NULL COMMENT '审批时间',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (leave_id),
  KEY idx_user_id (user_id),
  KEY idx_approval_status (approval_status)
) ENGINE=InnoDB COMMENT='请假申请表';

-- 报销申请表
CREATE TABLE oa_reimbursement (
  reimb_id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '报销ID',
  reimb_type        CHAR(1) DEFAULT '1' COMMENT '报销类型（1差旅 2办公 3招待 4其他）',
  total_amount      DECIMAL(10,2) NOT NULL COMMENT '报销总额',
  reimb_reason      TEXT COMMENT '报销原因',
  apply_time        DATETIME DEFAULT NULL COMMENT '申请时间',
  approval_status   CHAR(1) DEFAULT '0' COMMENT '审批状态（0草稿 1待审批 2已通过 3已拒绝 4已撤回）',
  current_step      INT DEFAULT 0 COMMENT '当前审批步骤',
  payment_status    CHAR(1) DEFAULT '0' COMMENT '支付状态（0待支付 1已支付）',
  payment_time      DATETIME DEFAULT NULL COMMENT '支付时间',
  approve_by        VARCHAR(50) DEFAULT '' COMMENT '审批人',
  approve_time      DATETIME DEFAULT NULL COMMENT '审批时间',
  approve_remark    VARCHAR(500) DEFAULT '' COMMENT '审批备注',
  user_id           BIGINT DEFAULT NULL COMMENT '用户ID',
  user_name         VARCHAR(50) DEFAULT '' COMMENT '用户姓名',
  dept_id           BIGINT DEFAULT NULL COMMENT '部门ID',
  dept_name         VARCHAR(100) DEFAULT '' COMMENT '部门名称',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (reimb_id),
  KEY idx_user_id (user_id),
  KEY idx_approval_status (approval_status)
) ENGINE=InnoDB COMMENT='报销申请表';

-- 报销明细表
CREATE TABLE oa_reimbursement_detail (
  detail_id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  reimb_id          BIGINT NOT NULL COMMENT '报销ID',
  expense_type      VARCHAR(50) DEFAULT '' COMMENT '费用类型',
  expense_date      DATE DEFAULT NULL COMMENT '费用日期',
  expense_amount    DECIMAL(10,2) NOT NULL COMMENT '费用金额',
  expense_desc      VARCHAR(500) DEFAULT '' COMMENT '费用说明',
  receipt_url       VARCHAR(500) DEFAULT '' COMMENT '票据地址',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (detail_id),
  KEY idx_reimb_id (reimb_id)
) ENGINE=InnoDB COMMENT='报销明细表';

-- 报销审批日志表
CREATE TABLE oa_reimbursement_approve_log (
  log_id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  reimb_id          BIGINT NOT NULL COMMENT '报销ID',
  approve_result    CHAR(1) DEFAULT NULL COMMENT '审批结果（1通过 2拒绝）',
  approve_remark    VARCHAR(500) DEFAULT '' COMMENT '审批备注',
  approve_by        VARCHAR(50) DEFAULT '' COMMENT '审批人',
  approve_time      DATETIME DEFAULT NULL COMMENT '审批时间',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (log_id),
  KEY idx_reimb_id (reimb_id)
) ENGINE=InnoDB COMMENT='报销审批日志表';

-- 考勤记录表
CREATE TABLE oa_attendance (
  attendance_id     BIGINT NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
  user_id           BIGINT NOT NULL COMMENT '用户ID',
  attendance_date   DATE NOT NULL COMMENT '考勤日期',
  clock_in_time     DATETIME DEFAULT NULL COMMENT '上班打卡时间',
  clock_out_time    DATETIME DEFAULT NULL COMMENT '下班打卡时间',
  status            CHAR(1) DEFAULT '0' COMMENT '考勤状态（0正常 1迟到 2早退 3旷工）',
  remark            VARCHAR(200) DEFAULT '' COMMENT '备注',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (attendance_id),
  KEY idx_user_id (user_id),
  KEY idx_attendance_date (attendance_date)
) ENGINE=InnoDB COMMENT='考勤记录表';

-- 加班记录表
CREATE TABLE oa_overtime (
  overtime_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '加班ID',
  user_id           BIGINT NOT NULL COMMENT '用户ID',
  overtime_date     DATE NOT NULL COMMENT '加班日期',
  start_time        DATETIME NOT NULL COMMENT '开始时间',
  end_time          DATETIME NOT NULL COMMENT '结束时间',
  overtime_hours    DECIMAL(4,1) NOT NULL COMMENT '加班时长(小时)',
  overtime_type     CHAR(1) DEFAULT '1' COMMENT '加班类型（1工作日 2休息日 3节假日）',
  reason            TEXT COMMENT '加班原因',
  approval_status   CHAR(1) DEFAULT '0' COMMENT '审批状态（0草稿 1待审批 2已通过 3已拒绝）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (overtime_id),
  KEY idx_user_id (user_id),
  KEY idx_overtime_date (overtime_date)
) ENGINE=InnoDB COMMENT='加班记录表';

-- 通讯录表
CREATE TABLE oa_contact (
  contact_id        BIGINT NOT NULL AUTO_INCREMENT COMMENT '联系人ID',
  contact_name      VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  company           VARCHAR(200) DEFAULT '' COMMENT '公司',
  department        VARCHAR(100) DEFAULT '' COMMENT '部门',
  position          VARCHAR(100) DEFAULT '' COMMENT '职位',
  phone             VARCHAR(20) DEFAULT '' COMMENT '电话',
  email             VARCHAR(100) DEFAULT '' COMMENT '邮箱',
  address           VARCHAR(500) DEFAULT '' COMMENT '地址',
  user_id           BIGINT DEFAULT NULL COMMENT '所属用户ID',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (contact_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='通讯录表';

-- 文档管理表
CREATE TABLE oa_document (
  document_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  document_name     VARCHAR(200) NOT NULL COMMENT '文档名称',
  document_type     VARCHAR(50) DEFAULT '' COMMENT '文档类型',
  file_path         VARCHAR(500) DEFAULT '' COMMENT '文件路径',
  file_size         BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
  user_id           BIGINT DEFAULT NULL COMMENT '上传用户ID',
  user_name         VARCHAR(50) DEFAULT '' COMMENT '上传用户姓名',
  status            CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1已删除）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (document_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='文档管理表';

-- 审批流程定义表
CREATE TABLE oa_approval_flow (
  flow_id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '流程ID',
  flow_name         VARCHAR(100) NOT NULL COMMENT '流程名称',
  flow_type         VARCHAR(50) DEFAULT '' COMMENT '流程类型(leave/reimbursement)',
  description       VARCHAR(500) DEFAULT '' COMMENT '流程描述',
  status            CHAR(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (flow_id)
) ENGINE=InnoDB COMMENT='审批流程定义表';

-- 审批流程步骤表
CREATE TABLE oa_approval_step (
  step_id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
  flow_id           BIGINT NOT NULL COMMENT '流程ID',
  step_name         VARCHAR(100) DEFAULT '' COMMENT '步骤名称',
  step_order        INT DEFAULT 0 COMMENT '步骤顺序',
  role_id           BIGINT DEFAULT NULL COMMENT '角色ID',
  role_name         VARCHAR(50) DEFAULT '' COMMENT '角色名称',
  is_sign           CHAR(1) DEFAULT '0' COMMENT '签批方式（0会签 1或签）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (step_id),
  KEY idx_flow_id (flow_id)
) ENGINE=InnoDB COMMENT='审批流程步骤表';

-- 审批记录表
CREATE TABLE oa_approval_record (
  record_id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  flow_id           BIGINT NOT NULL COMMENT '流程ID',
  step_id           BIGINT NOT NULL COMMENT '步骤ID',
  business_type     VARCHAR(50) DEFAULT '' COMMENT '业务类型',
  business_id       BIGINT DEFAULT NULL COMMENT '业务ID',
  user_id           BIGINT DEFAULT NULL COMMENT '审批用户ID',
  user_name         VARCHAR(50) DEFAULT '' COMMENT '审批用户姓名',
  approve_result    CHAR(1) DEFAULT NULL COMMENT '审批结果（1通过 2拒绝）',
  approve_remark    VARCHAR(500) DEFAULT '' COMMENT '审批备注',
  approve_time      DATETIME DEFAULT NULL COMMENT '审批时间',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (record_id),
  KEY idx_flow_id (flow_id),
  KEY idx_business (business_type, business_id)
) ENGINE=InnoDB COMMENT='审批记录表';

-- 公文类型表
CREATE TABLE oa_doc_type (
  doc_type_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '公文类型ID',
  type_name         VARCHAR(100) NOT NULL COMMENT '类型名称',
  type_code         VARCHAR(50) NOT NULL COMMENT '类型编码',
  parent_id         BIGINT DEFAULT 0 COMMENT '父类型ID',
  sort_order        INT DEFAULT 0 COMMENT '排序',
  status            CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (doc_type_id),
  UNIQUE KEY uk_type_code (type_code)
) ENGINE=InnoDB COMMENT='公文类型表';

-- 公文模板表
CREATE TABLE oa_doc_template (
  template_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  template_name     VARCHAR(100) NOT NULL COMMENT '模板名称',
  doc_type          CHAR(1) DEFAULT '2' COMMENT '文书方向（1收文 2发文）',
  doc_type_id       BIGINT DEFAULT NULL COMMENT '公文类型ID',
  red_header_text   VARCHAR(200) DEFAULT '' COMMENT '红头文字',
  title_font        VARCHAR(100) DEFAULT 'SimSun' COMMENT '标题字体',
  title_size        INT DEFAULT 22 COMMENT '标题字号',
  body_font         VARCHAR(100) DEFAULT 'FangSong' COMMENT '正文字体',
  body_size         INT DEFAULT 16 COMMENT '正文字号',
  template_content  TEXT COMMENT '模板内容',
  status            CHAR(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (template_id)
) ENGINE=InnoDB COMMENT='公文模板表';

-- 收文登记表
CREATE TABLE oa_incoming_doc (
  doc_id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '收文ID',
  doc_number        VARCHAR(100) NOT NULL COMMENT '收文编号',
  doc_title         VARCHAR(200) NOT NULL COMMENT '收文标题',
  doc_type_id       BIGINT DEFAULT NULL COMMENT '公文类型ID',
  template_id       BIGINT DEFAULT NULL COMMENT '模板ID',
  flow_id           BIGINT DEFAULT NULL COMMENT '流程ID',
  current_step_id   BIGINT DEFAULT NULL COMMENT '当前流程步骤ID',
  current_step_order INT DEFAULT 0 COMMENT '当前流程步骤顺序',
  source_unit       VARCHAR(200) DEFAULT '' COMMENT '来文单位',
  receive_date      DATE DEFAULT NULL COMMENT '收文日期',
  doc_level         CHAR(1) DEFAULT '1' COMMENT '紧急程度（1普通 2重要 3紧急）',
  doc_secret        CHAR(1) DEFAULT '1' COMMENT '密级（1普通 2秘密 3机密 4绝密）',
  summary           TEXT COMMENT '内容摘要',
  attachment_url    VARCHAR(500) DEFAULT '' COMMENT '附件地址',
  handler_id        BIGINT DEFAULT NULL COMMENT '办理人ID',
  handler_name      VARCHAR(50) DEFAULT '' COMMENT '办理人姓名',
  status            CHAR(1) DEFAULT '0' COMMENT '状态（0待签收 1办理中 2已办结 3已归档）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (doc_id),
  UNIQUE KEY uk_doc_number (doc_number),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='收文登记表';

-- 发文管理表
CREATE TABLE oa_outgoing_doc (
  doc_id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '发文ID',
  doc_number        VARCHAR(100) NOT NULL COMMENT '发文编号',
  doc_title         VARCHAR(200) NOT NULL COMMENT '发文标题',
  doc_type_id       BIGINT DEFAULT NULL COMMENT '公文类型ID',
  template_id       BIGINT DEFAULT NULL COMMENT '模板ID',
  flow_id           BIGINT DEFAULT NULL COMMENT '流程ID',
  current_step_id   BIGINT DEFAULT NULL COMMENT '当前流程步骤ID',
  current_step_order INT DEFAULT 0 COMMENT '当前流程步骤顺序',
  draft_unit        VARCHAR(200) DEFAULT '' COMMENT '拟稿单位',
  draft_date        DATE DEFAULT NULL COMMENT '拟稿日期',
  doc_level         CHAR(1) DEFAULT '1' COMMENT '紧急程度（1普通 2重要 3紧急）',
  doc_secret        CHAR(1) DEFAULT '1' COMMENT '密级（1普通 2秘密 3机密 4绝密）',
  main_send         VARCHAR(500) DEFAULT '' COMMENT '主送单位',
  copy_send         VARCHAR(500) DEFAULT '' COMMENT '抄送单位',
  summary           TEXT COMMENT '内容摘要',
  attachment_url    VARCHAR(500) DEFAULT '' COMMENT '附件地址',
  sign_user_id      BIGINT DEFAULT NULL COMMENT '签发人ID',
  sign_user_name    VARCHAR(50) DEFAULT '' COMMENT '签发人姓名',
  status            CHAR(1) DEFAULT '0' COMMENT '状态（0草稿 1审核中 2已签发 3已归档）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (doc_id),
  UNIQUE KEY uk_doc_number (doc_number),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='发文管理表';

-- 公文流程表
CREATE TABLE oa_doc_flow (
  flow_id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '流程ID',
  flow_name         VARCHAR(100) NOT NULL COMMENT '流程名称',
  flow_type         CHAR(1) DEFAULT '1' COMMENT '流程类型（1收文 2发文）',
  description       VARCHAR(500) DEFAULT '' COMMENT '流程描述',
  status            CHAR(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (flow_id)
) ENGINE=InnoDB COMMENT='公文流程表';

-- 公文流程步骤表
CREATE TABLE oa_doc_flow_step (
  step_id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
  flow_id           BIGINT NOT NULL COMMENT '流程ID',
  step_name         VARCHAR(100) DEFAULT '' COMMENT '步骤名称',
  step_order        INT DEFAULT 0 COMMENT '步骤顺序',
  role_id           BIGINT DEFAULT NULL COMMENT '角色ID',
  is_sign           CHAR(1) DEFAULT '1' COMMENT '是否需要签批（0否 1是）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (step_id),
  KEY idx_flow_id (flow_id)
) ENGINE=InnoDB COMMENT='公文流程步骤表';

-- 公文审批表
CREATE TABLE oa_doc_approval (
  approval_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '审批ID',
  doc_id            BIGINT NOT NULL COMMENT '公文ID',
  doc_type          CHAR(1) NOT NULL COMMENT '公文类型（1收文 2发文）',
  flow_id           BIGINT DEFAULT NULL COMMENT '流程ID',
  step_id           BIGINT DEFAULT NULL COMMENT '步骤ID',
  user_id           BIGINT DEFAULT NULL COMMENT '审批用户ID',
  user_name         VARCHAR(50) DEFAULT '' COMMENT '审批用户姓名',
  approve_result    CHAR(1) DEFAULT NULL COMMENT '审批结果（1通过 2拒绝）',
  approve_remark    VARCHAR(500) DEFAULT '' COMMENT '审批备注',
  approve_time      DATETIME DEFAULT NULL COMMENT '审批时间',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (approval_id),
  KEY idx_doc (doc_id, doc_type)
) ENGINE=InnoDB COMMENT='公文审批表';

-- 公文传阅表
CREATE TABLE oa_doc_circulation (
  circulation_id    BIGINT NOT NULL AUTO_INCREMENT COMMENT '传阅ID',
  doc_id            BIGINT NOT NULL COMMENT '公文ID',
  doc_type          CHAR(1) NOT NULL COMMENT '公文类型（1收文 2发文）',
  user_id           BIGINT NOT NULL COMMENT '传阅用户ID',
  user_name         VARCHAR(50) DEFAULT '' COMMENT '传阅用户姓名',
  read_status       CHAR(1) DEFAULT '0' COMMENT '阅读状态（0未读 1已读）',
  read_time         DATETIME DEFAULT NULL COMMENT '阅读时间',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (circulation_id),
  KEY idx_doc (doc_id, doc_type),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='公文传阅表';

-- 公文催办表
CREATE TABLE oa_doc_reminder (
  reminder_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '催办ID',
  doc_id            BIGINT NOT NULL COMMENT '公文ID',
  doc_type          CHAR(1) NOT NULL COMMENT '公文类型（1收文 2发文）',
  from_user_id      BIGINT DEFAULT NULL COMMENT '催办人ID',
  from_user_name    VARCHAR(50) DEFAULT '' COMMENT '催办人姓名',
  to_user_id        BIGINT DEFAULT NULL COMMENT '被催办人ID',
  to_user_name      VARCHAR(50) DEFAULT '' COMMENT '被催办人姓名',
  reminder_content  VARCHAR(500) DEFAULT '' COMMENT '催办内容',
  reminder_time     DATETIME DEFAULT NULL COMMENT '催办时间',
  status            CHAR(1) DEFAULT '0' COMMENT '状态（0未处理 1已处理）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (reminder_id),
  KEY idx_doc (doc_id, doc_type),
  KEY idx_to_user (to_user_id)
) ENGINE=InnoDB COMMENT='公文催办表';

-- 公文委托代理表
CREATE TABLE oa_doc_delegate (
  delegate_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '委托代理ID',
  doc_id            BIGINT NOT NULL COMMENT '公文ID',
  doc_type          CHAR(1) NOT NULL COMMENT '公文类型（1收文 2发文）',
  from_user_id      BIGINT DEFAULT NULL COMMENT '委托人ID',
  from_user_name    VARCHAR(50) DEFAULT '' COMMENT '委托人姓名',
  to_user_id        BIGINT DEFAULT NULL COMMENT '受托人ID',
  to_user_name      VARCHAR(50) DEFAULT '' COMMENT '受托人姓名',
  delegate_type     CHAR(1) DEFAULT '1' COMMENT '类型（1委托 2代理）',
  opinion           VARCHAR(500) DEFAULT '' COMMENT '说明',
  status            CHAR(1) DEFAULT '0' COMMENT '状态（0进行中 1已结束）',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志',
  remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (delegate_id),
  KEY idx_doc (doc_id, doc_type),
  KEY idx_to_user (to_user_id)
) ENGINE=InnoDB COMMENT='公文委托代理表';

-- 公文归档表
CREATE TABLE oa_doc_archive (
  archive_id        BIGINT NOT NULL AUTO_INCREMENT COMMENT '归档ID',
  doc_id            BIGINT NOT NULL COMMENT '公文ID',
  doc_type          CHAR(1) NOT NULL COMMENT '公文类型（1收文 2发文）',
  archive_code      VARCHAR(100) DEFAULT '' COMMENT '归档编号',
  archive_time      DATETIME DEFAULT NULL COMMENT '归档时间',
  archive_user_id   BIGINT DEFAULT NULL COMMENT '归档人ID',
  archive_user_name VARCHAR(50) DEFAULT '' COMMENT '归档人姓名',
  archive_location  VARCHAR(200) DEFAULT '' COMMENT '归档位置',
  remark            VARCHAR(500) DEFAULT '' COMMENT '备注',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT NULL COMMENT '更新时间',
  del_flag          CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (archive_id),
  KEY idx_doc (doc_id, doc_type)
) ENGINE=InnoDB COMMENT='公文归档表';

-- ============================================================
-- 4. 初始化数据
-- ============================================================

-- 部门
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status) VALUES
(100, 0, '0', '总公司', 1, '超级管理员', '13800000000', 'admin@example.com', '0'),
(101, 100, '0,100', '行政部', 1, '张三', '13800000001', 'hr@example.com', '0'),
(102, 100, '0,100', '财务部', 2, '李四', '13800000002', 'finance@example.com', '0'),
(103, 100, '0,100', '技术部', 3, '王五', '13800000003', 'tech@example.com', '0');
-- 初始账号不内置任何可用口令：password 为占位符（不是合法 BCrypt 串，任何密码都无法登录）。
-- 应用首次启动时由 InitialAccountInitializer 按 OA_INIT_<用户名大写>_PASSWORD 环境变量写入真实口令，
-- 未配置该环境变量时生成一次性随机密码，仅在启动日志输出一次，并强制首次登录修改密码。
INSERT INTO sys_user (user_id, username, password, nickname, dept_id, status, must_change_pwd) VALUES
(1, 'admin', 'PENDING_INITIAL_PASSWORD', '超级管理员', 100, '0', '1'),
(2, 'zhangsan', 'PENDING_INITIAL_PASSWORD', '张三', 103, '0', '1'),
(3, 'lisi', 'PENDING_INITIAL_PASSWORD', '李四', 102, '0', '1');

-- 角色
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status) VALUES
(1, '超级管理员', 'admin', 1, '1', '0'),
(2, '普通用户', 'common', 2, '5', '0'),
(3, '部门经理', 'manager', 3, '4', '0');

-- 岗位
INSERT INTO sys_post (post_id, post_code, post_name, post_sort, status) VALUES
(1, 'admin', '系统管理员', 1, '0'),
(2, 'manager', '部门经理', 2, '0'),
(3, 'staff', '普通员工', 3, '0');

-- 用户岗位关联
INSERT INTO sys_user_post VALUES (1, 1), (2, 3), (3, 2);

-- 用户角色关联
INSERT INTO sys_user_role VALUES (1, 1), (2, 2), (3, 3);

-- 菜单权限（OA 模块 + 系统管理，menu_id 一经发布不再复用）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon) VALUES
(1, 'OA办公', 0, 1, 'oa', NULL, 'M', '0', '0', NULL, 'documentation'),
(10, '公告管理', 1, 1, 'announcement', 'oa/announcement/index', 'C', '0', '0', 'oa:announcement:list', 'message'),
(11, '公告新增', 10, 1, '', NULL, 'F', '0', '0', 'oa:announcement:add', '#'),
(12, '公告修改', 10, 2, '', NULL, 'F', '0', '0', 'oa:announcement:edit', '#'),
(13, '公告删除', 10, 3, '', NULL, 'F', '0', '0', 'oa:announcement:remove', '#'),
(14, '公告查询', 10, 4, '', NULL, 'F', '0', '0', 'oa:announcement:query', '#'),
(20, '日程管理', 1, 2, 'schedule', 'oa/schedule/index', 'C', '0', '0', 'oa:schedule:list', 'date'),
(21, '日程查询', 20, 1, '', NULL, 'F', '0', '0', 'oa:schedule:query', '#'),
(22, '日程新增', 20, 2, '', NULL, 'F', '0', '0', 'oa:schedule:add', '#'),
(23, '日程修改', 20, 3, '', NULL, 'F', '0', '0', 'oa:schedule:edit', '#'),
(24, '日程删除', 20, 4, '', NULL, 'F', '0', '0', 'oa:schedule:remove', '#'),
(30, '请假管理', 1, 3, 'leave', 'oa/leave/index', 'C', '0', '0', 'oa:leave:list', 'tree-table'),
(31, '请假提交', 30, 1, '', NULL, 'F', '0', '0', 'oa:leave:add', '#'),
(32, '请假审批', 30, 2, '', NULL, 'F', '0', '0', 'oa:leave:approve', '#'),
(33, '请假查询', 30, 3, '', NULL, 'F', '0', '0', 'oa:leave:query', '#'),
(34, '请假修改', 30, 4, '', NULL, 'F', '0', '0', 'oa:leave:edit', '#'),
(35, '请假删除', 30, 5, '', NULL, 'F', '0', '0', 'oa:leave:remove', '#'),
(36, '请假送审', 30, 6, '', NULL, 'F', '0', '0', 'oa:leave:submit', '#'),
(37, '请假撤回', 30, 7, '', NULL, 'F', '0', '0', 'oa:leave:withdraw', '#'),
(40, '报销管理', 1, 4, 'reimbursement', 'oa/reimbursement/index', 'C', '0', '0', 'oa:reimbursement:list', 'money'),
(41, '报销查询', 40, 1, '', NULL, 'F', '0', '0', 'oa:reimbursement:query', '#'),
(42, '报销新增', 40, 2, '', NULL, 'F', '0', '0', 'oa:reimbursement:add', '#'),
(43, '报销修改', 40, 3, '', NULL, 'F', '0', '0', 'oa:reimbursement:edit', '#'),
(44, '报销删除', 40, 4, '', NULL, 'F', '0', '0', 'oa:reimbursement:remove', '#'),
(45, '报销送审', 40, 5, '', NULL, 'F', '0', '0', 'oa:reimbursement:submit', '#'),
(46, '报销撤回', 40, 6, '', NULL, 'F', '0', '0', 'oa:reimbursement:withdraw', '#'),
(47, '报销审批', 40, 7, '', NULL, 'F', '0', '0', 'oa:reimbursement:approve', '#'),
(50, '收文管理', 1, 5, 'incoming-doc', 'oa/incoming-doc/index', 'C', '0', '0', 'oa:incomingDoc:list', 'log'),
(51, '收文查询', 50, 1, '', NULL, 'F', '0', '0', 'oa:incomingDoc:query', '#'),
(52, '收文新增', 50, 2, '', NULL, 'F', '0', '0', 'oa:incomingDoc:add', '#'),
(53, '收文修改', 50, 3, '', NULL, 'F', '0', '0', 'oa:incomingDoc:edit', '#'),
(54, '收文删除', 50, 4, '', NULL, 'F', '0', '0', 'oa:incomingDoc:remove', '#'),
(60, '发文管理', 1, 6, 'outgoing-doc', 'oa/outgoing-doc/index', 'C', '0', '0', 'oa:outgoingDoc:list', 'send'),
(61, '发文查询', 60, 1, '', NULL, 'F', '0', '0', 'oa:outgoingDoc:query', '#'),
(62, '发文新增', 60, 2, '', NULL, 'F', '0', '0', 'oa:outgoingDoc:add', '#'),
(63, '发文修改', 60, 3, '', NULL, 'F', '0', '0', 'oa:outgoingDoc:edit', '#'),
(64, '发文删除', 60, 4, '', NULL, 'F', '0', '0', 'oa:outgoingDoc:remove', '#'),
(70, '流程配置', 1, 7, 'doc-flow', 'oa/doc-flow/index', 'C', '0', '0', 'oa:docFlow:list', 'guide'),
(71, '流程查询', 70, 1, '', NULL, 'F', '0', '0', 'oa:docFlow:query', '#'),
(72, '流程新增', 70, 2, '', NULL, 'F', '0', '0', 'oa:docFlow:add', '#'),
(73, '流程修改', 70, 3, '', NULL, 'F', '0', '0', 'oa:docFlow:edit', '#'),
(74, '流程删除', 70, 4, '', NULL, 'F', '0', '0', 'oa:docFlow:remove', '#'),
(80, '我的待办', 1, 8, 'todo', 'oa/todo/index', 'C', '0', '0', 'oa:todo:list', 'dashboard'),
(81, '待办办理', 80, 1, '', NULL, 'F', '0', '0', 'oa:todo:complete', '#'),
(90, '公文中心', 1, 9, 'doc-center', 'oa/doc-center/index', 'C', '0', '0', 'oa:doc:list', 'document'),
(91, '公文办理', 90, 1, '', NULL, 'F', '0', '0', 'oa:doc:action', '#'),
(92, '公文类型', 90, 2, '', NULL, 'F', '0', '0', 'oa:docType:list', '#'),
(93, '附件查看', 90, 3, '', NULL, 'F', '0', '0', 'oa:document:list', '#'),
(94, '附件上传', 90, 4, '', NULL, 'F', '0', '0', 'oa:document:upload', '#'),
(95, '附件下载', 90, 5, '', NULL, 'F', '0', '0', 'oa:document:query', '#'),
(96, '附件删除', 90, 6, '', NULL, 'F', '0', '0', 'oa:document:remove', '#'),
(200, '公文类型查询', 90, 7, '', NULL, 'F', '0', '0', 'oa:docType:query', '#'),
(201, '公文类型新增', 90, 8, '', NULL, 'F', '0', '0', 'oa:docType:add', '#'),
(202, '公文类型修改', 90, 9, '', NULL, 'F', '0', '0', 'oa:docType:edit', '#'),
(203, '公文类型删除', 90, 10, '', NULL, 'F', '0', '0', 'oa:docType:remove', '#'),
(210, '公文模板查看', 90, 11, '', NULL, 'F', '0', '0', 'oa:docTemplate:list', '#'),
(211, '公文模板查询', 90, 12, '', NULL, 'F', '0', '0', 'oa:docTemplate:query', '#'),
(212, '公文模板新增', 90, 13, '', NULL, 'F', '0', '0', 'oa:docTemplate:add', '#'),
(213, '公文模板修改', 90, 14, '', NULL, 'F', '0', '0', 'oa:docTemplate:edit', '#'),
(214, '公文模板删除', 90, 15, '', NULL, 'F', '0', '0', 'oa:docTemplate:remove', '#'),
(100, '系统管理', 0, 2, 'system', NULL, 'M', '0', '0', NULL, 'setting'),
(103, '用户管理', 100, 1, 'user', 'system/user/index', 'C', '0', '0', 'system:user:list', 'user'),
(104, '用户查询', 103, 1, '', NULL, 'F', '0', '0', 'system:user:query', '#'),
(105, '用户新增', 103, 2, '', NULL, 'F', '0', '0', 'system:user:add', '#'),
(106, '用户修改', 103, 3, '', NULL, 'F', '0', '0', 'system:user:edit', '#'),
(107, '用户删除', 103, 4, '', NULL, 'F', '0', '0', 'system:user:remove', '#'),
(108, '重置密码', 103, 5, '', NULL, 'F', '0', '0', 'system:user:resetPwd', '#'),
(109, '角色管理', 100, 2, 'role', 'system/role/index', 'C', '0', '0', 'system:role:list', 'peoples'),
(111, '角色查询', 109, 1, '', NULL, 'F', '0', '0', 'system:role:query', '#'),
(112, '角色新增', 109, 2, '', NULL, 'F', '0', '0', 'system:role:add', '#'),
(113, '角色修改', 109, 3, '', NULL, 'F', '0', '0', 'system:role:edit', '#'),
(114, '角色删除', 109, 4, '', NULL, 'F', '0', '0', 'system:role:remove', '#'),
(115, '部门管理', 100, 3, 'dept', 'system/dept/index', 'C', '0', '0', 'system:dept:list', 'tree'),
(116, '部门查询', 115, 1, '', NULL, 'F', '0', '0', 'system:dept:query', '#'),
(117, '部门新增', 115, 2, '', NULL, 'F', '0', '0', 'system:dept:add', '#'),
(118, '部门修改', 115, 3, '', NULL, 'F', '0', '0', 'system:dept:edit', '#'),
(119, '部门删除', 115, 4, '', NULL, 'F', '0', '0', 'system:dept:remove', '#'),
(101, '岗位管理', 100, 4, 'post', 'system/post/index', 'C', '0', '0', 'system:post:list', 'postcard'),
(121, '岗位查询', 101, 1, '', NULL, 'F', '0', '0', 'system:post:query', '#'),
(122, '岗位新增', 101, 2, '', NULL, 'F', '0', '0', 'system:post:add', '#'),
(123, '岗位修改', 101, 3, '', NULL, 'F', '0', '0', 'system:post:edit', '#'),
(124, '岗位删除', 101, 4, '', NULL, 'F', '0', '0', 'system:post:remove', '#'),
(102, '操作日志', 100, 5, 'log', 'system/log/index', 'C', '0', '0', 'system:log:list', 'document-checked'),
(126, '日志删除', 102, 1, '', NULL, 'F', '0', '0', 'system:log:remove', '#');

-- 超级管理员：全部权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 20), (1, 21), (1, 22), (1, 23), (1, 24), (1, 30), (1, 31), (1, 32), (1, 33), (1, 34), (1, 35), (1, 36), (1, 37), (1, 40), (1, 41), (1, 42), (1, 43), (1, 44), (1, 45), (1, 46), (1, 47), (1, 50), (1, 51), (1, 52), (1, 53), (1, 54), (1, 60), (1, 61), (1, 62), (1, 63), (1, 64), (1, 70), (1, 71), (1, 72), (1, 73), (1, 74), (1, 80), (1, 81), (1, 90), (1, 91), (1, 92), (1, 93), (1, 94), (1, 95), (1, 96), (1, 200), (1, 201), (1, 202), (1, 203), (1, 210), (1, 211), (1, 212), (1, 213), (1, 214), (1, 100), (1, 103), (1, 104), (1, 105), (1, 106), (1, 107), (1, 108), (1, 109), (1, 111), (1, 112), (1, 113), (1, 114), (1, 115), (1, 116), (1, 117), (1, 118), (1, 119), (1, 101), (1, 121), (1, 122), (1, 123), (1, 124), (1, 102), (1, 126);

-- 普通用户：仅本人数据，可自助维护并送审/撤回，不能审批、不能配置流程与系统管理
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 10), (2, 14), (2, 20), (2, 21), (2, 22), (2, 23), (2, 24), (2, 30), (2, 31), (2, 33), (2, 34), (2, 35), (2, 36), (2, 37), (2, 40), (2, 41), (2, 42), (2, 43), (2, 44), (2, 45), (2, 46), (2, 50), (2, 51), (2, 60), (2, 61), (2, 80), (2, 81), (2, 90), (2, 91), (2, 93), (2, 94), (2, 95), (2, 96);

-- 部门经理：在普通用户之上增加审批权、公文拟制与修改、流程查看、岗位查看
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 1), (3, 10), (3, 14), (3, 20), (3, 21), (3, 22), (3, 23), (3, 24), (3, 30), (3, 31), (3, 32), (3, 33), (3, 34), (3, 35), (3, 36), (3, 37), (3, 40), (3, 41), (3, 42), (3, 43), (3, 44), (3, 45), (3, 46), (3, 47), (3, 50), (3, 51), (3, 52), (3, 53), (3, 60), (3, 61), (3, 62), (3, 63), (3, 70), (3, 71), (3, 80), (3, 81), (3, 90), (3, 91), (3, 92), (3, 93), (3, 94), (3, 95), (3, 96), (3, 101), (3, 121), (3, 200);

-- 默认公告
INSERT IGNORE INTO oa_announcement (ann_id, ann_title, ann_type, ann_content, ann_level, is_public, status, create_by, create_time) VALUES
(1, '欢迎使用OA办公系统', '2', '欢迎使用OA办公系统，本系统提供公告管理、日程安排、请假审批、报销管理、公文收发等功能。', '1', '1', '1', 'admin', NOW()),
(2, '关于系统升级的通知', '1', '系统将于本周六进行升级维护，届时将暂停服务，请各位同事提前做好工作安排。', '2', '0', '1', 'admin', NOW());

-- 默认日程
INSERT IGNORE INTO oa_schedule (schedule_id, schedule_title, schedule_type, start_time, end_time, schedule_content, status, user_id, create_by, create_time) VALUES
(1, '周一例会', '3', DATE_FORMAT(DATE_ADD(NOW(), INTERVAL 1 DAY), '%Y-%m-%d 09:00:00'), DATE_FORMAT(DATE_ADD(NOW(), INTERVAL 1 DAY), '%Y-%m-%d 09:30:00'), '讨论本周工作计划', '0', 1, 'admin', NOW());

-- 公文类型
INSERT INTO oa_doc_type (type_name, type_code, sort_order, status) VALUES
('通知', 'TZ', 1, '0'), ('通报', 'TB', 2, '0'), ('决定', 'JD', 3, '0'),
('报告', 'BG', 4, '0'), ('请示', 'QS', 5, '0'), ('批复', 'PF', 6, '0'),
('函', 'HAN', 7, '0'), ('纪要', 'JY', 8, '0');

-- ============================================================
-- 完成
-- ============================================================