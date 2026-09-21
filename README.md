# OA 办公系统

基于 Spring Boot 2.7、MyBatis-Plus 和 Vue 2 的多数据库 OA 办公管理系统。

## 功能模块

- 用户、角色、部门管理
- 公告管理、通知查看
- 日程管理
- 请假申请与审批
- 报销申请、明细和审批
- 收文、发文及完整流程动作
- 公文中心、传阅、已办、归档和公文类型
- 公文模板和套红预览
- 岗位、角色数据范围和操作审计
- 待办事项与首页统计

## 环境要求

- JDK 8 或更高版本
- Maven 3.8+
- 任选一种受支持数据库：MySQL 8+、MariaDB 10.5+、PostgreSQL 12+、SQLite 3、H2 2、SQL Server 2019+、Oracle 12c+、达梦 8、人大金仓 V8
- Node.js 16+，推荐 Node.js 18/20
- npm 8+

## 多数据库支持

系统通过 Spring Profile 和 MyBatis-Plus 自动方言检测支持以下数据库：

| 数据库 | Profile | 默认端口 |
|---|---|---:|
| MySQL | `mysql` | 3306 |
| MariaDB | `mariadb` | 3306 |
| PostgreSQL | `postgresql` | 5432 |
| SQLite | `sqlite` | - |
| H2 | `h2` | - |
| SQL Server | `sqlserver` | 1433 |
| Oracle | `oracle` | 1521 |
| 达梦 DM | `dm` | 5236 |
| 人大金仓 | `kingbase` | 54321 |

启动时选择 Profile，例如：

```powershell
java -jar .\ruoyi-admin\target\ruoyi-admin-1.0.0.jar --spring.profiles.active=postgresql
java -jar .\ruoyi-admin\target\ruoyi-admin-1.0.0.jar --spring.profiles.active=mysql
java -jar .\ruoyi-admin\target\ruoyi-admin-1.0.0.jar --spring.profiles.active=kingbase
```

通用连接参数：

- `DB_HOST`：数据库地址
- `DB_PORT`：数据库端口
- `DB_NAME`：数据库名称
- `DB_USERNAME`：用户名
- `DB_PASSWORD`：密码
- `SQLITE_DB_PATH`：SQLite 文件位置
- `H2_DB_PATH`：H2 文件位置
- `DB_INIT_MODE`：Oracle/达梦的初始化模式，默认 `always`

Oracle 和达梦脚本采用兼容初始化方式，重复启动时忽略“对象已存在”错误。生产环境建议设置 `DB_INIT_MODE=never`，由数据库管理员执行：

- `E:\api\oa-system\ruoyi-admin\src\main\resources\db\oracle\schema.sql`
- `E:\api\oa-system\ruoyi-admin\src\main\resources\db\dm\schema.sql`

初始化脚本由统一配置生成：

```powershell
py E:\api\oa-system\tools\generate_db_schemas.py
```

生成模板位于 `src/main/resources/db/<dialect>/schema.sql`，包含当前系统实际使用的 16 张核心表。MySQL 历史脚本 `sql/oa_init.sql` 仍保留全部预留扩展表。
## 数据库适配验证

当前验证状态：

| 数据库 | 验证方式 |
|---|---|
| MySQL / MariaDB | Schema 在 H2 MySQL 兼容模式下执行通过，JDBC 驱动已打包 |
| PostgreSQL | Schema 在 H2 PostgreSQL 兼容模式下执行通过，JDBC 驱动已打包 |
| SQLite | 使用真实 SQLite 文件启动、登录和查询通过 |
| H2 | 使用真实 H2 文件启动、登录和查询通过 |
| 人大金仓 | Schema 在 PostgreSQL 兼容模式下执行通过，JDBC 驱动已打包 |
| SQL Server | DDL 按 T-SQL 方言生成并通过语法解析，JDBC 驱动已打包 |
| Oracle | DDL 按 Oracle 方言生成并通过语法解析，JDBC 驱动已打包 |
| 达梦 DM | DDL 按兼容方言生成并通过语法解析，JDBC 驱动已打包 |

SQL Server、Oracle、达梦和人大金仓在正式部署前，仍应在对应真实数据库实例上执行一次 `schema.sql` 集成测试。

MySQL、MariaDB、PostgreSQL、SQL Server 和 Oracle 需要预先创建数据库；SQLite 和 H2 会自动创建数据库文件。
## SQLite 本地快速启动

