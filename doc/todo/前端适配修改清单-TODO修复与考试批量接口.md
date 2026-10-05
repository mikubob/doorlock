# 前端适配修改清单：后端 TODO 修复与考试批量接口

> 整理日期：2026-10-04（Asia/Shanghai）  
> 前端项目：`D:\CodingFiles\School\assessment-front\assessment-front`  
> 后端项目：`D:\CodingFiles\School\assessment-back-v2\assessment`  
> 依据：本目录 TODO 专项方案、实施记录，以及当前前后端源码。方案中的旧代码和建议不直接当作当前接口契约；有差异时以当前源码为准。  
> 本次只编写修改清单，没有修改前端或后端代码，也没有执行运行环境联调。下文复选框均为后续实施与验收事项。

## 1. 修改范围与优先级

本文前端文件路径均相对于上述前端根目录。接口路径包含现有 `/api` 上下文前缀。

| 优先级 | 事项 | 涉及前端文件 | 工作性质 |
|---|---|---|---|
| P0 | 正确处理实际 HTTP 401、403、409，以及下载错误 JSON | `src/util/axios.js` | 必须修改，多个模块共同依赖 |
| P0 | 区分教室 ID、班牌 SN、考试 ID；接入考试批量删除 | `src/views/digitaldoor2d/index.vue`、`src/util/axios.js` | 字段修正、批量入口新增 |
| P1 | 新增批量增加考试、批量修改考试功能 | `src/views/digitaldoor2d/index.vue`，或新增考试管理页面 | 新功能 |
| P1 | 显示巡查 Excel 导入回执和逐行错误 | `src/views/patrolsentering/index.vue` | 现有上传入口适配 |
| P1 | 子任务成绩查询权限提示、修改结果确认 | `src/views/subexamineupdate/index.vue`、`src/views/attachdowload/index.vue` | 现有成绩入口适配 |
| P1 | Word 下载区分附件与错误，取消提前报成功 | `src/views/attachdowload/index.vue`、`src/util/axios.js` | 现有导出入口适配 |
| P1 | 审批重复提交、无权限、失效任务提示 | `src/views/examinedetails/index.vue`，并回归 `src/views/assess/index.vue` | 现有审批及提交入口适配 |
| P2 | 优秀项目考核列表分页、参数和排名适配 | `src/views/projectassesslist/index.vue` | 现有分页及错误判断修正 |
| P2 | 子项合并导入回执与项目冻结提示 | 建议新增子项管理组件；关联 `src/views/creationproject/index.vue`、`src/views/project/index.vue` | 当前未找到合并导入调用点，按功能需求新增 |
| 回归 | 课表同步、电子班牌读取、原单条考试接口 | `src/views/digitaldoor2d/index.vue`、`src/views/eboard/index.vue` | 保留现有调用，重点验证数据展示 |

**当前前端接入情况：**搜索 `src` 后，考试业务只发现 `/api/exam/list`、`/api/exam/add`、`/api/exam/{id}` 调用，没有发现批量考试接口调用，也没有发现单条考试修改入口。`examinelist`、`examinedetails` 是项目审批页面，不是班牌考试管理页面；考试批量功能应接在 `digitaldoor2d` 或独立考试管理页。

## 2. 公共请求封装：先修复错误处理与 DELETE 请求体

### 2.1 HTTP 状态与业务状态均须处理

当前位置：`src/util/axios.js` 响应拦截器。

- 当前成功分支把 `code=401` 和 `code=403` 都当成登录失效，清除账号状态、Token 并跳登录。
- 当前错误分支使用 `error.data`，但 Axios 的服务端错误响应应从 `error.response` 读取；实际 HTTP 403、409 等可能被转换为 `undefined`。
- `get()` 还存在空的 `.catch()`，页面可能拿不到可识别的失败结果。

后端现在同时存在“HTTP 200 + 非 200 业务码”和“实际 HTTP 非 2xx + JSON 业务码”，前端须覆盖两条响应路径。

