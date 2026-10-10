# OA 办公系统

基于 Spring Boot 2.7、MyBatis-Plus 和 Vue 2 的多数据库 OA 办公管理系统。

## 功能模块

- 用户、角色、部门管理
- 公告管理、通知查看
- 日程管理
- 请假申请与审批
- 报销申请、明细和审批
- 收文、发文及完整流程动作
- 提交或签收后按流程步骤自动流转到对应角色待办
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
- **TRUST_FORWARDED_HEADERS**：部署在可信反向代理后时设为 `true`，登录限流将使用 `X-Forwarded-For` 的首个地址；直连部署保持 `false`。
- **PUBLIC_API_DOCS**：是否匿名开放 Swagger，默认 `false`，生产环境保持关闭。
- **OA_INIT_<用户名大写>_PASSWORD**：初始账号的首次登录口令，例如 `OA_INIT_ADMIN_PASSWORD`、`OA_INIT_ZHANGSAN_PASSWORD`、`OA_INIT_LISI_PASSWORD`。未配置时应用会生成一次性随机密码，只写入本地口令文件，**不再写入日志**。
- **INIT_PASSWORD_FILE**：生成的一次性初始口令的落地文件路径，默认 `./oa-init-passwords.txt`；应用会自动创建父目录并尽量把权限收紧到仅属主可读写（Linux/macOS 为 `rw-------`，Windows 用 ACL 只保留属主），该文件不映射任何 HTTP 接口，读取后请立即删除。默认路径相对进程工作目录，从仓库根目录启动时会落在仓库里，**切勿提交到版本库**，生产环境建议用该变量把它指到仓库外的目录。
- **注册接口**：`/register` 已要求 `system:user:add` 权限，不再匿名开放。
- **登录保护**：单用户连续失败 5 次开始锁定，锁定时长随连续失败次数递增（30 秒 → 1 分钟 → 2 分钟 → 5 分钟 → 15 分钟封顶），锁定期间返回 429，错误提示统一为“用户名或密码错误”。
- **文件安全**：上传目录不再通过 `/profile/**` 静态暴露，附件只能通过鉴权下载接口访问，并限制允许的文件扩展名。
- **初始账号**：仓库不再内置任何可用口令，`admin`、`zhangsan`、`lisi` 的密码由首次启动时初始化，且强制首次登录修改；
  强制改密由**服务端**执行，改密完成前除 `GET /getInfo`、`POST /logout`、`PUT /system/user/profile/updatePwd` 外的接口一律 403。

## 启动后端

```powershell
cd E:\api\oa-system
$env:JWT_SECRET = "replace-with-at-least-32-random-bytes"
$env:DB_PASSWORD = "your-database-password"
$env:OA_INIT_ADMIN_PASSWORD = "your-initial-admin-password"
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

## 初始账号与密码

仓库中不再内置任何可用口令。数据库脚本把 `admin`、`zhangsan`、`lisi` 的 `password` 写成占位符
`PENDING_INITIAL_PASSWORD`（不是合法的 BCrypt 串，任何密码都无法匹配），并置 `must_change_pwd = '1'`。

| 账号 | 角色 | 权限范围 |
| --- | --- | --- |
| `admin` | 超级管理员 | 全部 84 个菜单/按钮权限 |
| `zhangsan` | 普通用户 | 自助办公（公告查看、日程、请假、报销、待办），无审批权 |
| `lisi` | 部门经理 | 普通用户权限 + 请假/报销审批、收发文拟制、流程与公文类型查看 |

应用首次启动时由 `InitialAccountInitializer` 完成初始化：

1. 优先读取环境变量 `OA_INIT_<用户名大写>_PASSWORD`，例如 `OA_INIT_ADMIN_PASSWORD`；
2. 未配置时生成 16 位一次性随机密码，**只写入服务器本地口令文件**（配置项 `app.init-password-file`，
   环境变量 `INIT_PASSWORD_FILE`，默认 `./oa-init-passwords.txt`），**不再写入任何日志**：日志里只出现文件路径
   和“用完立即删除”的提示。文件每行是 `用户名=口令`，多次启动会追加写入；应用会自动创建父目录并
   尽量把权限收紧到仅属主可读写（Linux/macOS 用 POSIX `rw-------`；Windows 没有 POSIX 权限位，
   改用 ACL 只给属主授权，但管理员仍可能读取，所以必须取走内容后立刻删除该文件）。该文件不是任何
   HTTP 资源的映射目录，也没有任何接口能读取它，只能登录服务器通过文件系统访问；
   若口令来自环境变量，则**不会**写任何文件（来源方已经知道口令）；
3. 无论口令来源如何，账号都被标记为首次登录必须修改密码；**该限制由服务端强制执行**
   （`JwtAuthenticationFilter`）。在改密完成前，除 `GET /getInfo`、`POST /logout`、
   `PUT /system/user/profile/updatePwd` 之外的任何接口一律返回 403
   `{"code":403,"msg":"首次登录必须修改密码后才能继续使用"}`，因此不能靠直接调用接口跳过改密；
   前端也会弹出不可关闭的改密框。
4. 口令需满足强度策略：8-64 位且同时包含字母与数字，否则回退为随机密码。

自助改密接口为 `PUT /system/user/profile/updatePwd`（请求体 `{"oldPassword":"...","newPassword":"..."}`），
任何已登录用户都可调用，不需要 `system:user:*` 权限。

从旧版本升级时，`DatabaseMigrationRunner` 会补上 `must_change_pwd` 列；对仍在使用内置演示口令
`admin123` 的账号，它不再只是"标记"，而是**直接作废口令**（把 `password` 覆盖为占位符
`PENDING_INITIAL_PASSWORD` 并置 `must_change_pwd='1'`），由同一次启动的 `InitialAccountInitializer`
重新初始化一个新口令。这样升级后的库不会继续接受公开的 `admin123`。

由于强制改密是服务端强制的，**任何自动化脚本在全新库上都必须先完成一次改密**才能调用其他接口。
集成测试脚本不再硬编码密码，需要显式传入：

```powershell
$env:OA_INIT_ADMIN_PASSWORD = "口令文件里的一次性密码"
$env:OA_INIT_ZHANGSAN_PASSWORD = "..."
.\tests\phase1-smoke.ps1
```

若脚本在全新库上直接调用业务接口会得到 403，需要先改密：

```powershell
# 1) 登录拿到 token
$login = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post -ContentType 'application/json' `
  -Body (@{username='admin';password=$env:OA_INIT_ADMIN_PASSWORD} | ConvertTo-Json -Compress)
$token = $login.data.token
# 2) 完成强制改密（之后 must_change_pwd 变为 0，其余接口恢复可用）
Invoke-RestMethod -Uri "$BaseUrl/system/user/profile/updatePwd" -Method Put `
  -Headers @{Authorization='Bearer '+$token} -ContentType 'application/json' `
  -Body (@{oldPassword=$env:OA_INIT_ADMIN_PASSWORD;newPassword='YourNewPass123'} | ConvertTo-Json -Compress)
```

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