无需安装 MySQL，直接使用 SQLite Profile：

```powershell
cd E:\api\oa-system
mvn clean package
java -jar .\ruoyi-admin\target\ruoyi-admin-1.0.0.jar --spring.profiles.active=sqlite
```

启动后会在当前目录创建 `oa-system-local.db`，并自动建表和初始化数据。

可以通过环境变量指定数据库位置：

```powershell
$env:SQLITE_DB_PATH='E:\api\oa-system\oa-system-local.db'
java -jar .\ruoyi-admin\target\ruoyi-admin-1.0.0.jar --spring.profiles.active=sqlite
```

SQLite 仅建议用于本地开发和演示，生产环境仍建议使用 MySQL。

旧 SQLite 数据库升级到首期结构时，可执行：

`E:\\api\\oa-system\\ruoyi-admin\\src\\main\\resources\\db\\sqlite\\migration-phase1.sql`

## 初始化数据库

1. 创建数据库：

```sql
CREATE DATABASE oa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

2. 导入初始化脚本：

```powershell
mysql -uroot -p oa_system < E:\api\oa-system\sql\oa_init.sql
```

3. 根据本机环境修改：

`E:\api\oa-system\ruoyi-admin\src\main\resources\application-dev.yml`

生产环境可通过 `DB_PASSWORD` 覆盖数据库密码。

## 安全配置（生产必读）

生产环境必须配置以下环境变量，仓库中不再保留 JWT 或数据库弱默认密码：

- **JWT_SECRET**：至少 32 字节的随机字符串；缺失或过短时应用拒绝启动。
- **DB_PASSWORD**：数据库密码。
- **CORS_ALLOWED_ORIGINS**：允许跨域的前端域名，多个域名用英文逗号分隔；默认仅允许本地 `http://localhost:18081`。
- **PUBLIC_API_DOCS**：是否匿名开放 Swagger，默认 `false`，生产环境保持关闭。
- **注册接口**：`/register` 已要求 `system:user:add` 权限，不再匿名开放。
- **登录保护**：单用户连续失败 5 次锁定 15 分钟，错误提示统一为“用户名或密码错误”。
- **文件安全**：上传目录不再通过 `/profile/**` 静态暴露，附件只能通过鉴权下载接口访问，并限制允许的文件扩展名。
- **默认账号**：`admin/admin123`、`zhangsan/admin123`、`lisi/admin123` 仅用于演示，上线前请修改密码或禁用。

## 启动后端

```powershell
cd E:\api\oa-system
$env:JWT_SECRET = "replace-with-at-least-32-random-bytes"
$env:DB_PASSWORD = "your-database-password"
mvn clean package
java -jar .\ruoyi-admin\target\ruoyi-admin-1.0.0.jar
```

本地开发默认地址：`http://localhost:8080`

## 启动前端

```powershell
cd E:\api\oa-system\ruoyi-ui
npm install
npm run dev
```

本地开发默认地址：`http://localhost:18081`

## 构建前端

```powershell
cd E:\api\oa-system\ruoyi-ui
npm run build:prod
```

构建产物位于 `ruoyi-ui\dist`。

## 默认账号

- 用户名：`admin`
- 密码：`admin123`

另外提供测试账号 `zhangsan`、`lisi`，密码同为 `admin123`。

## 项目结构

```text
oa-system/
├─ pom.xml                 Maven 聚合工程
├─ sql/oa_init.sql         数据库建表和初始化数据
├─ ruoyi-admin/            后端服务
│  ├─ pom.xml
│  └─ src/main/java/com/oa
│     ├─ oa/               OA 业务模块
│     ├─ system/           用户、角色、部门模块
│     ├─ security/         JWT 和认证
│     └─ config/           框架配置
└─ ruoyi-ui/               Vue 前端
   ├─ package.json
   └─ src/
      ├─ api/              API 封装
      ├─ router/           路由
      ├─ store/            Vuex
      └─ views/            页面
```

## 预留数据表

初始化脚本保留了考勤、加班、通讯录、通用审批、公文传阅、提醒和归档等扩展表。这些表用于后续功能扩展，当前菜单和 API 不依赖它们。


## 验证命令

```powershell
cd E:\api\oa-system
mvn test

cd E:\api\oa-system\ruoyi-ui
npm run build:prod
```