| 状态 | 前端应有行为 |
|---|---|
| 401 | 认证失效：沿用登录恢复流程，电子班牌等已有路由例外需保留 |
| 403 | 当前操作没有权限或学院配置不完整：显示后端 `msg`，保留登录状态 |
| 409 | 重复审批、项目冻结、任务失效等业务冲突：显示 `msg`，由页面提供刷新或返回操作 |
| 400 | 参数不合法、考试冲突、导入被拒绝等：显示 `msg`，保留可修改的表单 |
| 404 | 对象失效时优先给当前页面提供提示和刷新机会；不要仅凭对象 404 就强制全站跳转 |
| 413 / 422 / 503 | Word 导出超限、PDF 无效、生成繁忙等：解析错误 JSON 并展示原因 |
| 网络中断 / 超时 | 给出明确失败或结果未知提示，恢复按钮；写操作不自动重发 |

- [ ] 选择统一的错误交付方式：保留结构化错误并 reject，或返回可识别的失败对象；同步适配调用页，避免继续返回 `undefined`。
- [ ] 页面读取 `data.list`、`list` 之前验证响应成功及字段存在，失败时不继续处理数据。
- [ ] 请求按钮在等待期间禁用，在 `finally` 中恢复；失败不报成功、不主动丢弃用户输入。
- [ ] 403、409 不清除 Token，不触发重新登录。

### 2.2 批量删除必须发送 JSON Body

现有 `delete(url, params)` 把第二个参数放进 Axios `params`，只会形成查询参数。新考试批量删除是 `@RequestBody List<Long>`，因此必须支持 DELETE 的 `data`。

- [ ] 为 DELETE 增加明确的 Body 支持，或增加批量删除专用封装；保留其他调用方已有查询参数语义。
- [ ] 保持统一 Token 请求拦截器和 JSON Content-Type。
- [ ] 不把 `[101, 102]` 放入查询字符串，也不包装成 `{ "ids": [101, 102] }`。
- [ ] 核对 PUT、DELETE 的 `baseURL` 与 `/api` 代理组合，确保生产与开发地址均正确。

`src/main.js` 已挂载 `$put`、`$delete`，不必因接入批量考试再重复挂载。`digitaldoor2d` 的单条删除目前使用 `$axios` / `fetch` 分支；建议统一到现有请求封装，以便自动携带认证信息并统一处理错误。

### 2.3 下载响应必须单独处理

`responseType: 'blob'` 时，即使服务端返回错误 JSON，Axios 也可能交付 Blob。成功的附件没有业务 `code`，不能按普通 JSON 判断登录或成功。

- [ ] 附件请求保留响应头和 HTTP 状态，依据 Content-Type 识别附件或 JSON。
- [ ] 对成功分支和非 2xx 错误分支中的 JSON Blob 均读取文本、解析 `code/msg`。
- [ ] 只有取得有效附件后才触发保存；错误 JSON 不得保存成 `.docx`。
- [ ] 保留 `Retry-After` 等响应头，供页面提示重试时间。

## 3. 考试接口：路径与请求格式

依据：后端 `ExamController`、`ExamServiceImpl`、`Exam`。

### 3.1 当前接口表

| 功能 | 方法 | 当前完整路径 | 参数 |
|---|---|---|---|
| 查询考试列表 | POST | `/api/exam/list` | 可选考试条件对象，例如 `{ "boardSn": 12345 }` |
| 查询考试详情 | GET | `/api/exam/{id}` | 路径中的考试 ID |
| 单条新增 | POST | `/api/exam/add` | 考试对象 |
| 单条修改 | PUT | `/api/exam/update` | 考试对象，包含 `id`，当前控制器仍要求 `boardSn` |
| 单条删除 | DELETE | `/api/exam/{id}` | 路径中的考试 ID |
| **批量删除** | **DELETE** | **`/api/exam/batchDelete`** | **Body：考试 ID 数组** |
| **批量增加** | **POST** | **`/api/exam/batchAdd`** | **Body：考试对象数组** |
| **批量修改** | **PUT** | **`/api/exam/batchUpdate`** | **Body：包含考试 ID 的对象数组** |
| 修改考试状态 | PUT | `/api/exam/status/{id}` | 查询参数 `status=0/1/2` |

