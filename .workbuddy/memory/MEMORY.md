# 项目长期约定（assessment-back / assessment）

> 详细过程与一次性排查记录在 `.workbuddy/memory/YYYY-MM-DD.md`，此处只留**长期有效、必须遵守**的规则。

## 1. 构建与环境
- **必须 JDK 1.8 编译**：`JAVA_HOME=D:/CodeEnvironment/JDK1.8 mvn -B compile -DskipTests`。用 JDK 21 会因 Lombok 过旧报 `NoSuchFieldError: JCTree$JCImport.qualid`（与代码无关）。`-o` 离线缺 maven-resources-plugin，需联网。
- Windows 下 Write/Edit 偶发 `EBUSY: resource busy or locked`（IDE 索引），**直接重试**。
- `target/` 未被 git 跟踪，`mvn clean/package` 不污染 `git status`。
- 本地：profile 默认 `dev`；MySQL `localhost:3306/soft_manage`（root/1234，客户端在 `D:/CodeEnvironment/MySQL/mysql-8.0.34-winx64/bin`）；Redis `localhost:6379`/123456；服务端口 9696。

## 2. 数据库基线（★ dump ≠ 环境，动手前先 `DESC` 核对）
- **`sys_lock_info` 的教室列：线上叫 `JSH`，dump 文件里叫 `classroom`（★ 已实测确认，2026-10-01）**
  两边都是**第 7 列**，是同一列改名。线上 `JSH varchar(50) NULL`；dump 是 `classroom text NULL`。
  **本地库已于 2026-10-01 执行 `CHANGE COLUMN classroom JSH varchar(50) NULL` 改好了**，但 `soft_manage/src/sql/` 下的 **5 个 dump 文件仍是旧结构**。
  `SmartLockMapper`（5 处 SQL，含 `INSERT ... JSH ...`）、`LockInfo.@TableField("JSH")`、`ClassroomMapper.getClassroomList` 的 JOIN 全依赖 `JSH` → 用旧 dump 建的库必报 `Unknown column 'sli.JSH' in 'on clause'` / `'l.JSH' in 'field list'`。
  **这是环境问题，不是代码问题 —— 不要为此改代码。**
- **★ `soft_main/src/sql/` 下的 dump 不能当建库基线**（2026-10-01 逐一核对）：`soft_manage.sql`(2025-09-29,来自 192.168.1.214,25 表)、`soft_manage20251104.sql`(2025-11-04,**来自线上 10.255.12.53**,26 表)、`soft_manage_all.sql`(2026-09-21,**来自 localhost**,30 表)、`sys_classroom.sql`(2026-05-20)、`sys_exam.sql`(2026-06-08)。
  全部早于线上 `classroom`→`JSH` 的改名，且**全部没有 `sys_lock_password`**（线上有，手工建，仓库从无建表脚本）。`soft_manage_all.sql` 表清单与本地库曾完全一致（本地库就是它导入的）→ 旧结构被一路复制。
  新人建库前**必须按线上 `information_schema` 核对**。`migration/` 目录目前**尚不存在**（仅文档规划），V 序号暂空。
- **`sys_classroom.board_sn`：线上 `int`，dump `varchar(50)`**（旧 schema）。线上 52 行无空值，dump 里 52 行有 50 行空串。
  > 推论：文档里「V13 必须先于 V11（board_sn 有 2 行空串会触发 1062）」的约束来自本地 dump，**线上该列是 int 不可能是空串**，执行前需按线上实测重新评估。
- **`sys_student_info`** 的 `web_total/java_total/program_total/database_total`：线上 `float(255,1)`，dump `float(255,0)`（dump 建的库会丢小数位）。
- `sys_classroom`：`id/JSH/SKDD/XQMC/JZWMC/floor/board_sn`，**无 `switch_status`**；`Classroom.switchStatus` 未加 `@TableField(exist=false)` → 用 MP 实体查询会炸。
  注：`sys_classroom.JSH` 是 `utf8mb4_general_ci`，而 `sys_lock_info.JSH` 是 `utf8mb4_0900_ai_ci` → JOIN 上的 `COLLATE` 是**必要的**，不是冗余。
