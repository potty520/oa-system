#!/usr/bin/env python3
"""Generate database-specific schema scripts from the MySQL source schema.

The application schema is defined once in sql/oa_init.sql for MySQL. This
script translates the active tables and system seed data into the SQL syntax
used by every supported database. It intentionally keeps comments out of the
generated files so the output stays portable and easy to diff.
"""
from __future__ import annotations

import hashlib
import pathlib
import re
from dataclasses import dataclass, field

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "sql" / "oa_init.sql"
OUTPUT = ROOT / "ruoyi-admin" / "src" / "main" / "resources" / "db"
ACTIVE_TABLES = {
    "sys_dept", "sys_user", "sys_role", "sys_menu", "sys_user_role", "sys_role_menu",
    "oa_announcement", "oa_schedule", "oa_leave", "oa_reimbursement",
    "oa_reimbursement_detail", "oa_reimbursement_approve_log", "oa_incoming_doc",
    "oa_outgoing_doc", "oa_doc_flow", "oa_doc_flow_step",
    "sys_post", "sys_user_post", "sys_role_dept", "sys_oper_log",
    "oa_document", "oa_doc_type", "oa_doc_template", "oa_doc_approval", "oa_doc_circulation", "oa_doc_reminder", "oa_doc_delegate", "oa_doc_archive",
}


@dataclass
class Column:
    name: str
    kind: str
    args: str = ""
    not_null: bool = False
    default: str | None = None
    auto: bool = False


@dataclass
class Table:
    name: str
    columns: list[Column] = field(default_factory=list)
    primary_key: list[str] = field(default_factory=list)
    unique_keys: list[tuple[str, list[str]]] = field(default_factory=list)
    indexes: list[tuple[str, list[str]]] = field(default_factory=list)


def parse_type(raw: str) -> tuple[str, str]:
    match = re.match(r"([A-Z]+)(?:\(([^)]*)\))?", raw.upper())
    if not match:
        raise ValueError(f"Unsupported column type: {raw}")
    return match.group(1), match.group(2) or ""


def parse_schema() -> list[Table]:
    sql = SOURCE.read_text(encoding="utf-8")
    tables: list[Table] = []
    for table_match in re.finditer(r"(?is)CREATE\s+TABLE\s+`?(\w+)`?\s*\((.*?)\)\s*ENGINE\s*=\s*InnoDB", sql):
        name = table_match.group(1)
        if name not in ACTIVE_TABLES:
            continue
        table = Table(name=name)
        for raw_line in table_match.group(2).splitlines():
            line = raw_line.strip().rstrip(",")
            if not line:
                continue
            primary = re.match(r"(?is)PRIMARY\s+KEY\s*\(([^)]+)\)", line)
            if primary:
                table.primary_key = [item.strip().strip('`') for item in primary.group(1).split(",")]
                continue
            unique = re.match(r"(?is)UNIQUE(?:\s+KEY)?\s*(\w+)?\s*\(([^)]+)\)", line)
            if unique:
                key_name = unique.group(1) or f"uk_{name}_{len(table.unique_keys)}"
                table.unique_keys.append((key_name, [item.strip().strip('`') for item in unique.group(2).split(",")]))
                continue
            index = re.match(r"(?is)(?:KEY|INDEX)\s+(\w+)\s*\(([^)]+)\)", line)
            if index:
                table.indexes.append((index.group(1), [item.strip().strip('`') for item in index.group(2).split(",")]))
                continue
            column = re.match(r"(?is)`?(\w+)`?\s+([A-Z]+(?:\([^)]*\))?)(.*)$", line)
            if not column:
                continue
            kind, args = parse_type(column.group(2))
            rest = column.group(3)
            rest_wo_comment = re.sub(r"(?is)\s+COMMENT\s+'(?:''|[^'])*'", "", rest)
            default_match = re.search(r"(?is)\bDEFAULT\s+(NULL|'(?:''|[^'])*'|[-+]?\d+(?:\.\d+)?|[A-Z_]+(?:\(\))?)", rest_wo_comment)
            default = default_match.group(1) if default_match else None
            table.columns.append(Column(
                name=column.group(1),
                kind=kind,
                args=args,
                not_null="NOT NULL" in rest_wo_comment.upper(),
                default=default,
                auto="AUTO_INCREMENT" in rest_wo_comment.upper(),
            ))
        tables.append(table)
    return tables


