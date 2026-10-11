# 测试资产说明

本目录包含四类测试。**运行任何一类之前，请先读第 4 节的注意事项** —— 其中几条会直接导致测试失败，且原因不在业务代码。

## 1. 后端单元/集成测试（随构建执行）

```powershell
cd <仓库根目录>
mvn verify
```

- 24 个测试，其中与数据层相关的几个值得注意：
  - `DatabaseSchemaTest` —— 在真实 H2/SQLite 上执行 9 种方言的 schema 并校验种子数据。
  - `SchemaIdentityStartTest` —— 断言每张自增表的身份列起点严格大于种子中的最大主键（防止"新建记录主键冲突"）。**这个不变量被破坏时会直接失败。**
  - `PreAuthorizeSeedTest` —— 断言代码里每个 `@PreAuthorize` 权限串都能在菜单种子里找到。**新增权限串却没在 `sql/oa_init.sql` 与 `tools/generate_db_schemas.py` 的菜单数据里同步时会失败**，这是它的设计目的。
- 这些测试不启动 Spring 上下文（没有 `@SpringBootTest`/MockMvc），因此它们**不覆盖**"某角色调用某接口是否返回 403"这类行为，那种覆盖由下面的 `fulltest.py` 提供。

## 2. 接口级全功能测试 `fulltest.py`

覆盖全部 24 个控制器、三种角色、约 156 条断言，包含权限边界、数据范围、状态机、公文流转全链路、附件上传下载、越权（IDOR）与自签/接管等安全断言。

```powershell
$env:OA_INIT_ADMIN_PASSWORD    = "<admin 初始口令>"
$env:OA_INIT_ZHANGSAN_PASSWORD = "<zhangsan 初始口令>"
$env:OA_INIT_LISI_PASSWORD     = "<lisi 初始口令>"
py tests\fulltest.py                 # 或 python tests/fulltest.py
# 可用 OA_BASE_URL 指定后端地址（默认 http://127.0.0.1:8080）
```

- 脚本会先完成服务端强制的首次改密，再继续测试；改密后的口令默认是"初始口令 + 1"，可用 `OA_TEST_NEW_<用户大写>_PASSWORD` 指定。
- **必须在全新数据库上运行**，否则"强制改密前受保护接口被拒绝"这类断言会被跳过。
- 退出码非 0 表示有断言失败，末尾会列出失败项与已知缺口。

## 3. HTTP 冒烟脚本（PowerShell）

| 脚本 | 覆盖 |
|---|---|
| `api-smoke.ps1` | 37 条断言：登录/认证、用户/角色/部门/岗位 CRUD、公告、日程、请假提交审批、报销明细、收发文签发、流程、核心列表 |
| `phase1-smoke.ps1` | 完整公文流程：签收 → 会签 → 传阅 → 催办 → 办结 → 归档，并打印时间线各计数 |
| `phase2-exhaustive.ps1` | 数据范围矩阵、公文动作矩阵、附件、审计脱敏等更细的检查 |

```powershell
$env:OA_INIT_ADMIN_PASSWORD = "<当前 admin 口令>"
.\tests\api-smoke.ps1     -BaseUrl http://127.0.0.1:8080 -AdminPassword $env:OA_INIT_ADMIN_PASSWORD
.\tests\phase1-smoke.ps1  -BaseUrl http://127.0.0.1:8080 -AdminPassword $env:OA_INIT_ADMIN_PASSWORD
```

## 4. UI 功能测试（Playwright）

`ui-functional.fn.js`（16 个路由 + 界面公告增删）、`ui-smoke.fn.js`（13 个路由）、`ui-announcement-crud.fn.js`（公告增删）由 `run-ui.js` 依次执行：

```powershell
# 仅首次需要安装 playwright（用 --no-save，避免影响 CI 的 npm ci）
cd ruoyi-ui
npm i --no-save playwright
cd ..

$env:OA_INIT_ADMIN_PASSWORD = "<当前 admin 口令>"
node tests\run-ui.js
```