批量删除应使用当前 `/batchDelete` 映射。当前源码没有 `/exam/deleteBatch` 映射，也没有保留另一条批量删除别名；若其他分支或后续功能还存在旧路径，需要一并替换。**本次检查的前端没有批量删除调用点，因此该前端的实际工作是新增接入，不能只做路径文本替换。**

### 3.2 批量删除示例

```http
DELETE /api/exam/batchDelete
Content-Type: application/json
```

```json
[101, 102, 103]
```

这里的每个 ID 是 `Exam.id`，不是教室主键或班牌 SN。后端拒绝空数组；当前不返回逐条删除结果或实际删除条数，前端不要编造这类回执。

### 3.3 批量增加示例

```http
POST /api/exam/batchAdd
Content-Type: application/json
```

```json
[
  {
    "boardSn": 12345,
    "examCode": "A101",
    "examContent": "软件测试",
    "startTime": "2026-10-10 09:00:00",
    "endTime": "2026-10-10 11:00:00",
    "status": 0
  },
  {
    "boardSn": 12346,
    "examCode": "A102",
    "examContent": "软件测试",
    "startTime": "2026-10-10 09:00:00",
    "endTime": "2026-10-10 11:00:00",
    "status": 0
  }
]
```

示例 SN 为占位，联调时必须选择后端实际存在的教室。顶层必须是数组，不是 `{ "exams": [...] }`。新增不提交 `id`、`createTime`、`updateTime`。

### 3.4 批量修改示例

```http
PUT /api/exam/batchUpdate
Content-Type: application/json
```

```json
[
  {
    "id": 101,
    "examContent": "软件测试（调整）",
    "startTime": "2026-10-10 13:00:00",
    "endTime": "2026-10-10 15:00:00"
  },
  {
    "id": 102,
    "status": 2
  }
]
```

每条必须带 `id`。批量接口会读取原记录并合并非 null 字段，支持只提交变更字段；省略或 null 表示保留原值，不能用 null 清空字段。`examCode` 不允许清空；可选文本若需清空，应按当前非 null 合并规则提交空字符串并联调确认。

### 3.5 字段与校验规则

| 字段 | 含义与前端处理 |
|---|---|
| `id` | 考试主键，修改、删除使用；新增不填 |
| `boardSn` | 教室电子班牌 SN，后端按此字段检查教室是否存在；必须来自教室的 `boardSn` |
| `examCode` | 考试号；新增及修改合并后的值不能为空。现有 UI 标为考场编号，需确保含义与业务一致 |
| `examContent` | 考试内容，当前后端没有必填校验；前端是否要求填写属于界面业务规则 |
| `startTime` / `endTime` | `yyyy-MM-dd HH:mm:ss`；将日期控件中的 `T` 转为空格，并补足秒 |
| `status` | 0 未开始、1 进行中、2 已结束；新增不传时后端默认 0。批量页面应限制为这三个选项 |
| `imageUrl` / `videoUrl` | 可选媒体路径；修改时仅提交用户实际变更内容 |

- [ ] 校验非空批次、必填字段和有效时间，批量修改及删除去除重复 ID。
- [ ] 同一教室的时间冲突同时检查数据库已有记录和本次批次内记录；前端可提前提示，最终以后端为准。
- [ ] 当前冲突判断包含时间端点：已有考试 11:00 结束，新考试 11:00 开始也会被判为冲突。
- [ ] 后端时间顺序校验拒绝“开始晚于结束”，允许相等；现有前端要求结束严格晚于开始，可保留该更严格规则，但不能描述成后端已经拒绝相等。
- [ ] 将后端 `msg` 完整展示，包含“第 N 条考试”及批次内冲突条目，失败后保留输入。
- [ ] 当前批量接口先校验再写入，并使用事务；响应是整批成功或错误消息，没有逐条成功清单，也没有新增 ID 列表。
- [ ] 只在 `code=200` 时关闭弹窗、清除选中项并重新读取列表；不要在失败后展示“部分成功”。