TABLES = parse_schema()
TABLES_BY_NAME = {table.name: table for table in TABLES}
PASSWORD_HASH = "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2"
NOW = "CURRENT_TIMESTAMP"

MENU_ROWS: list[tuple] = [
    (1, "OA办公", 0, 1, "oa", None, "M", "0", "0", None, "documentation"),
    (10, "公告管理", 1, 1, "announcement", "oa/announcement/index", "C", "0", "0", "oa:announcement:list", "message"),
    (11, "公告新增", 10, 1, "", None, "F", "0", "0", "oa:announcement:add", "#"),
    (12, "公告修改", 10, 2, "", None, "F", "0", "0", "oa:announcement:edit", "#"),
    (13, "公告删除", 10, 3, "", None, "F", "0", "0", "oa:announcement:remove", "#"),
    (14, "公告查询", 10, 4, "", None, "F", "0", "0", "oa:announcement:query", "#"),
    (20, "日程管理", 1, 2, "schedule", "oa/schedule/index", "C", "0", "0", "oa:schedule:list", "date"),
    (21, "日程查询", 20, 1, "", None, "F", "0", "0", "oa:schedule:query", "#"),
    (22, "日程新增", 20, 2, "", None, "F", "0", "0", "oa:schedule:add", "#"),
    (23, "日程修改", 20, 3, "", None, "F", "0", "0", "oa:schedule:edit", "#"),
    (24, "日程删除", 20, 4, "", None, "F", "0", "0", "oa:schedule:remove", "#"),
    (30, "请假管理", 1, 3, "leave", "oa/leave/index", "C", "0", "0", "oa:leave:list", "tree-table"),
    (31, "请假提交", 30, 1, "", None, "F", "0", "0", "oa:leave:add", "#"),
    (32, "请假审批", 30, 2, "", None, "F", "0", "0", "oa:leave:approve", "#"),
    (33, "请假查询", 30, 3, "", None, "F", "0", "0", "oa:leave:query", "#"),
    (34, "请假修改", 30, 4, "", None, "F", "0", "0", "oa:leave:edit", "#"),
    (35, "请假删除", 30, 5, "", None, "F", "0", "0", "oa:leave:remove", "#"),
    (36, "请假送审", 30, 6, "", None, "F", "0", "0", "oa:leave:submit", "#"),
    (37, "请假撤回", 30, 7, "", None, "F", "0", "0", "oa:leave:withdraw", "#"),
    (40, "报销管理", 1, 4, "reimbursement", "oa/reimbursement/index", "C", "0", "0", "oa:reimbursement:list", "money"),
    (41, "报销查询", 40, 1, "", None, "F", "0", "0", "oa:reimbursement:query", "#"),
    (42, "报销新增", 40, 2, "", None, "F", "0", "0", "oa:reimbursement:add", "#"),
    (43, "报销修改", 40, 3, "", None, "F", "0", "0", "oa:reimbursement:edit", "#"),
    (44, "报销删除", 40, 4, "", None, "F", "0", "0", "oa:reimbursement:remove", "#"),
    (45, "报销送审", 40, 5, "", None, "F", "0", "0", "oa:reimbursement:submit", "#"),
    (46, "报销撤回", 40, 6, "", None, "F", "0", "0", "oa:reimbursement:withdraw", "#"),
    (47, "报销审批", 40, 7, "", None, "F", "0", "0", "oa:reimbursement:approve", "#"),
    (50, "收文管理", 1, 5, "incoming-doc", "oa/incoming-doc/index", "C", "0", "0", "oa:incomingDoc:list", "log"),
    (51, "收文查询", 50, 1, "", None, "F", "0", "0", "oa:incomingDoc:query", "#"),
    (52, "收文新增", 50, 2, "", None, "F", "0", "0", "oa:incomingDoc:add", "#"),
    (53, "收文修改", 50, 3, "", None, "F", "0", "0", "oa:incomingDoc:edit", "#"),
    (54, "收文删除", 50, 4, "", None, "F", "0", "0", "oa:incomingDoc:remove", "#"),
    (60, "发文管理", 1, 6, "outgoing-doc", "oa/outgoing-doc/index", "C", "0", "0", "oa:outgoingDoc:list", "send"),
    (61, "发文查询", 60, 1, "", None, "F", "0", "0", "oa:outgoingDoc:query", "#"),
    (62, "发文新增", 60, 2, "", None, "F", "0", "0", "oa:outgoingDoc:add", "#"),
    (63, "发文修改", 60, 3, "", None, "F", "0", "0", "oa:outgoingDoc:edit", "#"),
    (64, "发文删除", 60, 4, "", None, "F", "0", "0", "oa:outgoingDoc:remove", "#"),
    (70, "流程配置", 1, 7, "doc-flow", "oa/doc-flow/index", "C", "0", "0", "oa:docFlow:list", "guide"),
    (71, "流程查询", 70, 1, "", None, "F", "0", "0", "oa:docFlow:query", "#"),
    (72, "流程新增", 70, 2, "", None, "F", "0", "0", "oa:docFlow:add", "#"),
    (73, "流程修改", 70, 3, "", None, "F", "0", "0", "oa:docFlow:edit", "#"),
    (74, "流程删除", 70, 4, "", None, "F", "0", "0", "oa:docFlow:remove", "#"),
    (80, "我的待办", 1, 8, "todo", "oa/todo/index", "C", "0", "0", "oa:todo:list", "dashboard"),
    (81, "待办办理", 80, 1, "", None, "F", "0", "0", "oa:todo:complete", "#"),
    (90, "公文中心", 1, 9, "doc-center", "oa/doc-center/index", "C", "0", "0", "oa:doc:list", "document"),
    (91, "公文办理", 90, 1, "", None, "F", "0", "0", "oa:doc:action", "#"),
    (92, "公文类型", 90, 2, "", None, "F", "0", "0", "oa:docType:list", "#"),
    (93, "附件查看", 90, 3, "", None, "F", "0", "0", "oa:document:list", "#"),
    (94, "附件上传", 90, 4, "", None, "F", "0", "0", "oa:document:upload", "#"),
    (95, "附件下载", 90, 5, "", None, "F", "0", "0", "oa:document:query", "#"),
    (96, "附件删除", 90, 6, "", None, "F", "0", "0", "oa:document:remove", "#"),
    (200, "公文类型查询", 90, 7, "", None, "F", "0", "0", "oa:docType:query", "#"),
    (201, "公文类型新增", 90, 8, "", None, "F", "0", "0", "oa:docType:add", "#"),
    (202, "公文类型修改", 90, 9, "", None, "F", "0", "0", "oa:docType:edit", "#"),
    (203, "公文类型删除", 90, 10, "", None, "F", "0", "0", "oa:docType:remove", "#"),
    (210, "公文模板查看", 90, 11, "", None, "F", "0", "0", "oa:docTemplate:list", "#"),
    (211, "公文模板查询", 90, 12, "", None, "F", "0", "0", "oa:docTemplate:query", "#"),
    (212, "公文模板新增", 90, 13, "", None, "F", "0", "0", "oa:docTemplate:add", "#"),
    (213, "公文模板修改", 90, 14, "", None, "F", "0", "0", "oa:docTemplate:edit", "#"),
    (214, "公文模板删除", 90, 15, "", None, "F", "0", "0", "oa:docTemplate:remove", "#"),
    (100, "系统管理", 0, 2, "system", None, "M", "0", "0", None, "setting"),
    (103, "用户管理", 100, 1, "user", "system/user/index", "C", "0", "0", "system:user:list", "user"),
    (104, "用户查询", 103, 1, "", None, "F", "0", "0", "system:user:query", "#"),
    (105, "用户新增", 103, 2, "", None, "F", "0", "0", "system:user:add", "#"),
    (106, "用户修改", 103, 3, "", None, "F", "0", "0", "system:user:edit", "#"),
    (107, "用户删除", 103, 4, "", None, "F", "0", "0", "system:user:remove", "#"),
    (108, "重置密码", 103, 5, "", None, "F", "0", "0", "system:user:resetPwd", "#"),
    (109, "角色管理", 100, 2, "role", "system/role/index", "C", "0", "0", "system:role:list", "peoples"),
    (111, "角色查询", 109, 1, "", None, "F", "0", "0", "system:role:query", "#"),
    (112, "角色新增", 109, 2, "", None, "F", "0", "0", "system:role:add", "#"),
    (113, "角色修改", 109, 3, "", None, "F", "0", "0", "system:role:edit", "#"),
    (114, "角色删除", 109, 4, "", None, "F", "0", "0", "system:role:remove", "#"),
    (115, "部门管理", 100, 3, "dept", "system/dept/index", "C", "0", "0", "system:dept:list", "tree"),
    (116, "部门查询", 115, 1, "", None, "F", "0", "0", "system:dept:query", "#"),
    (117, "部门新增", 115, 2, "", None, "F", "0", "0", "system:dept:add", "#"),
    (118, "部门修改", 115, 3, "", None, "F", "0", "0", "system:dept:edit", "#"),
    (119, "部门删除", 115, 4, "", None, "F", "0", "0", "system:dept:remove", "#"),
    (101, "岗位管理", 100, 4, "post", "system/post/index", "C", "0", "0", "system:post:list", "postcard"),
    (121, "岗位查询", 101, 1, "", None, "F", "0", "0", "system:post:query", "#"),
    (122, "岗位新增", 101, 2, "", None, "F", "0", "0", "system:post:add", "#"),
    (123, "岗位修改", 101, 3, "", None, "F", "0", "0", "system:post:edit", "#"),
    (124, "岗位删除", 101, 4, "", None, "F", "0", "0", "system:post:remove", "#"),
    (102, "操作日志", 100, 5, "log", "system/log/index", "C", "0", "0", "system:log:list", "document-checked"),
    (126, "日志删除", 102, 1, "", None, "F", "0", "0", "system:log:remove", "#"),
]
ALL_MENU_IDS = [row[0] for row in MENU_ROWS]
# 普通用户：仅本人数据。可自助维护自己的日程/请假/报销并送审撤回，但不能审批、不能配置流程和系统管理。
COMMON_ROLE_MENUS = [
    1, 10, 14, 20, 21, 22, 23, 24, 30, 31, 33, 34, 35, 36, 37,
    40, 41, 42, 43, 44, 45, 46, 50, 51, 60, 61, 80, 81, 90, 91, 93, 94, 95, 96,
]
# 部门经理：在普通用户之上增加审批权、公文拟制与修改、流程查看、岗位查看。
MANAGER_ROLE_MENUS = sorted(set(COMMON_ROLE_MENUS) | {32, 47, 52, 53, 62, 63, 70, 71, 92, 101, 121, 200})