- `run-ui.js` 会在 `ruoyi-ui/node_modules`、仓库根 `node_modules`、全局安装与本机 Chrome 之间自动选择，因此安装位置不影响使用；找不到时会给出明确的安装提示。
- 默认使用**本机已安装的 Google Chrome**，无需下载 Playwright 自带浏览器；也可用 `OA_CHROME_PATH` 指定，或先 `npx playwright install chromium`。
- `OA_UI_URL` 可覆盖前端地址（默认 `http://localhost:18081`；脚本内硬编码的地址会被替换）。
- 结果是 `tests/ui-result.json`。若账号仍处于强制改密状态，需额外设置 `OA_TEST_NEW_PASSWORD`。
- **未把 `playwright` 写进 `ruoyi-ui/package.json`**：那会让 CI 的 `npm ci` 额外拉取浏览器（数十 MB），而 UI 测试并非每次构建都需要。

## 5. 注意事项（踩过的坑，按重要性排列）

1. **`mvn verify` 期间不要运行应用实例。** Windows 会锁住 `ruoyi-admin/target/ruoyi-admin-1.0.0.jar`，`spring-boot:repackage` 无法把它改名为 `.jar.original`，构建会报 `Unable to rename ... jar.original` 而失败。这是文件锁问题，不是代码问题。
2. **服务端强制首次改密。** 在 `must_change_pwd='1'` 时，除 `GET /getInfo`、`POST /logout`、`PUT /system/user/profile/updatePwd` 之外的接口一律返回 403。因此**任何自动化脚本都必须先完成改密**，`fulltest.py` 与 `api-smoke.ps1` 已内置这一步。
3. **PowerShell 5.1 的编码陷阱（本目录的 `.ps1` 已修复，改造脚本时请勿回退）：**
   - 脚本文件必须带 UTF-8 BOM，否则 PS 5.1 按 ANSI/GBK 解析，中文字符串会破坏脚本语法；
   - 请求体必须显式以 **UTF-8 字节** 提交并声明 `charset=utf-8`，否则中文会被写成 `????` 存进数据库；
   - 响应体必须自己按 UTF-8 解码（Spring 返回的 `Content-Type` 不带 charset，PS 5.1 会退回 ISO-8859-1），否则中文变乱码、按中文名比对的断言必然失败。
4. **`phase2-exhaustive.ps1` 需要 PowerShell 7+**（用到了 `Invoke-RestMethod -Form`）。
5. **登录失败会触发递增退避锁定并返回 429**：第 5 次失败锁 30 秒，之后 1/2/5 分钟递增，上限 15 分钟。计数与锁定都在内存里，重启应用即清除。不要用探测脚本试口令，否则会把自己锁住（首窗只有 30 秒，但连续触发会越来越久）。
6. **前端 dev server 的代理目标**：`ruoyi-ui/vue.config.js` 默认指向 `http://localhost:18080`，可用 `VUE_APP_BACKEND_URL` 覆盖。开发服务器默认只监听 `127.0.0.1`。
7. **界面文案断言用的是中文文本**（如"公告标题"）。改动页面列标题会让 UI 测试失败 —— 这是有意的，它同时也是"页面确实渲染了"的证据。

## 详尽矩阵：phase2-exhaustive.ps1

覆盖状态机（2 种公文 × 4 种状态 × 16 个动作 = 96 条）、文件上传/下载/归属、数据范围矩阵（5/3/4/2/多角色）、
审计日志脱敏与导出。**在 Windows PowerShell 5.1 下即可运行**（不需要 pwsh 7）。

```powershell
.\tests\phase2-exhaustive.ps1 -BaseUrl http://127.0.0.1:8080 -AdminPassword '<管理员口令>' -CommonPassword '<张三口令>'
# 期望 SUMMARY PASS=157 FAIL=0
```

注意两点：