当前批量新增/修改成功示例：

```json
{ "code": 200, "msg": "批量添加考试成功" }
```

```json
{ "code": 400, "msg": "第2条考试：该教室在此时间段已有考试安排，时间冲突" }
```

考试控制器的上述业务失败通常仍是 HTTP 200，因此只判断 HTTP 状态不足以判断成功。

**批量修改的现有边界：**数据库冲突检查只排除当前这一条的 ID，没有排除同批次其他考试的旧记录。即使新安排之间不冲突，交换两个既有考试时间等场景也可能被旧记录挡住。前端应显示实际错误，不自动拆批、自动重试或宣称已支持时间互换；如业务需要时间互换，需要另行完善后端。

## 4. 考试页面具体修改清单

主要位置：`src/views/digitaldoor2d/index.vue`。

### 4.1 先修正 SN 映射

当前 `formatClassrooms()` 使用 `examMap[String(room.id)]` 匹配考试，返回的页面教室对象仅保留 `id`。`saveExamMode()`、`viewExamRecords()` 又把 `selectedClassroom.id` 转成 `boardSn`，缺失时使用固定 `12345`。

后端 `Classroom.id` 和 `Classroom.boardSn` 是两个独立字段，`ClassroomMapper.xml` 也分别映射，不能互相替代。

- [ ] 教室页面模型同时保存教室 `id` 和真实 `boardSn`。
- [ ] 教室考试状态按 `room.boardSn` 匹配 `exam.boardSn`。
- [ ] 新增及查询考试提交选中教室的真实 `boardSn`。
- [ ] 移除固定 `12345` 兜底；缺少 SN 时阻止提交并提示教室未配置班牌。
- [ ] 考试记录模型保留 `id`、`boardSn`、`status` 和原始考试字段，不能只保留展示用的 `subject/roomNumber/time`，否则修改时易丢字段或混淆 ID。

### 4.2 新增批量操作入口

- [ ] 考试记录列表增加多选和“批量删除”；无选中时禁用，确认框显示所选考试数量。
- [ ] 增加“批量安排考试”：支持新增/删除表单行，或选择多教室后展开为每教室一条考试；提交一次 `/batchAdd`。
- [ ] 增加“批量修改”：勾选记录后编辑，保留每条考试 ID；提交一次 `/batchUpdate`。
- [ ] 若采用统一时间、状态等批量编辑面板，区分“未修改”和“用户填写空值”，避免把所有记录覆盖成表单默认值。
- [ ] 新增和修改的行顺序须与请求数组一致，便于按后端“第 N 条”定位错误。
- [ ] 批量操作成功后刷新考试记录和教室总览，更新考试标识；已有单条新增、删除也应同步刷新。
- [ ] 当前 `saveExamMode()` 无论业务是否成功都会关闭弹窗，改为只有成功才关闭。
- [ ] 保留单条 `/add`、`/{id}` 的有效调用；批量接口新增不意味着原单条路径被删除。

### 4.3 列表和班牌回归

`POST /api/exam/list` 当前返回 `data` 数组；没有数据时返回 `code=400,msg=考场不存在`，并不是固定的成功空数组。

- [ ] 考试管理页和 `src/views/eboard/index.vue` 对无安排状态做明确空态展示，避免沿用旧考试内容。
- [ ] 识别后端“无安排”响应与网络/权限错误，不能把所有失败都当作暂无考试。
- [ ] 批量操作后回归班牌考试内容、考试时间、状态及倒计时；列表接口路径不变。

## 5. T-03：巡查 Excel 导入回执

位置：`src/views/patrolsentering/index.vue` 的 `el-upload`（约第 79 行）及空的 `uploadSuccess()`（约第 660 行）。

接口仍为 `POST /api/schedule/upload`，使用 multipart，文件字段名为 `file`。无需改上传路径。

成功或部分成功时响应示意：

