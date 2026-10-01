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
- 仓库 `target/` **未被 git 跟踪**（`.gitignore` 有 `**/target/`）。`mvn clean/package` 不会污染 `git status`，可放心执行。

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

## 课表同步（T-01，2026-09-29 已实现，零 DDL）

- 方案文档：`doc/T-01-课表同步清空风险-完整解决方案.md`。
- **铁律：`sys_course_schedule` 的同步禁止 `TRUNCATE`**（DDL 隐式提交、无法回滚），必须「先拉取 → 校验 → 事务内 `DELETE` + `saveBatch` 整体替换」。`CourseScheduleMapper.truncateTable()` 已**整体删除**，不要加回来。
- 所有同步入口（cron / startup / 手动 `/courseSchedule/refresh`）统一走 `CourseScheduleService.sync(source)` 单一入口，内部自带 Redis 分布式锁 + 四道数据量闸门（0 条 / `min-rows` / `shrink-guard-ratio` / `page-size` 截断）。
- 配置项在 `schedule.sync.*`（`ScheduleSyncProperties`，soft_common）。**`sync-on-startup` 生产必须 false**。
- **做 Redis 分布式锁必须用 `StringRedisTemplate`**：项目的 `RedisTemplate<String,Object>` 走 Jackson JSON 序列化，值带引号，Lua 里比对 token 会失败 → 锁永不过期。`RedisUtils` 实际注入的也是 StringRedisTemplate。
- `SoftApplication` 上已有 `@ConfigurationPropertiesScan("com.hnkjzyxy.ab")`，新增配置类**只写 `@ConfigurationProperties`，不要再加 `@Component`**（同类型 Bean 会注册两次，按类型注入变歧义）。参照 `AuthUrlConfig`。
- `sys_course_schedule` **只有主键 `id`，无业务唯一键**（要做增量 upsert 必须先加唯一索引）；不再 TRUNCATE → `AUTO_INCREMENT` 持续增长，按现量估算可用约 1.9 万年，不处理。
- `OaRequestAPIUtils.getClassBoardData` **吞异常返回 `null`**（无法区分「网络失败」与「真的没数据」），且 URL 写死 `per_page=1000` 会静默截断。改造归 T-02 / R-04。
- 依赖关系：`soft_service` → `soft_mapper` + `soft_common`（**不依赖 soft_main**），因此被 service 使用的配置类要放 soft_common / soft_model。

## 资源文件位置约定（重要）

**所有资源统一收敛在 `soft_main/src/main/resources/`**，Java 代码才按模块拆分。

- 先例：`CourseScheduleMapper.java` 在 `soft_mapper`，`CourseScheduleMapper.xml` 在 `soft_main/src/main/resources/mapper/`，配置写 `classpath:mapper/*.xml`。
- 因此 `soft_common` 的代码可以读取 `soft_main` 下的资源：**classpath 在运行期合并**（fat jar 内统一落在 `BOOT-INF/classes/`），与编译期模块依赖无关。已用 `ClassPathResource#exists` 探针实测：`soft_common/target/classes` + `soft_main/target/classes` → `true`；仅 `soft_common` → `false`。
- 代价：这类工具类**运行期依赖 soft_main**，单模块复用会快速失败（已在 `RedisLockUtils` 里抛 `IllegalStateException`，不静默降级）。要恢复自包含，只需挪文件、代码零改动。
- `soft_main/pom.xml` 自带 `<resources>` 块，**在本模块内覆盖父 POM**：`src/main/resources` 下所有文件（含 `.lua`）都按 `filtering=true` 处理。脚本内不要出现 `${}` / `@...@` 占位符。

## Redis Lua 脚本与「等待」的写法约定

- **Lua 脚本一律放独立文件**：`soft_main/src/main/resources/lua/*.lua`（按上面的资源约定，**不**放 soft_common）。加载用
  `RedisScript.of(new ClassPathResource("lua/xxx.lua"), Long.class)`（路径是 classpath 相对路径，与物理模块无关）。
  **不要**用 `new DefaultRedisScript<>(text)` 再 `setLocation(...)` —— `sha1` 只在它作为 Spring Bean 时由 `afterPropertiesSet()` 初始化，手工 `new` 走不到，EVALSHA 会出错。
- **禁止用 `Thread.sleep` 做等待或延迟**，按意图选工具：等容器就绪 → `ApplicationRunner` / `ApplicationReadyEvent`；等外部依赖 → 带退避的重试 / `RetryTemplate`；稍后异步 → 线程池 / 延迟队列；周期执行 → `@Scheduled`。

## 已知代码问题（待处理，未改动）

- 类名 `SamrtLockController` 拼写错误（应为 `SmartLockController`）。
- `AuthController` 有两个同名重载 `getUserInfo`，在 Apifox 中易混淆。
- `ScheduleController` 含 `/debug/userInfo` 临时调试接口，上线前应移除。