SEEDS: dict[str, tuple[list[str], list[tuple]]] = {
    "sys_dept": (["dept_id", "parent_id", "ancestors", "dept_name", "order_num", "leader", "phone", "email", "status"], [
        (100, 0, "0", "总公司", 1, "超级管理员", "13800000000", "admin@example.com", "0"),
        (101, 100, "0,100", "行政部", 1, "张三", "13800000001", "hr@example.com", "0"),
        (102, 100, "0,100", "财务部", 2, "李四", "13800000002", "finance@example.com", "0"),
        (103, 100, "0,100", "技术部", 3, "王五", "13800000003", "tech@example.com", "0"),
    ]),
    "sys_user": (["user_id", "username", "password", "nickname", "dept_id", "status", "create_time"], [
        (1, "admin", PASSWORD_HASH, "超级管理员", 100, "0", NOW),
        (2, "zhangsan", PASSWORD_HASH, "张三", 103, "0", NOW),
        (3, "lisi", PASSWORD_HASH, "李四", 102, "0", NOW),
    ]),
    "sys_role": (["role_id", "role_name", "role_key", "role_sort", "data_scope", "status"], [
        (1, "超级管理员", "admin", 1, "1", "0"),
        (2, "普通用户", "common", 2, "5", "0"),
        (3, "部门经理", "manager", 3, "4", "0"),
    ]),
    "sys_user_role": (["user_id", "role_id"], [(1, 1), (2, 2), (3, 2)]),
    "sys_post": (["post_id", "post_code", "post_name", "post_sort", "status"], [
        (1, "admin", "系统管理员", 1, "0"),
        (2, "manager", "部门经理", 2, "0"),
        (3, "staff", "普通员工", 3, "0"),
    ]),
    "sys_user_post": (["user_id", "post_id"], [(1, 1), (2, 3), (3, 2)]),
    "sys_menu": (["menu_id", "menu_name", "parent_id", "order_num", "path", "component",
                  "menu_type", "visible", "status", "perms", "icon"], MENU_ROWS),
    "sys_role_menu": (["role_id", "menu_id"], (
        [(1, menu_id) for menu_id in ALL_MENU_IDS]
        + [(2, menu_id) for menu_id in COMMON_ROLE_MENUS]
        + [(3, menu_id) for menu_id in MANAGER_ROLE_MENUS]
    )),
    "oa_doc_type": (["doc_type_id", "type_name", "type_code", "parent_id", "sort_order", "status"], [
        (1000, "通知", "TZ", 0, 1, "0"), (1001, "通报", "TB", 0, 2, "0"),
        (1002, "决定", "JD", 0, 3, "0"), (1003, "报告", "BG", 0, 4, "0"),
        (1004, "请示", "QS", 0, 5, "0"), (1005, "批复", "PF", 0, 6, "0"),
        (1006, "函", "HAN", 0, 7, "0"), (1007, "纪要", "JY", 0, 8, "0"),
    ]),
}