```json
{
  "code": 200,
  "msg": "示例导入结果说明，以服务端返回文本为准",
  "data": {
    "success": true,
    "totalRows": 10,
    "savedRows": 8,
    "failedRows": 2,
    "errors": [
      { "rowIndex": 4, "message": "示例：应到人数格式不正确" },
      { "rowIndex": 9, "message": "示例：日期格式不正确" }
    ],
    "headRowCount": 1,
    "message": "示例导入结果说明，以服务端返回文本为准",
    "costMillis": 120
  }
}
```

- [ ] 实现 `uploadSuccess()`：首先检查 `response.code`，不能把上传组件的成功事件等同于业务成功。
- [ ] 展示 `totalRows/savedRows/failedRows`，有失败行时使用警告或部分成功提示。
- [ ] 展示 `errors` 表格，列为 Excel 行号及错误原因；`rowIndex` 已是从 1 开始且包含表头的行号，不再加减。
- [ ] `success=true` 也可能包含跳过的坏行，不能仅凭该字段显示“全部成功”。
- [ ] `code=400` 的“导入被拒绝”显示 `msg`，不显示入库成功，不期待一定存在 `data/errors`。
- [ ] 补充上传网络错误处理及加载状态；`el-upload` 自己的上传链路须单独处理，不依赖 Axios 响应拦截器。
- [ ] 有实际入库行时刷新巡查列表和相关统计，保留错误清单供用户修正。
- [ ] 提示用户仅修正并重传失败行；后端事务保护不等于重复上传自动去重，部分成功后整份重传可能重复导入成功行。
- [ ] 学院由服务端上传用户上下文确定，不在前端自行追加未经支持的学院选择参数。

## 6. T-04：项目子项合并为任务及冻结提示

当前前端未找到 `/api/project/item/insertIntoTask` 或项目子项增删入口调用，已有 `/api/project/item/selectIntoTaskFile` 是佐证查询，不能作为导入入口。若要交付子项管理功能，建议在项目编辑页增加组件或独立页面，而不是假设已有页面只需换响应字段。

### 6.1 合并导入契约

```http
POST /api/project/item/insertIntoTask
Content-Type: application/json
```

```json
[
  { "id": 201, "projectId": 46 },
  { "id": 202, "projectId": 46 }
]
```

- [ ] 只提交来源子项 `id` 和 `projectId`；标题、分数、评分规则等由后端从数据库读取。
- [ ] 每批 1～1000 条、ID 不重复、属于同一项目；失败不自动拆批绕过整批规则。
- [ ] 展示 `data.projectId/mode/requested/created/updated/skipped/deleted/retainedOtherTasks`。
- [ ] 当前 `mode` 固定 `MERGE`，`deleted` 固定 0；界面文案使用“合并导入”，不提供全量替换、清空原任务、未选即删除等选项。
- [ ] 导入保留来源子项、原任务主键、未选任务和其他来源任务；成功后刷新任务树与子项列表。
- [ ] 完全相同的再次导入可显示跳过计数，不能把 `created=0` 当成失败。

### 6.2 权限、历史审核与冻结

管理权限为当前有效管理员或项目创建者。审批人、抄送人和院长身份不自动获得他人项目的子项管理权限。

有任务实际变更时，后端检查未发布、历史审核完成、未产生暂存/结果/材料/审批历史。409 应展示具体原因；无变化的合并重试不会仅因已发布或已冻结而失败。

- [ ] 子项管理的 403 显示权限提示，409 显示冻结或迁移审核原因；前端不通过修改提交字段绕过。
- [ ] 不将“禁止改变任务来源”误解释为“所有已发布项目成绩都不能修改”；成绩接口适用自己的授权和有效性校验。
- [ ] `src/views/project/index.vue` 的发布、删除等动作等待成功响应，遇到冻结、来源不一致、权限拒绝时保留当前页面并提示。
- [ ] 回归 `creationproject` 项目保存、`assess` 暂存/提交的失败提示；任务归属或评分规则失效的 409 提供重新加载操作，不自动重复提交。
- [ ] `/api/uploadFile` 仍只是解析任务树预览，不把 Excel 预览成功当作项目任务已经入库。