1. 脚本会临时改写**角色 2（普通用户）**的数据范围与菜单来做数据范围矩阵，结束时会还原。
   如果脚本中途被打断（例如断电），请手工确认角色 2 的 `data_scope` 仍是 `5`。
2. 脚本需要 `ruoyi-admin/target` 目录存在（上传用临时文件放在那里）。

### 撤签（cancel_sign）的语义

撤签撤销的是**流程派发出来、尚未办理的待办签批**（`oa_doc_approval.approve_result = '0'`）：

| `approve_result` | 含义 | 能否撤签 |
|---|---|---|
| `0` | 待办（流程派发，未办理） | 能（本人或管理员） |
| `1` / `2` | 已通过 / 已拒绝 | 否 |
| `5` / `7` | 已办结的会签 / 加签意见 | 否 |
| `8` | 已撤签（撤签写入的结果） | 否 |
| `9` | 因他人已办理而作废 | 否 |

**关键前提**：待办记录只在公文**绑定流程**（创建时带 `flowId`，或提交时自动匹配唯一启用流程）后
由 `assignStepToRole` 产生。种子数据不含任何流程，需先在「流程配置」里建流程；没有流程的公文
不会有待办，也就无从撤签。

## 可重复运行性（两个套件都会自建前置条件）

两个接口级套件都**可以被反复运行**，不必每次重置数据库：

| 情形 | 行为 |
|---|---|
| 数据库不是全新的（账号 `must_change_pwd=0`） | `fulltest.py` 会**跳过** 2 条「强制改密前受保护接口被拦截」断言并打印 NOTE，断言总数 166 而非 168 |
| 库中已存在**启用中的公文流程** | 两个套件都会在开始前**临时停用**它们、结束时**原样还原**。因为没带 `flowId` 的公文在提交时会被自动绑定到唯一启用流程并分派给流程角色，从而让「签发」「数据范围」类断言失真；同类型存在多条启用流程时还会触发 `findSingleActiveFlow` 的多流程保护（400） |
| 套件中途被中断 | `fulltest.py` 的流程还原、`phase2-exhaustive.ps1` 的角色2/流程还原都在收尾段；`phase2` 的 `Check()` 已改为失败不中断，因此收尾必然执行 |

> `fulltest.py` 第 [10] 节会用 `bootstrap()` 记录下来的**当前真正生效的口令**登录，不再假设口令等于 `OA_INIT_* + '1'`。

## PowerShell 5.1 踩坑清单（本项目实测）

1. **`.ps1` 必须带 UTF-8 BOM**，否则 5.1 按 ANSI 解析，中文注释/字符串会乱码甚至语法错误。
2. **`Invoke-RestMethod` 没有 `-Form`**（那是 PS 7 才有的）。文件上传要手工拼 `multipart/form-data`
   字节体，见 `phase2-exhaustive.ps1` 的 `UploadFile`。
3. **`Invoke-WebRequest` 不带 `-UseBasicParsing`** 会走 IE 引擎，在非交互模式下直接报
   "Windows PowerShell 处于非交互模式"。下载请求一律加 `-UseBasicParsing`。
4. **收发中文必须显式处理 UTF-8**：请求体用 `[Text.Encoding]::UTF8.GetBytes($json)` 并以
   `application/json; charset=utf-8` 发送；响应读 `RawContentStream` 再按 UTF-8 解码。
   直接用 `Invoke-RestMethod` 会按 ISO-8859-1 猜测，中文变成 `å¬...` 这类乱码 ——
   断言中文错误信息时会**误报失败**（本次实测 `errorMsg="公文不存在"` 就踩了这个坑，而库里存的是正确的）。
5. **不要用 `Select-Object -Last N` 去接脚本内部的 `Format-Table` 输出**，会触发
   `out-lineoutput ... FormatEntryData 类型无效` 错误。改为重定向到文件再读尾部。
6. **`Check()` 失败不要再 `throw`**：否则后续断言永远不执行、清理段也跑不到，遗留数据会改变下一次运行的结果。