def type_for(col: Column, dialect: str) -> str:
    kind = col.kind
    args = col.args
    if dialect in {"mysql", "mariadb"}:
        return f"{kind}({args})" if args else kind
    if dialect in {"postgresql", "h2", "kingbase"}:
        if kind == "BIGINT": return "BIGINT"
        if kind == "INT": return "INTEGER"
        if kind == "DECIMAL": return f"NUMERIC({args})"
        if kind == "DATETIME": return "TIMESTAMP"
        if kind == "DATE": return "DATE"
        if kind == "TEXT": return "TEXT"
        if kind in {"VARCHAR", "CHAR"}: return f"{kind}({args})"
    if dialect == "h2":
        if kind == "BIGINT": return "BIGINT"
        if kind == "INT": return "INTEGER"
        if kind == "DECIMAL": return f"NUMERIC({args})"
        if kind == "DATETIME": return "TIMESTAMP"
        if kind == "DATE": return "DATE"
        if kind == "TEXT": return "CLOB"
        if kind in {"VARCHAR", "CHAR"}: return f"{kind}({args})"
    if dialect == "sqlite":
        if kind in {"BIGINT", "INT"}: return "INTEGER"
        if kind == "DECIMAL": return "NUMERIC"
        if kind == "DATE": return "TEXT"
        if kind == "DATETIME": return "TEXT"
        return "TEXT"
    if dialect == "sqlserver":
        if kind == "BIGINT": return "BIGINT"
        if kind == "INT": return "INT"
        if kind == "DECIMAL": return f"DECIMAL({args})"
        if kind == "DATETIME": return "DATETIME2"
        if kind == "DATE": return "DATE"
        if kind == "TEXT": return "NVARCHAR(MAX)"
        if kind == "VARCHAR": return f"NVARCHAR({args})"
        if kind == "CHAR": return f"NCHAR({args})"
    if dialect in {"oracle", "dm"}:
        if kind == "BIGINT": return "NUMBER(19)"
        if kind == "INT": return "NUMBER(10)"
        if kind == "DECIMAL": return f"NUMBER({args})"
        if kind == "DATETIME": return "TIMESTAMP"
        if kind == "DATE": return "DATE"
        if kind == "TEXT": return "CLOB"
        if kind == "VARCHAR": return f"VARCHAR2({args})"
        if kind == "CHAR": return f"CHAR({args})"
    raise ValueError(f"Unsupported type {kind} for {dialect}")