若新增子项维护入口，还需覆盖当前 `GET /api/project/item/{projectId}`、`POST /api/project/item`、`DELETE /api/project/item/{id}`。当前 POST/DELETE 维护方法返回 `void`，不能臆造 `code=200` 的统一成功 Body；接入时分别处理实际成功空响应与错误 JSON。

## 7. T-05 / T-06：成绩查询与修改权限

| 功能 | 接口 | 当前范围 |
|---|---|---|
| 旧成绩查询 | `GET /api/project/subTaskScore` | 保留兼容，已废弃，推荐新查询 |
| 新成绩查询 | `GET /api/project/subTaskScores` | 有效 admin 可查看全学院；有效 dean 仅查看本学院 |
| 修改成绩 | `PUT /api/project/subTaskScore` | 必须有有效 dean，且只能修改当前所属学院；只有 admin 不获得修改权 |

多角色 admin + dean 可以查看全学院，但修改仍受 dean 的本学院范围限制。clerk 不因与 dean 权重相同获得成绩权限；前端菜单和按钮使用服务端权限信息，不能按中文角色名或“权重 ≥ 某数”自行放行。

涉及 `subexamineupdate`、`attachdowload`，请求成功时成绩列表仍在 `data.list/data.total`。

- [ ] 查询 403 展示后端提示；院长筛选其他学院返回成功空列表时，显示空态。
- [ ] 不将目标 `userId` 当成当前操作人；不新增客户端角色/学院授权参数。
- [ ] 成绩修改保留对象数组格式，包含 `projectId/taskId/userId/score`；任务 ID 沿用后端字符串，不转成 JS 数值。
- [ ] `subexamineupdate` 约第 244、285 行的 PUT 目前未等待请求就提示“分数修改成功”，必须改为响应成功后再提示和关闭弹窗。
- [ ] 当前批量 `modify()` 在仅选中一行时可能构造空数组，应使单条和多条都正确生成待修改数组；空选中不提交。
- [ ] 400、403、409 时保留待修改内容；409 提示结果或任务失效并刷新。
- [ ] 批量越界或失效会导致整批拒绝，不显示部分保存成功。

## 8. T-07 / T-08 / T-09：Word 导出

接口不变：`POST /api/evidence/exportToWord`。已有调用在 `src/views/attachdowload/index.vue` 的 `DownloadWord()`（约第 326 行）。

- [ ] 保留请求中的 `evidence` 为 JSON 数组的字符串，例如 `"[\"/pdf/file/a.pdf\",\"/pdf/file/b.pdf\"]"`；不是逗号拼接路径，也不是提交浏览器完整下载 URL。
- [ ] 空证据、非 PDF 等在页面提前提示；后端负责最终输入和路径校验。
- [ ] 当前页面发出请求后立即提示“下载成功”，应改为有效附件获取后提示“文件已生成/已开始下载”，错误时不提示成功。
- [ ] 按第 2.3 节处理 JSON Blob，避免把错误信息下载为 Word 文件。
- [ ] 下载中禁用按钮，完成或失败后恢复；503 繁忙时展示 `Retry-After`，不自动循环请求。
- [ ] 413 提示分批导出；422 提示 PDF 无效或不可转换；其他错误显示安全的服务端 `msg`。
- [ ] 全页转换耗时可能增加：现有 `$downs` 的 300 秒等待不是本次必须调整的项，但需要核对部署网关超时能覆盖后端默认 60 秒生成预算及传输。
- [ ] 回归多 PDF、空白页、横向页的页序和等比排版。产物仍是 PDF 页面图片嵌入 DOCX，不宣传为 OCR 或可编辑文本转换。

当前默认最多 20 项、总 50 页；这些是可配置后端预算。可作说明提示，但前端不把默认值宣称为永久固定接口限制。

## 9. T-10：优秀项目考核列表

位置：`src/views/projectassesslist/index.vue`。接口仍为 `GET /api/project/assess/list`。

成功结构保持**顶层** `code/msg/total/list`，补充 `page/limit/totalPage`；不要改成 `res.data.list`。

```json
{
  "code": 200,
  "msg": "success",
  "page": 2,
  "limit": 10,
  "total": 25,
  "totalPage": 3,
  "list": []
}
```