- 其它线上↔本地结构差异（2026-10-01 实测）：仅线上有表 `sys_lock_password`（全仓库无建表脚本）、`sys_student_info_2024`；仅本地有表 `sys_task_back`；`sys_student_info` 的 `web_total/java_total/program_total/database_total` 线上是 `float(255,1)`、本地是 `float(255,0)`（本地会丢小数位）。
- **核对线上结构的办法**：prd 数据源见 `application-prd.yml`（10.255.12.53/soft_manage），本机可直连，只做 `SHOW COLUMNS` / `information_schema` 只读查询。
- `sys_operation_log`：仅 5 列、仅主键索引；**`user_id` 是 `text`**。
- `sys_schedule_task`：已有 5 个索引（`user_id`/`task_status`/`created_time`/`lock_id`/`count_day`）。
- `sys_course_schedule`：`ZC`/`XQJ`/`SKJC`/`JSH`/`JGH`/`SKRQ`(yyyyMMdd)/`KKXND`/`KKXQM` 均 varchar；**只有主键 `id`，无业务唯一键**（upsert 前必须先加唯一索引）。
- `sys_lock_password`：**全仓库无建表脚本**（线上手工建），实体有 `@TableName`。
- `sys_menu`：101 `sys:lcok:op` 拼写错；102/103 同 path 同 component（102 `status=0`），103 应删；`AUTO_INCREMENT=108`，新菜单用 104~107 且 `parent_id=98`（不是 0）。
- `sys_flow`/`sys_flow_task` 可复用于借用审批→临时授权。
- 排课回退用 **`SKRQ = 今天`**（单双周/调课/停课已由教务烘焙进 `SKRQ`），不要用 `ZC`+`XQJ` 推算；周历推算留给课表联动（配合新增 `sys_term_calendar`）。

## 3. 门锁子系统文档（`doc/`）
**两套互斥路线，按约束选用，切勿混用**：
- **路线 A｜可改表**：`现状分析与功能规划.md`（只读分析）→ `优化实施方案-v2（可改表版）.md`（主方案：授权六级判定 灰度→超管→管理员→授权表→排课回退→拒绝、F-1~F-6、IMP-01~37、P0-A/P0-B/P1/P2/P3、灰度开关 `lock.auth.enabled`、RK-01~14）→ **`数据库表变更清单.md`（「DB 到底改了什么」的唯一权威）** → `接口文档-v3（可改表版）.md`。文件名前缀均为「智能班牌门锁系统-」。
- **路线 B｜不改表**：`智能班牌门锁系统-优化实施方案.md`（零 DDL，代码修复 + Redis 承载 + 复用 `remarks`/`task_details`）、`…-接口文档-v2.md`（扩展信息走 `remarks` JSON）。
- 迁移脚本目录（**仅路线 A**）：`soft_main/src/sql/migration/`（`V{序号}__{描述}.sql`，只增不改）。
- **路线 A 硬依赖顺序**：① **V13 先于 V11**（`board_sn` 有 2 行空串 id=308/310，直接加 `uk_board_sn` 必报 1062）；② **V14 在 V8 之后**（先把 `remarks` 里的通道号搬到新列 `channel`）；③ **V9 必须与 `SwitchRecordMapper.java` 改造同批发布**（该 Mapper 有 7 处读接口，否则开锁记录页 500），执行前先清洗 `sys_operation_log.user_id` 非数字值。

## 4. ApifoxHelper 注释规范
插件**只解析 Javadoc 块注释 `/** */`，不解析 `//`**。类级 Javadoc 首行=Apifox 目录名，方法级首行=接口名，`@param`/`@return` 与实体字段 Javadoc 分别做参数与字段说明。
**约定：所有 Controller 的类与方法、所有请求/响应模型字段必须写 Javadoc**；方法体内的实现注释仍用 `//`。目录命名统一「XXX管理」，**不带** `Controller`/`控制器` 后缀。