def identity_type(col: Column, dialect: str) -> str:
    base = {
        "mysql": f"{col.kind}({col.args})" if col.args else col.kind,
        "mariadb": f"{col.kind}({col.args})" if col.args else col.kind,
        "postgresql": "BIGINT",
        "kingbase": "BIGINT",
        "h2": "BIGINT",
        "sqlserver": "BIGINT",
        "oracle": "NUMBER(19)",
        "dm": "BIGINT",
    }[dialect]
    if dialect in {"mysql", "mariadb"}:
        return base
    if dialect in {"postgresql", "h2", "kingbase"}:
        return base + " GENERATED BY DEFAULT AS IDENTITY (START WITH 1000)"
    if dialect == "sqlserver":
        return base + " IDENTITY(1000,1)"
    if dialect == "oracle":
        return base + " GENERATED BY DEFAULT AS IDENTITY (START WITH 1000)"
    if dialect == "dm":
        return base + " IDENTITY(1000,1)"
    return base


def render_default(value: str | None, dialect: str) -> str:
    if value is None or value.upper() == "NULL":
        return ""
    if value == "'":
        return " DEFAULT ' '" if dialect in {"oracle", "dm"} else ""
    if value.startswith("'") and dialect in {"oracle", "dm"} and value == "''":
        return " DEFAULT ' '"
    return f" DEFAULT {value}"