上例仅示意字段结构，真实正常第 2 页应返回对应记录。

- [ ] 修正多个请求后的 `if (!result.code == 200)`：逻辑非会先执行，不能正确判断业务失败。
- [ ] 每次分页成功更新 `total` 和当前页记录，不只在首次请求更新总数。
- [ ] 搜索、重置及改变页大小时，同步 `current/typeInfo.page/size/typeInfo.limit`，从第 1 页开始；当前只重置 `typeInfo.page` 可能造成分页控件与数据不一致。
- [ ] 正常翻页替换当前页数据；避免把服务端分页结果继续按无限滚动方式累加。
- [ ] 参数满足 `page≥1`、`1≤limit≤100`；项目/角色 ID 选中时为正数，status 仅 0/1；空的可选 ID 省略。
- [ ] 所有请求入口使用同一时间格式，检查开始时间不晚于结束时间；筛选仍是“项目覆盖查询区间”，不要解释成任意时间重叠。
- [ ] 直接展示服务端 `rank`；它是当前权限和筛选全集的顺序编号，第 2 页继续编号，同分不并列。
- [ ] 保留服务端排序 `score DESC, projectId ASC, userId ASC`，不在当前页另做本地重排或重新排名。
- [ ] 空页与超页有明确空态，超页仍保留真实 total；权限或名单错误显示 `msg`，不能包装成正常无数据。
- [ ] 原导出发送 `projectAssessList`，改为服务器分页后仍仅是当前页。界面应明确“导出当前页”；需要导出全部筛选结果时另行设计，不把 limit 提到 100 以上获取全量。

T-10 的考核列表权限仍是它原有的权重分支，没有随 T-05/T-06 统一为 admin/dean 策略。前端不能把子任务成绩的管理员全学院权限直接套到这个列表。

## 10. T-12：审批重复提交与业务冲突

位置：`src/views/examinedetails/index.vue` 的 `submitApprove()`；接口仍为 `POST /api/submit/approve`。

| 返回 | 页面处理 |
|---|---|
| 成功 `code=200` | 显示成功，关闭打回弹窗并按现有流程返回 |
| HTTP 400 + `code=400` | 参数校验提示；`isFlag` 只能 0/1，当前约定 0 提交、1 打回 |
| HTTP 403 + `code=403` | 无流程审批权限，显示 `msg`，保留登录态 |
| HTTP 409 + `code=409` | 重复有效审批或任务失效，显示 `msg`，提供刷新列表/详情 |

- [ ] 增加提交中状态，防止按钮重复点击，结束后恢复。
- [ ] 请求失败时不关闭弹窗、不 `$router.back()`，不读取不存在的 `result.code`。
- [ ] 409 不自动重发、不当成审批成功；超时后先查询有权限的列表/详情确认结果。
- [ ] 不由前端指定审批人、步骤、记录有效状态来决定是否能重复提交；这些字段由服务端处理。
- [ ] 同一步骤多审批人可分别提交；不将一人的已审批状态当作整个步骤无人可再审批。
- [ ] 回归“打回 → 教师重新提交 → 再审批”的完整流程，验证列表、详情及按钮状态同步更新。

## 11. 不需要直接修改前端接口的 TODO

| 编号 | 后端改动 | 前端安排 |
|---|---|---|
| T-01 | 课表同步失败保留旧数据、校验与事务保护 | 现有 `/courseSchedule/list` 读取不改路径；回归 `digitaldoor2d` 和 `eboard`。本前端未找到手动刷新调用，不强制新增按钮 |
| T-02 | OA 凭据外置及客户端错误语义 | 不需要前端新增 OA Key/Secret 配置或直接调用 OA；回归原业务失败提示 |
| T-11 | 提醒流程的残留 TODO 注释完善 | 不需要新增接口或参数；提醒功能按原接口回归 |
| T-13 / T-14 | 类说明完善 | 不需要前端功能改动 |