## 5. 课表同步（T-01，已实现，零 DDL）
- 方案：`doc/T-01-课表同步清空风险-完整解决方案.md`。
- **铁律：禁止 `TRUNCATE` `sys_course_schedule`**（DDL 隐式提交、无法回滚）。必须「先拉取 → 校验 → 事务内 `DELETE` + `saveBatch`」。`CourseScheduleMapper.truncateTable()` 已整体删除，**不要加回来**。
- 全部入口（cron / startup / 手动 `/courseSchedule/refresh`）统一走 `CourseScheduleService.sync(source)`，内含 Redis 分布式锁 + 四道闸门（0 条 / `min-rows` / `shrink-guard-ratio` / `page-size` 截断）。
- 配置在 `schedule.sync.*`（`ScheduleSyncProperties`，soft_common）。**`sync-on-startup` 生产必须 false**。
- **Redis 分布式锁必须用 `StringRedisTemplate`**：项目 `RedisTemplate<String,Object>` 走 Jackson，值带引号，Lua 比对 token 会失败 → 锁永不过期。`RedisUtils` 实际注入的也是 StringRedisTemplate。
- `SoftApplication` 已有 `@ConfigurationPropertiesScan("com.hnkjzyxy.ab")` → 新配置类**只写 `@ConfigurationProperties`，不要加 `@Component`**（重复注册致按类型注入歧义）。参照 `AuthUrlConfig`。
- `OaRequestAPIUtils.getClassBoardData` **吞异常返回 `null`**，URL 写死 `per_page=1000` 会静默截断 → 归 T-02 / R-04。
- 模块依赖：`soft_service` → `soft_mapper` + `soft_common`（**不依赖 soft_main**），被 service 使用的配置类放 soft_common / soft_model。

## 6. 资源文件位置（重要）
**所有资源统一收敛在 `soft_main/src/main/resources/`**，Java 代码才按模块拆分（先例：`CourseScheduleMapper.java` 在 soft_mapper，`.xml` 在 soft_main，配置 `classpath:mapper/*.xml`）。
- classpath 运行期合并 → `soft_common` 的代码能读 soft_main 下的资源（fat jar 都在 `BOOT-INF/classes/`）。已用 `ClassPathResource#exists` 实测。
- 代价：这类工具类**运行期依赖 soft_main**，单模块复用会快速失败（`RedisLockUtils` 抛 `IllegalStateException`，不静默降级）。要自包含只需挪文件。
- `soft_main/pom.xml` 自带 `<resources>` 块**覆盖父 POM**：`src/main/resources` 下所有文件（含 `.lua`）均 `filtering=true` → 脚本内**不要出现 `${}` / `@...@`**。

## 7. Redis Lua 与「等待」写法
- **Lua 一律独立文件** `soft_main/src/main/resources/lua/*.lua`（不放 soft_common），加载用 `RedisScript.of(new ClassPathResource("lua/xxx.lua"), Long.class)`。
  **不要** `new DefaultRedisScript<>(text)` + `setLocation(...)` —— `sha1` 只在作为 Spring Bean 时由 `afterPropertiesSet()` 初始化，手工 `new` 走不到，EVALSHA 会出错。
- **禁止 `Thread.sleep` 做等待/延迟**：等容器就绪 → `ApplicationRunner`/`ApplicationReadyEvent`；等外部依赖 → 带退避重试/`RetryTemplate`；稍后异步 → 线程池/延迟队列；周期 → `@Scheduled`。

## 8. 测试踩坑
- **`@ActiveProfiles("xxx")` 是「替换」而非「叠加」** → `application-dev.yml` **完全不加载**，测试 profile 必须自带数据源、`mybatis-plus.*`、Redis、`yue.url`、`absolute.jwt.*`、`upload.*`、`oa.*`、`schedule.sync.sync-on-startup:false` 等全部关键配置，否则以「表不存在」「占位符无法解析」报错，极难定位。
- **MP 3.2.0 的 `table-prefix` 必须在当前 profile 里配**，缺失时实体解析成 `check_result` 而非 `sys_check_result`。排查一律用运行时探针（`TableInfoHelper.getTableInfo(...).getTableName()` + `env.getProperty(...)`）。
- **`soft_main/pom.xml` 硬写 `<skipTests>true</skipTests>`** 会压掉命令行 `-DskipTests=false` → 跑测试需临时改 `${skipTests}`，**用完还原**。
- 启动完整容器：`ScheduleLoad` 启动期全表查 `sys_schedule_task`，**表不存在则启动失败**（需建空表）；必须设 `sync-on-startup: false`。
- **当前仓库无任何 T-03 测试代码**（2026-10-01 删除 31 条用例 + `src/test/resources/`），**回归保护缺失**；要重建先读 `doc/T-03-Excel巡查导入映射失效与解析中断-完整解决方案.md` 第三/四轮补充。
- 删自己写的测试前先 `git ls-files | grep test`（已跟踪=原有）对比 `git status`（`??`=新增）—— `src/test` 下混有原作者遗留的 `TestMain.java`，**不要 `rm -rf` 整个 `src/test`**。