def render_column(col: Column, dialect: str, inline_pk: bool = False) -> str:
    if col.auto:
        if dialect == "sqlite":
            text = "INTEGER PRIMARY KEY AUTOINCREMENT"
        else:
            text = identity_type(col, dialect)
            if dialect in {"mysql", "mariadb"}:
                text += " AUTO_INCREMENT"
    else:
        text = type_for(col, dialect)
    if col.not_null and not (col.auto and dialect == "sqlite"):
        text += " NOT NULL"
    if not col.auto:
        text += render_default(col.default, dialect)
    return f"  {col.name} {text}"


def render_table(table: Table, dialect: str) -> str:
    auto_col = next((col for col in table.columns if col.auto), None)
    lines = []
    for col in table.columns:
        inline = dialect == "sqlite" and col is auto_col
        lines.append(render_column(col, dialect, inline))
    if table.primary_key and not (dialect == "sqlite" and auto_col):
        lines.append(f"  PRIMARY KEY ({', '.join(table.primary_key)})")
    for name, cols in table.unique_keys:
        if dialect in {"oracle", "dm"}:
            digest = hashlib.md5((table.name + "." + name + "," + ",".join(cols)).encode("utf-8")).hexdigest()[:8]
            constraint_name = f"uk_{table.name[:10]}_{digest}"
        else:
            constraint_name = f"uk_{table.name}_{name}"
        lines.append(f"  CONSTRAINT {constraint_name} UNIQUE ({', '.join(cols)})")
    if dialect in {"mysql", "mariadb"}:
        for name, cols in table.indexes:
            lines.append(f"  KEY idx_{table.name}_{name} ({', '.join(cols)})")
    optional = ""
    if dialect in {"mysql", "mariadb"} and auto_col:
        optional = " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1000"
    if_not_exists = "IF NOT EXISTS " if dialect in {"mysql", "mariadb", "postgresql", "sqlite", "h2", "kingbase"} else ""
    ddl = f"CREATE TABLE {if_not_exists}{table.name} (\n" + ",\n".join(lines) + f"\n){optional};"
    if dialect == "sqlserver":
        ddl = f"IF OBJECT_ID(N'{table.name}', N'U') IS NULL\nBEGIN\n{ddl}\nEND;"
    return ddl


def render_indexes(table: Table, dialect: str) -> list[str]:
    if dialect in {"mysql", "mariadb"}:
        return []
    statements = []
    for name, cols in table.indexes:
        if dialect in {"oracle", "dm"}:
            digest = hashlib.md5((table.name + "." + name + "," + ",".join(cols)).encode("utf-8")).hexdigest()[:8]
            index_name = f"ix_{table.name[:10]}_{digest}"
        else:
            index_name = f"idx_{table.name}_{name}"
        joined = ", ".join(cols)
        if dialect in {"postgresql", "sqlite", "h2", "kingbase"}:
            statements.append(f"CREATE INDEX IF NOT EXISTS {index_name} ON {table.name}({joined});")
        elif dialect == "sqlserver":
            statements.append(
                "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = '" + index_name +
                "' AND object_id = OBJECT_ID('" + table.name + "'))\n"
                f"CREATE INDEX {index_name} ON {table.name}({joined});"
            )
        elif dialect in {"oracle", "dm"}:
            statements.append(f"CREATE INDEX {index_name} ON {table.name}({joined});")
    return statements


def sql_value(value, dialect: str) -> str:
    if value is None:
        return "NULL"
    if value == NOW:
        return NOW
    if isinstance(value, str):
        if value == "" and dialect in {"oracle", "dm"}:
            return "''"
        return "'" + value.replace("'", "''") + "'"
    return str(value)