若后续新增手动课表同步入口，当前是 `GET /api/courseSchedule/refresh`：成功返回 `data` 同步明细，失败返回业务错误 `msg`，**失败响应当前没有附带完整同步明细**。不要直接照旧方案示例假定所有失败均能读取 `data.executed/data.success`。

## 12. 联调验收清单

- [ ] 真正的 HTTP 403、409 均显示服务端原因，Token 和登录态保留；401 能恢复登录。
- [ ] 教室主键与 SN 不同的样例中，查询、新增、批量安排及总览标识都对应正确教室。
- [ ] 批量删除使用 `/api/exam/batchDelete`，Network 中可见 JSON ID 数组 Body；单条删除继续有效。
- [ ] 批量新增不同教室成功；同教室数据库冲突、批次内冲突、端点相接均准确提示且表单保留。
- [ ] 批量修改支持仅修改指定字段，其他字段保留；不存在 ID、重复 ID、冲突等按实际响应处理。
- [ ] 考试操作成功后记录、总览和班牌更新；无安排时不继续展示旧内容。
- [ ] 巡查导入覆盖全部成功、跳过坏行、整份拒绝及网络失败，计数和行号正确。
- [ ] 成绩查看覆盖 admin、dean、clerk、无角色及学院配置异常；评分覆盖本学院成功、跨学院整批拒绝。
- [ ] 单条选中和多条选中成绩修改均提交非空数组；服务端拒绝时不提前报成功。
- [ ] Word 导出覆盖多页成功、非法材料、超限、繁忙及 JSON Blob，错误时不生成伪附件。
- [ ] 考核列表覆盖第 2 页排名、筛选后重置、页大小变化、空页、超页及当前页导出。
- [ ] 新增子项管理后覆盖首次 MERGE、相同请求跳过、403、冻结/待审核 409；原任务和其他来源任务保留。
- [ ] 审批覆盖成功、重复提交 409、无权限 403、非法标志 400、超时确认及打回重审。

## 13. 后端核对来源

接口和字段核对位置（相对于后端根目录）：

- `soft_main/src/main/java/com/hnkjzyxy/ab/controller/ExamController.java`
- `soft_service/src/main/java/com/hnkjzyxy/ab/service/impl/ExamServiceImpl.java`
- `soft_model/src/main/java/com/hnkjzyxy/ab/model/Exam.java`、`model/Classroom.java`
- `soft_main/src/main/java/com/hnkjzyxy/ab/controller/ScheduleController.java`、`CourseScheduleController.java`
- `soft_model/src/main/java/com/hnkjzyxy/ab/vo/CheckResultImportResult.java`、`ProjectTaskImportResult.java`
- `soft_model/src/main/java/com/hnkjzyxy/ab/params/ProjectItemImportParam.java`
- `soft_main/src/main/java/com/hnkjzyxy/ab/controller/ProjectController.java`、`FlowController.java`
- `soft_service/src/main/java/com/hnkjzyxy/ab/service/impl/ProjectTaskImportServiceImpl.java`、`ProjectTaskGuardImpl.java`、`ResultServiceImpl.java`
- `soft_service/src/main/java/com/hnkjzyxy/ab/service/security/ResultPermissionPolicy.java`
- `soft_service/src/main/java/com/hnkjzyxy/ab/service/support/EvidenceWordService.java`
- `soft_common/src/main/java/com/hnkjzyxy/ab/handler/GlobalExceptionHandler.java`、`result/ApiResult.java`

主要改动记录：

- [TODO 总清单](项目TODO清单分析与解决方案.md)
- [T-01～T-03 修复与验收记录](T-01-T-03修复与验收记录.md)
- [T-04 项目子项导入方案](T-04-项目子项导入任务与原有数据清理-完整解决方案.md)
- [T-05/T-06 权限方案](T-05-T-06-权限校验空指针与角色判定口径不统一-完整解决方案.md)
- [T-07/T-08/T-09 实施记录](T-07-T-08-T-09-PDF转Word工具-实施与验证记录.md)
- [T-10 实施记录](T-10-优秀项目考核列表-实施与验证记录.md)
- [T-11～T-14 实施记录](T-11-T-12-T-13-T-14-实施与验证记录.md)