## 9. MyBatis-Plus 自动填充（★ 已修，勿退回）
`MyMetaObjectHandler`（soft_common/.../hanlder/）**禁止**用 `metaObject.hasSetter(name)` 判字段存在 —— **`hasSetter` 对 `Map` 恒 true**，而 `updateById`/`update(entity,wrapper)` 传进来的是 `MapperMethod.ParamMap`（键 `param1`/`et`），`getSetterType` 对缺失 key 抛 `BindingException: Parameter 'xxx' not found. Available parameters are [param1, et]`。
→ 凡「实体没有 `updateTime` 字段 + 调 `updateById`」必炸。必须走 `hasWritableTimeField()`：先 `getOriginalObject() instanceof Map → false`，再 `hasGetter && hasSetter`。详见 `doc/登录报错-updateTime-Parameter-not-found-根因分析与修复.md`。
复现要点：`SystemMetaObject.forObject(普通 HashMap)` 复现不出来，必须 `MetaObject.forObject(...)` + 真实 `MapperMethod.ParamMap`。

## 10. MP 3.2.0 逻辑删除（★ 已实测）
**3.2.0 的 `GlobalConfig.DbConfig` 没有 `logicDeleteField`** → yml 里 `logic-delete-field: xxx` 是死配置、被静默忽略（三份 yml 已删并加注释）。逻辑删除**只认实体字段的 `@TableLogic`**。
- `@TableLogic=false`：`User`/`CheckResult`/`Exam`/`Message`/`Project`/`SwitchRecord`/`Task`/`ScheduleTask`/`CourseSchedule`；`=true`：`Notice`/`Menu`/`Role`/`StudentInfo`/`Flow`/`FlowTask`/`ResultItem`/`ConstructResult`/`MajorDetails`。
- **业务状态字段不要加 `@TableLogic`**：`Exam.status`(考试进度)/`Message.status`(已读未读)/`User.status`(启用禁用)/`Project.status`。
- `UserMapper` 手写的 `and status = 1` 是显式过滤，与 MP 配置无关。

## 11. 已修点与已知问题
- 登录：`UserDetailsImpl.loadUserByUsername` 调 `UserService.updateLastLogin(userId, date)`（只 `set(last_login)`）。**勿退回 `setLastLogin + updateById`** —— `getUserByName` 带 `@Cacheable` 返回旧快照，全字段写回会覆盖他人刚改的昵称/手机号；且 `User` 表无 `update_time` 列。
- `SwitchRecord` 已加 `@TableName("sys_operation_log")` + `@TableId("switch_id")` + `Serializable` + `equals/hashCode`；`user_id` 库中是 `text` 而实体是 `Integer`，Mapper 全用注解 SQL，比较条件已显式 `CAST(#{userId} AS CHAR)`；根治需改列类型（路线 A）。
- 待处理：类名 `SamrtLockController` 拼写错（应 `SmartLockController`）；`ClassroomMapper.xml` resultMap 里 `boardSn` 重复映射两次；`AuthController` 两个同名重载 `getUserInfo`；`ScheduleController` 含 `/debug/userInfo` 临时接口待删；`soft_main/src/test/.../TestMain` 硬编码作者桌面路径、**无 `@ActiveProfiles` 且直连生产库** → 全量 `mvn test` 必 FAILURE，判断失败归因时先排除它。
- Redis：`RedisConfig#redisTemplate` 的 value 用 Jackson JSON 序列化，而 `RedisUtils` 按 String 使用 → 字符串值带引号。项目**没有**自定义 `stringRedisTemplate` Bean（Boot 已提供），自行添加会触发 `BeanDefinitionOverrideException`；`RedisUtils` 字段虽声明 `RedisTemplate<String,String>`，泛型擦除后**按 Bean 名 `redisTemplate` 注入** → 实际仍是 Jackson 模板。