def render_seed(table_name: str, dialect: str) -> list[str]:
    columns, rows = SEEDS[table_name]
    table = TABLES_BY_NAME[table_name]
    pk = table.primary_key
    statements = []
    if dialect in {"mysql", "mariadb"}:
        values = ",\n".join("(" + ", ".join(sql_value(v, dialect) for v in row) + ")" for row in rows)
        return [f"INSERT IGNORE INTO {table_name} ({', '.join(columns)}) VALUES\n{values};"]
    if dialect == "sqlite":
        values = ",\n".join("(" + ", ".join(sql_value(v, dialect) for v in row) + ")" for row in rows)
        return [f"INSERT OR IGNORE INTO {table_name} ({', '.join(columns)}) VALUES\n{values};"]
    if dialect in {"postgresql", "kingbase"}:
        values = ",\n".join("(" + ", ".join(sql_value(v, dialect) for v in row) + ")" for row in rows)
        return [f"INSERT INTO {table_name} ({', '.join(columns)}) VALUES\n{values}\nON CONFLICT DO NOTHING;"]
    if dialect == "h2":
        for row in rows:
            pairs = ", ".join(f"{col} = {sql_value(v, dialect)}" for col, v in zip(columns, row))
            statements.append(f"MERGE INTO {table_name} ({', '.join(columns)}) KEY ({', '.join(pk)}) VALUES ({', '.join(sql_value(v, dialect) for v in row)});")
        return statements
    if dialect == "sqlserver":
        for row in rows:
            predicates = " AND ".join(f"{col} = {sql_value(row[columns.index(col)], dialect)}" for col in pk)
            statements.append(
                f"IF NOT EXISTS (SELECT 1 FROM {table_name} WHERE {predicates})\n"
                f"INSERT INTO {table_name} ({', '.join(columns)}) VALUES ({', '.join(sql_value(v, dialect) for v in row)});"
            )
        return statements
    if dialect in {"oracle", "dm"}:
        for row in rows:
            selects = []
            aliases = []
            for col, value in zip(columns, row):
                selects.append(f"{sql_value(value, dialect)} AS {col}")
                aliases.append(col)
            on_clause = " AND ".join(f"t.{col} = s.{col}" for col in pk)
            statements.append(
                f"MERGE INTO {table_name} t USING (SELECT {', '.join(selects)} FROM DUAL) s ON ({on_clause}) "
                f"WHEN NOT MATCHED THEN INSERT ({', '.join(aliases)}) VALUES ({', '.join('s.' + col for col in aliases)});"
            )
        return statements
    return statements


def render_schema(dialect: str) -> str:
    statements = [
        "-- Generated by tools/generate_db_schemas.py. Do not edit manually.",
        f"-- Dialect: {dialect}",
        "",
    ]
    if dialect in {"mysql", "mariadb"}:
        statements.append("SET NAMES utf8mb4;")
    for table in TABLES:
        statements.append(render_table(table, dialect))
        statements.extend(render_indexes(table, dialect))
    statements.append("")
    statements.append("-- System seed data")
    for table_name in SEEDS:
        table = TABLES_BY_NAME[table_name]
        identity = any(col.auto for col in table.columns)
        if dialect in {"sqlserver", "dm"} and identity:
            statements.append(f"SET IDENTITY_INSERT {table_name} ON;")
        statements.extend(render_seed(table_name, dialect))
        if dialect in {"sqlserver", "dm"} and identity:
            statements.append(f"SET IDENTITY_INSERT {table_name} OFF;")
    if dialect in {"postgresql", "kingbase"}:
        statements.append("")
        statements.append("-- Keep identity columns ahead of explicit seed IDs")
        for table in TABLES:
            if any(col.auto for col in table.columns):
                auto = next(col for col in table.columns if col.auto)
                statements.append(f"ALTER TABLE {table.name} ALTER COLUMN {auto.name} RESTART WITH 1000;")
    return "\n\n".join(statements) + "\n"


def main() -> None:
    dialects = ["mysql", "mariadb", "postgresql", "sqlite", "h2", "sqlserver", "oracle", "dm", "kingbase"]
    for dialect in dialects:
        target = OUTPUT / dialect / "schema.sql"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(render_schema(dialect), encoding="utf-8")
        print(f"generated {target.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
