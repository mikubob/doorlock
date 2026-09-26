# 项目长期约定（assessment-back / assessment）

## 接口文档：ApifoxHelper 注释规范

团队用 **ApifoxHelper** 把接口推送到 Apifox。该插件**只解析 Javadoc 块注释 `/** */`，不解析行注释 `//`**。

映射规则：

| 位置 | 作用 |
|---|---|
| 类级 Javadoc 第一行 | Apifox 目录名 |
| 方法级 Javadoc 第一行 | 接口名 |
| `@param` / `@return` | 参数说明 / 响应说明 |
| 实体 / VO / DTO 字段 Javadoc | 请求体、响应体字段说明 |

**约定：所有 Controller 的类与方法、所有请求/响应模型的字段，必须写 Javadoc，禁止用 `//` 当文档注释。**
方法体内的实现注释仍用 `//`，不要转成 Javadoc。

目录命名统一为「XXX管理」，**不带** `Controller` / `控制器` 后缀。

## 构建环境

- `pom.xml` 里 `java.version=1.8`，**必须用 JDK 1.8 编译**：
  `JAVA_HOME=D:/CodeEnvironment/JDK1.8 mvn -B compile -DskipTests`
- 用 JDK 21 会因 Lombok 版本过旧报 `NoSuchFieldError: JCTree$JCImport.qualid`（与代码无关）。
- `-o` 离线模式缺 maven-resources-plugin，需联网构建。

## 环境注意事项

- Windows 下 Write/Edit 偶发 `EBUSY: resource busy or locked`（IDE 索引占用），**直接重试即可**。
- 仓库 `target/` 目录被 git 跟踪，`git diff` 会混入大量 class 变更，核对改动时用 `git diff -- <具体文件>`。

## 文档产出约定

- 门锁子系统的设计文档统一放在仓库 `doc/` 下。
- **两套互斥路线，按约束选用，切勿混用**：

  **路线 A｜可改表（5 份）**
  - `doc/智能班牌门锁系统-现状分析与功能规划.md` —— 只读分析结论（问题 65 条 + 功能缺失 + SDK 对照 + 路线图 + 建议 DDL/接口）。
  - `doc/智能班牌门锁系统-优化实施方案-v2（可改表版）.md` —— 主方案：含 2.2 节数据库变更摘要、授权六级判定（灰度→超管→管理员→授权表→排课回退`SKRQ`→拒绝）、F-1~F-6 功能、IMP-01~37 任务卡、P0-A/P0-B/P1/P2/P3 分批（P0-A 与 P3 为零 DDL）、灰度开关 `lock.auth.enabled`、风险登记册 RK-01~14。
  - `doc/智能班牌门锁系统-数据库表变更清单.md` —— **「数据库到底改了什么」的唯一权威**：7 张新表完整 DDL + 4 张存量表字段级前后对照 + 15 个新增列 + 3 处类型变更 + 4 处数据订正 + V1~V15 脚本全文 + 逐支回滚 + 上线 Checklist + 待确认项。
  - `doc/智能班牌门锁系统-接口文档-v3（可改表版）.md` —— 12 章、8 大模块、扩展字段直接对应数据库列。

  **路线 B｜不改表（2 份）**
  - `doc/智能班牌门锁系统-优化实施方案.md` —— 零 DDL：代码修复 + Redis 承载 + 复用 `remarks`/`task_details` 列。
  - `doc/智能班牌门锁系统-接口文档-v2.md` —— 扩展信息走 `remarks` JSON。

  > 历史遗留：`doc/智能班牌门锁系统-具体实施方案.md`（已被 v2 取代，已删除）。
- SQL 迁移脚本规划目录（**仅"可改表"路线使用**）：`soft_main/src/sql/migration/`（`V{序号}__{描述}.sql`，只增不改）。

### 可改表路线的关键执行顺序（硬依赖，勿错）
1. **V13 必须先于 V11** —— `sys_classroom.board_sn` 有 2 行空串（id=308/310），直接加 `uk_board_sn` 必报 1062。
2. **V14 必须在 V8 之后** —— 先把 `remarks` 里的通道号搬到新列 `channel`。
3. **V9 必须与 `SwitchRecordMapper.java` 改造同批发布** —— 该 Mapper 有 7 处读接口，否则开锁记录页 500。
4. V9 执行前必须清洗 `sys_operation_log.user_id` 非数字值（要改 `int`）。

### 可改表路线的数据库基线（dump 与线上不一致，务必先 `DESC` 核对）
- dump 里 `sys_lock_info` 是 `lock_id/ip_address/sn_code/port_number/switch_status/college/classroom/remarks`，**无 `JSH`**；但 `SmartLockMapper` 全部 5 处 SQL 都在用 `JSH` 联表 → 线上必有，dump 滞后。
- `sys_classroom`（`sys_classroom.sql` L24）：`id/JSH/SKDD/XQMC/JZWMC/floor/board_sn`，**无 `switch_status`**（`Classroom` 实体有 → 需 `@TableField(exist=false)`）。
- `sys_operation_log`（L4319）：仅 5 列、仅主键索引；`user_id` 是 `text`。
- `sys_schedule_task`（L13445）：已有 5 个索引（`idx_user_id`/`idx_task_status`/`idx_created_time`/`idx_lock_id`/`idx_count_day`）。
- `sys_course_schedule`（L2738）：`ZC`/`XQJ`/`SKJC`/`JSH`/`JGH`/`SKRQ`(yyyyMMdd)/`KKXND`/`KKXQM` 均为 varchar。
- `sys_lock_password`：**全仓库无建表脚本**（线上手工建的表），实体 `LockPassword` 有 `@TableName`。
- `sys_menu`（L3613-3617 数据）：101 `sys:lcok:op` 拼写错；102 与 103 同 path 同 component（102 `status=0`），103 应删；当前 `AUTO_INCREMENT=108`，新菜单用 104~107 且 `parent_id=98`（不是 0）。
- `sys_flow`/`sys_flow_task` 可复用于借用审批→临时授权。

### 排课回退用 SKRQ 而非 ZC+XQJ
`sys_course_schedule.SKRQ` 是上课日期（`yyyyMMdd`），教务已把单双周/调课/停课烘焙进去。P0 直接比对 `SKRQ = 今天` 即可，无需周历推算；`ZC`+`XQJ` 的推算留给课表联动生成任务时用（配合新增 `sys_term_calendar`）。

## 已知代码问题（待处理，未改动）

- 类名 `SamrtLockController` 拼写错误（应为 `SmartLockController`）。
- `AuthController` 有两个同名重载 `getUserInfo`，在 Apifox 中易混淆。
- `ScheduleController` 含 `/debug/userInfo` 临时调试接口，上线前应移除。
