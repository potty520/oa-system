PRAGMA foreign_keys = OFF;

CREATE TABLE IF NOT EXISTS sys_dept (
  dept_id INTEGER PRIMARY KEY AUTOINCREMENT,
  parent_id INTEGER DEFAULT 0,
  ancestors TEXT DEFAULT '',
  dept_name TEXT NOT NULL,
  order_num INTEGER DEFAULT 0,
  leader TEXT DEFAULT '',
  phone TEXT DEFAULT '',
  email TEXT DEFAULT '',
  status TEXT DEFAULT '0',
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS sys_user (
  user_id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE,
  password TEXT NOT NULL,
  nickname TEXT DEFAULT '',
  email TEXT DEFAULT '',
  phone TEXT DEFAULT '',
  avatar TEXT DEFAULT '',
  sex TEXT DEFAULT '0',
  dept_id INTEGER,
  status TEXT DEFAULT '0',
  login_ip TEXT DEFAULT '',
  login_date TEXT,
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS sys_role (
  role_id INTEGER PRIMARY KEY AUTOINCREMENT,
  role_name TEXT NOT NULL,
  role_key TEXT NOT NULL,
  role_sort INTEGER DEFAULT 0,
  status TEXT DEFAULT '0',
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS sys_menu (
  menu_id INTEGER PRIMARY KEY AUTOINCREMENT,
  menu_name TEXT NOT NULL,
  parent_id INTEGER DEFAULT 0,
  order_num INTEGER DEFAULT 0,
  path TEXT DEFAULT '',
  component TEXT,
  menu_type TEXT DEFAULT '',
  visible TEXT DEFAULT '0',
  status TEXT DEFAULT '0',
  perms TEXT,
  icon TEXT DEFAULT '#',
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id INTEGER NOT NULL,
  role_id INTEGER NOT NULL,
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id INTEGER NOT NULL,
  menu_id INTEGER NOT NULL,
  PRIMARY KEY (role_id, menu_id)
);

CREATE TABLE IF NOT EXISTS oa_announcement (
  ann_id INTEGER PRIMARY KEY AUTOINCREMENT,
  ann_title TEXT NOT NULL,
  ann_type TEXT DEFAULT '1',
  ann_content TEXT,
  ann_level TEXT DEFAULT '1',
  attachment_url TEXT DEFAULT '',
  publish_start_time TEXT,
  publish_end_time TEXT,
  is_top TEXT DEFAULT '0',
  is_public TEXT DEFAULT '0',
  view_count INTEGER DEFAULT 0,
  status TEXT DEFAULT '0',
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS oa_schedule (
  schedule_id INTEGER PRIMARY KEY AUTOINCREMENT,
  schedule_title TEXT NOT NULL,
  schedule_type TEXT DEFAULT '1',
  schedule_color TEXT DEFAULT '',
  start_time TEXT,
  end_time TEXT,
  schedule_content TEXT,
  location TEXT DEFAULT '',
  remind_flag TEXT DEFAULT '0',
  remind_time TEXT,
  status TEXT DEFAULT '0',
  user_id INTEGER,
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS oa_leave (
  leave_id INTEGER PRIMARY KEY AUTOINCREMENT,
  leave_type TEXT DEFAULT '1',
  start_time TEXT NOT NULL,
  end_time TEXT NOT NULL,
  leave_days REAL NOT NULL DEFAULT 0,
  leave_reason TEXT,
  apply_time TEXT,
  approval_status TEXT DEFAULT '0',
  current_step INTEGER DEFAULT 0,
  user_id INTEGER,
  user_name TEXT DEFAULT '',
  dept_id INTEGER,
  dept_name TEXT DEFAULT '',
  approve_by TEXT DEFAULT '',
  approve_time TEXT,
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS oa_reimbursement (
  reimb_id INTEGER PRIMARY KEY AUTOINCREMENT,
  reimb_type TEXT DEFAULT '1',
  total_amount REAL NOT NULL DEFAULT 0,
  reimb_reason TEXT,
  apply_time TEXT,
  approval_status TEXT DEFAULT '0',
  current_step INTEGER DEFAULT 0,
  payment_status TEXT DEFAULT '0',
  payment_time TEXT,
  approve_by TEXT DEFAULT '',
  approve_time TEXT,
  approve_remark TEXT DEFAULT '',
  user_id INTEGER,
  user_name TEXT DEFAULT '',
  dept_id INTEGER,
  dept_name TEXT DEFAULT '',
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS oa_reimbursement_detail (
  detail_id INTEGER PRIMARY KEY AUTOINCREMENT,
  reimb_id INTEGER NOT NULL,
  expense_type TEXT DEFAULT '',
  expense_date TEXT,
  expense_amount REAL NOT NULL DEFAULT 0,
  expense_desc TEXT DEFAULT '',
  receipt_url TEXT DEFAULT '',
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS oa_reimbursement_approve_log (
  log_id INTEGER PRIMARY KEY AUTOINCREMENT,
  reimb_id INTEGER NOT NULL,
  approve_result TEXT,
  approve_remark TEXT DEFAULT '',
  approve_by TEXT DEFAULT '',
  approve_time TEXT,
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS oa_incoming_doc (
  doc_id INTEGER PRIMARY KEY AUTOINCREMENT,
  doc_number TEXT NOT NULL UNIQUE,
  doc_title TEXT NOT NULL,
  doc_type_id INTEGER,
  source_unit TEXT DEFAULT '',
  receive_date TEXT,
  doc_level TEXT DEFAULT '1',
  doc_secret TEXT DEFAULT '1',
  summary TEXT,
  attachment_url TEXT DEFAULT '',
  handler_id INTEGER,
  handler_name TEXT DEFAULT '',
  status TEXT DEFAULT '0',
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS oa_outgoing_doc (
  doc_id INTEGER PRIMARY KEY AUTOINCREMENT,
  doc_number TEXT NOT NULL UNIQUE,
  doc_title TEXT NOT NULL,
  doc_type_id INTEGER,
  draft_unit TEXT DEFAULT '',
  draft_date TEXT,
  doc_level TEXT DEFAULT '1',
  doc_secret TEXT DEFAULT '1',
  main_send TEXT DEFAULT '',
  copy_send TEXT DEFAULT '',
  summary TEXT,
  attachment_url TEXT DEFAULT '',
  sign_user_id INTEGER,
  sign_user_name TEXT DEFAULT '',
  status TEXT DEFAULT '0',
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS oa_doc_flow (
  flow_id INTEGER PRIMARY KEY AUTOINCREMENT,
  flow_name TEXT NOT NULL,
  flow_type TEXT DEFAULT '1',
  description TEXT DEFAULT '',
  status TEXT DEFAULT '0',
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE TABLE IF NOT EXISTS oa_doc_flow_step (
  step_id INTEGER PRIMARY KEY AUTOINCREMENT,
  flow_id INTEGER NOT NULL,
  step_name TEXT DEFAULT '',
  step_order INTEGER DEFAULT 0,
  role_id INTEGER,
  is_sign TEXT DEFAULT '1',
  create_by TEXT DEFAULT '',
  create_time TEXT,
  update_by TEXT DEFAULT '',
  update_time TEXT,
  del_flag TEXT DEFAULT '0',
  remark TEXT
);

CREATE INDEX IF NOT EXISTS idx_dept_parent ON sys_dept(parent_id);
CREATE INDEX IF NOT EXISTS idx_user_dept ON sys_user(dept_id);
CREATE INDEX IF NOT EXISTS idx_schedule_user ON oa_schedule(user_id);
CREATE INDEX IF NOT EXISTS idx_leave_user ON oa_leave(user_id);
CREATE INDEX IF NOT EXISTS idx_reimb_user ON oa_reimbursement(user_id);
CREATE INDEX IF NOT EXISTS idx_reimb_detail ON oa_reimbursement_detail(reimb_id);
CREATE INDEX IF NOT EXISTS idx_doc_flow_step ON oa_doc_flow_step(flow_id);

INSERT OR IGNORE INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status) VALUES
(100, 0, '0', '总公司', 1, '超级管理员', '13800000000', 'admin@example.com', '0'),
(101, 100, '0,100', '行政部', 1, '张三', '13800000001', 'hr@example.com', '0'),
(102, 100, '0,100', '财务部', 2, '李四', '13800000002', 'finance@example.com', '0'),
(103, 100, '0,100', '技术部', 3, '王五', '13800000003', 'tech@example.com', '0');

INSERT OR IGNORE INTO sys_user (user_id, username, password, nickname, dept_id, status, create_time) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 100, '0', datetime('now', 'localtime')),
(2, 'zhangsan', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', 103, '0', datetime('now', 'localtime')),
(3, 'lisi', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李四', 102, '0', datetime('now', 'localtime'));

INSERT OR IGNORE INTO sys_role (role_id, role_name, role_key, role_sort, status) VALUES
(1, '超级管理员', 'admin', 1, '0'),
(2, '普通用户', 'common', 2, '0'),
(3, '部门经理', 'manager', 3, '0');

INSERT OR IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2), (3, 2);

INSERT OR IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon) VALUES
(1, 'OA办公', 0, 1, 'oa', NULL, 'M', '0', '0', NULL, 'documentation'),
(10, '公告管理', 1, 1, 'announcement', 'oa/announcement/index', 'C', '0', '0', 'oa:announcement:list', 'message'),
(11, '公告新增', 10, 1, '', NULL, 'F', '0', '0', 'oa:announcement:add', '#'),
(12, '公告修改', 10, 2, '', NULL, 'F', '0', '0', 'oa:announcement:edit', '#'),
(13, '公告删除', 10, 3, '', NULL, 'F', '0', '0', 'oa:announcement:remove', '#'),
(20, '日程管理', 1, 2, 'schedule', 'oa/schedule/index', 'C', '0', '0', 'oa:schedule:list', 'date'),
(30, '请假管理', 1, 3, 'leave', 'oa/leave/index', 'C', '0', '0', 'oa:leave:list', 'tree-table'),
(31, '请假提交', 30, 1, '', NULL, 'F', '0', '0', 'oa:leave:add', '#'),
(32, '请假审批', 30, 2, '', NULL, 'F', '0', '0', 'oa:leave:approve', '#'),
(40, '报销管理', 1, 4, 'reimbursement', 'oa/reimbursement/index', 'C', '0', '0', 'oa:reimbursement:list', 'money'),
(50, '收文管理', 1, 5, 'incoming-doc', 'oa/incoming-doc/index', 'C', '0', '0', 'oa:incomingDoc:list', 'log'),
(60, '发文管理', 1, 6, 'outgoing-doc', 'oa/outgoing-doc/index', 'C', '0', '0', 'oa:outgoingDoc:list', 'send'),
(70, '流程配置', 1, 7, 'doc-flow', 'oa/doc-flow/index', 'C', '0', '0', 'oa:docFlow:list', 'guide'),
(80, '我的待办', 1, 8, 'todo', 'oa/todo/index', 'C', '0', '0', 'oa:todo:list', 'dashboard');

INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 10), (1, 11), (1, 12), (1, 13), (1, 20), (1, 30), (1, 31), (1, 32), (1, 40), (1, 50), (1, 60), (1, 70), (1, 80),
(2, 1), (2, 10), (2, 20), (2, 30), (2, 31), (2, 40), (2, 50), (2, 60), (2, 80);

INSERT OR IGNORE INTO oa_announcement (ann_id, ann_title, ann_type, ann_content, ann_level, publish_start_time, publish_end_time, is_public, status, create_by, create_time) VALUES
(1, '欢迎使用OA办公系统', '2', '欢迎使用OA办公系统，本系统提供公告、日程、请假、报销、公文和待办功能。', '1', datetime('now', 'localtime'), datetime('now', '+30 day', 'localtime'), '1', '1', 'admin', datetime('now', 'localtime')),
(2, '关于系统升级的通知', '1', '系统将于本周六进行升级维护，请提前安排工作。', '2', datetime('now', 'localtime'), datetime('now', '+30 day', 'localtime'), '0', '1', 'admin', datetime('now', 'localtime'));

INSERT OR IGNORE INTO oa_schedule (schedule_id, schedule_title, schedule_type, start_time, end_time, schedule_content, status, user_id, create_by, create_time) VALUES
(1, '周一例会', '2', datetime('now', '+1 day', 'localtime'), datetime('now', '+1 day', '+30 minute', 'localtime'), '讨论本周工作计划', '0', 1, 'admin', datetime('now', 'localtime'));