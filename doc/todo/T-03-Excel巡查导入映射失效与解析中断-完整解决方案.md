# T-03 Excel 巡查结果导入：字段映射失效、解析易中断 —— 完整解决方案

> 对应问题：`项目TODO清单分析与解决方案.md` 中 **T-03【P1】**
> 原 TODO 位置：`soft_service/src/main/java/com/hnkjzyxy/ab/service/listener/CheckResultDataListener.java:87`
> TODO 原文：`//TODO 这里应该是没有读到数据的，需要排查原因`
> 关联任务卡：**IMP-03**（显式映射 + 容错解析 + 错误行回执，预估 1d，无前置依赖）
> 计划编写时间：2026-10-01
> 本文状态：**已实施（2026-10-01），`mvn -B compile -DskipTests` JDK 1.8 BUILD SUCCESS**
> 编译/验证方式：`JAVA_HOME=D:/CodeEnvironment/JDK1.8 mvn -B compile -DskipTests`
> 数据库变更：**无 DDL**（零改表）
>
> **实施说明（与原计划的差异）**
> 1. **`college` 策略落地为 S1 + 兜底拒绝**：`CheckResultImportProperties.collegeStrategy` 默认 `FROM_UPLOADER`，
>    取上传人 `User#getCollege()`；为空且 `require-college=true` 时**该行记为失败并给出可读原因**（等价于拒绝导入），
>    不再静默写 `null`。原计划中的「空学院 → 抛异常中断整次导入」改为「行级失败 + 回执」，与防线 3 的隔离策略保持一致。
> 2. **`soft_service` 新增 `soft_model` 显式依赖**：`soft_model` 原本经 `soft_mapper` 传递引入（compile scope），
>    显式声明是为表明意图，不改变运行期依赖图。
> 3. **`CheckResultModel` 未改**（沿用推荐的不加 `@ExcelProperty("学院")`），因此 Excel 模板零改动。
> 4. **待实测项 C-2（事务边界）已按最稳妥方式规避**：落库事务统一收在监听器内由
>    `CheckResultImportProperties` 驱动的 `TransactionTemplate.executeWithoutResult` 承担，
>    `CheckResultServiceImpl#uploadCheckResult` 上**不再叠加** `@Transactional`，从根本上排除「内层提交后外层回滚无效」的嵌套陷阱。
> 5. 未按原计划「保留最外层 TransactionTemplate」——该写法与内层事务嵌套会产生误导性语义，已直接移除。
>
> **实施补充（2026-10-01 第二轮，两项遗留已决策并落地）**
> - **`max-rows` 改为整次导入闸门**：原实现把判定放在 `invoke` 里抛 `RowParseException` 是**逻辑错误**——
>   `data.add(...)` 已执行，抛异常只是让 `failCount++`，**超出部分照常入库**（`fail-fast-on-error=false` 时彻底失效）。
>   现移至 `doAfterAllAnalysed`，超限抛 `ImportRejectedException` **放弃整次导入、不写入任何数据**。
> - **整次拒绝在接口层可见**：新增 `ImportRejectedException`。此前 `doAfterAllAnalysed` 抛出的异常被 EasyExcel
>   静默丢弃（仅一条 warn 日志），接口会返回「成功 0 条」的 200 响应，用户看不出被拒绝；
>   现由 `ScheduleController#uploadCourse` 捕获并返回 `code=400` + 拒绝原因。
> - **锚点列序告警**：`invokeHeadMap` 校验「日期」列是否在 `index=1`。因模板由业务方线下维护、列名可能被调整，
>   默认**只告警不拦截**（写入回执 `message`），`strict-head-check=true` 时切严格拒绝。
> - **回执新增 `headRowCount`**，用于核对模板表头行数。
>
> **实施补充（2026-10-01 第三轮：本地端到端验证 —— 用例已按要求移除，仅留结论）**
> - 曾在 `soft_service/src/test/.../listener/CheckResultDataListenerTest.java` 用 EasyExcel 在内存生成**真实 xlsx**，
>   经 `MockMultipartFile` 走完整解析链路；Service 与 `TransactionTemplate` 用内存桩，**不依赖 Spring 容器 / DB / Redis**。
> - **结果：25 条全绿**，覆盖计划书 A/B/C/D 组用例。**该测试代码后续已按要求删除**，结论保留于此。
> - **验证中发现并修复一个真实缺陷**：`isYes()` 原为全等比较，导致「是，第 1 节迟到 10 分钟」这类
>   **带补充描述的写法被判为「否」**，出现 `remark` 写迟到但 `is_late=0` 的自相矛盾。
>   已改为「以『是』/『1』/『Y』开头即判定为是」，并优先排除「否/无/不」开头。
> - 此轮尚未验证真实 MySQL 的事务回滚，由第四轮补齐。
>
> **实施补充（2026-10-01 第四轮：真库验证 —— 用例已按要求移除，仅留结论）**
> - 曾在 `soft_main/src/test/.../CheckResultImportIntegrationTest.java` 启动**真实 Spring 容器 + 真实 MySQL**，
>   连隔离库 `soft_manage_t03test`（**绝不触碰 `soft_manage`**），**6 条全绿**（与上轮合计 31 条）。
>   **该测试代码及配套的 `src/test/resources/`（profile、建库脚本）后续已按要求全部删除**。
> - **C-1 遗留已闭环**：I-2 曾**实测证明事务整体回滚**。为排除「压根没写入也会通过断言」的假阳性，
>   用例用 JDK 动态代理包裹 `saveBatch`，断言批次数 ≥ 2 且每次调用都
>   `TransactionSynchronizationManager.isActualTransactionActive() == true`。
>   实测证据：`实际批次数 = 2，isActualTransactionActive = [true, true]`，
>   异常为真实 MySQL 报错 `Data truncation: Data too long for column 'classes'`，
>   而表行数仍回到导入前 → **第 1 批确实写入过，并被完整回滚**。
>   > ⚠️ **重要**：该结论依赖的测试代码已被移除，**回归保护也随之消失**。
>   > 若日后需要重新验证，须重建测试并注意：启动期 `ScheduleLoad`（`CommandLineRunner`）会全表查
>   > `sys_schedule_task`，隔离库必须建该表（可空表）否则容器启动即失败。
> - **排查中最有价值的一条教训（务必记住）**：
>   `@ActiveProfiles("t03test")` 会**替换**而非叠加 `application.yml` 里的 `spring.profiles.active: dev`，
>   导致 `application-dev.yml` **完全不加载**。后果极隐蔽：数据源、`mybatis-plus.global-config.db-config.table-prefix`
>   全为 `null`，实体 `CheckResult` 被解析成 **`check_result`** 而不是真实的 **`sys_check_result`**，
>   报错却伪装成「表不存在」。
>   → 因此写任何集成测试 profile **必须自带全部关键配置**（数据源 / MyBatis-Plus（含 `table-prefix`）/
>     Redis / 安全白名单 `yue.url` / `absolute.jwt.*` / 上传路径 / `schedule.sync.sync-on-startup:false`），
>     不能假设继承 dev。
> - **补记：MyBatis-Plus 3.2.0 的 `tablePrefix` 确有效**。
>   反编译 `TableInfoHelper` 曾一度显示 `initTableNameWithDbConfig` 只处理 `tableUnderline`/`capitalMode`
>   （该前缀支持在 3.3.x 才完善），但补齐 `table-prefix: sys_` 后 `TableInfo` 实测即解析为 `sys_check_result`。
>   **结论以实测为准**：3.2.0 下前缀可用，只是**必须在当前 profile 里显式配置**。
> - **`soft_main/pom.xml` 的 `<skipTests>true</skipTests>` 硬编码**：会压掉 CLI 的 `-DskipTests=false` /
>   `-Dsurefire.skip=false`。跑测试需临时改为 `<skipTests>${skipTests}</skipTests>`，**用完必须还原**。
> - **顺带发现的两个既有隐患（未修，属其他任务范围）**：
>   1. `com.hnkjzyxy.ab.TestMain` 是**遗留调试类**，硬编码了作者桌面路径（`E:\桌面\...xlsx`）等，
>      3 条用例必然失败；且它**没有 `@ActiveProfiles`**，会直接连**生产库 `soft_manage`** 跑 SQL，
>      建议按 T-14 思路清理或迁到隔离 profile。
>   2. `RedisConfig#redisTemplate` 把 value 序列化器设为 Jackson JSON，`RedisUtils` 却按 `String` 使用，
>      导致字符串值在 Redis 中被写成**带引号的 JSON**（如 `"abc"`）。属既有行为，
>      排查「Lua 里 token 比对不相等」类问题时要留意（与 T-01 分布式锁的教训同源）。


---

## 一、问题定性

原 TODO 注释把现象描述成了「**没有读到数据**」，开发者因此去排查 EasyExcel 有没有解析到行——**方向从一开始就是错的**。数据其实读到了，只是被搬运的过程中**丢了一半**，而丢的方式很安静：不报错、不告警、不留痕。

真正的问题是三层叠加的：

1. **映射层**：`BeanUtils.copyProperties` 因类型不兼容**静默跳过**字段，开发者看到对象里一片 `null`/`0`，误判为「没读到数据」；
2. **解析层**：数值与日期**强转**（`Integer.parseInt` / `SimpleDateFormat.parse`）遇到空值或 `2024/5/5` 这类格式**直接抛异常**，`saveData()` 整体失败，**一批 100 条连坐**；
3. **事务层**：`uploadCheckResult` 只做了「分批保存」，没有「整次导入」的事务语义，第 3 批失败会**留下前两批脏数据**，且统计口径（`count - 1`）本身就是错的。

一句话概括：**这不是「读不到」，而是「读到了但搬丢了，且一旦某行有问题就整批陪葬、还留下一半脏数据」。**

### 1.1 原代码的问题链

```java
// CheckResultDataListener#saveData()（原文）
List<CheckResult> checkResults = data.stream().map(item -> {
    CheckResult checkResult = new CheckResult();
    checkResult.setId(snowFlowUtils.nextId());

    //TODO 这里应该是没有读到数据的，需要排查原因
    BeanUtils.copyProperties(item, checkResult);        // ① 类型不兼容 → 静默不复制

    System.out.println(checkResult + "---?");            // ② System.out 调试残留（R-04）
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    try {
        Date parse = format.parse(item.getDate());       // ③ 只认 yyyy-MM-dd，2024/5/5 直接抛
        checkResult.setDate(parse);
    } catch (ParseException e) {
        throw new RuntimeException("日期格式不正确，请使用2024-05-05这种格式");  // ④ 整批中断
    }

    checkResult.setShouldArrival(Integer.parseInt(item.getShouldArrival()));   // ⑤ 空/“-” → NFE
    checkResult.setArrival(Integer.parseInt(item.getArrival()));               // ⑤
    checkResult.setFoodBringPerson(Integer.parseInt(item.getFoodBringPerson()));// ⑤
    ...
    if (item.getIsLate().length() > 2) { joiner.add(item.getIsLate()); }       // ⑥ NPE（列为空时）
    ...
}).collect(Collectors.toList());
checkResultService.saveBatch(checkResults);                                   // ⑦ 每批独立提交
data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);                   // ⑧ data 重置，count 不清零
```

| 序号 | 风险 | 实际后果 |
|---|---|---|
| ① | `BeanUtils.copyProperties` 类型不兼容静默跳过 | `date`/人数/是否类共 7 个字段**从未被复制**，得默认值 |
| ② | `System.out.println` 打印整行对象 | 日志污染；值全是 `null`/`0`，成为「没读到数据」误判的直接来源（R-04） |
| ③④ | 日期只认 `yyyy-MM-dd` | 中文 Excel 默认输出 `2024/5/5` → 抛异常 → **整批 100 条全部失败** |
| ⑤ | `Integer.parseInt` 无容错 | 单元格留空 / 填 `-` / 含空格 → `NumberFormatException` → **整批失败** |
| ⑥ | `item.getIsLate().length()` 无判空 | 「是否迟到」列为空时抛 NPE → **整批失败** |
| ⑦ | 每 100 条独立 `saveBatch`，外层无整次导入事务 | 第 3 批失败 → 前 2 批**已落库**，形成脏数据 |
| ⑧ | `count` 与 `data` 口径分离 | `count` 只增不减，报告数字与实际入库量不一致 |

### 1.2 根因定位（代码级，可直接核对）

`BeanUtils.copyProperties`（Spring 版）的语义是：**只有当属性名相同、且源类型可赋值给目标类型时才复制，否则静默跳过**。对照两个类逐字段核对：

| 字段 | `CheckResultModel`（Excel 行） | `CheckResult`（实体） | copy 是否生效 | 说明 |
|---|---|---|---|---|
| `date` | `String` | `Date` | ❌ | 类型不兼容 |
| `weeks` / `section` / `classroom` / `classes` | `String` | `String` | ✅ | 正常复制 |
| `arrivalRate` / `discipline` / `foodBringRate` | `String` | `String` | ✅ | 正常复制 |
| `counsellor` / `teacher` / `checkPerson` | `String` | `String` | ✅ | 正常复制 |
| `shouldArrival` / `arrival` / `foodBringPerson` | `String` | `Integer` | ❌ | 类型不兼容 |
| `isLate` / `isViolate` / `isNormal` | `String` | `Integer` | ❌ | 类型不兼容 |
| `college` | 无 | `String` | ❌ | **Model 里没有来源字段** |
| `peopleLeave` | 无 | `int` | ❌ | **Model 里没有来源字段** |

结论：`copyProperties` 这一行**实际只搬了 9 个字符串字段**（`weeks`/`section`/`classroom`/`classes`/`arrivalRate`/`discipline`/`foodBringRate`/`counsellor`/`teacher`/`checkPerson` 中的 String→String 部分），其余 7 个字段（`date` + 3 个人数 + 3 个是否）**全部为默认值**。而紧随其后的 101~120 行又手工给这些字段赋了值——也就是说 **`copyProperties` 这一行既无用（结果被覆盖）又误导（制造了「没读到」的假象）**。

> 这也解释了为什么这个 TODO 长期没被解决：注释把注意力引向了「EasyExcel 读没读到行」，而真实缺陷在「Java 对象之间怎么搬」。

### 1.3 影响面

`sys_check_result` 是**教学巡查**的核心事实表，被下列链路消费（全仓已核对）：

| 消费方 | 位置 | 依赖字段 |
|---|---|---|
| 巡查列表 / 详情 | `ScheduleController#checkResult` → `CheckResultService#getList` | 全字段 |
| 按班级统计 | `CheckResultMapper#selectListByClasses`、`dataStatisticsByClasses` | `classes`/`college` |
| 出勤率与带食物率统计 | `selectByTime`、`selectByTimeAndTeacher`、`getCollegeStatistics` | `arrival_rate`/`food_bring_rate`（**字符串字段，靠 `avg()` 做隐式转换**） |
| 月度缺勤明细 | `getMonthlyAbsenceDetailByCollege`、`getAbsenceClassesByCollegeAndDateRange` | `should_arrival`/`arrival`/`people_leave` |
| 学院维度统计 | `findByDateRangeAndCollege`、`findByCollege`、`getAllColleges` | `college`/`date` |

影响要点：

1. **导入即失败**是最轻的后果（至少数据没脏）。真正危险的是**部分失败**——EasyExcel 每满 100 条就 `saveBatch` 一次，一旦第 3 批抛异常，前 2 批**已经落库**，而接口返回「上传失败」，运维/老师会**重复上传**，于是同一份数据进库两遍（表上**无业务唯一键**，无法去重）。
2. **`college` 无来源**：`CheckResultModel` 里根本没有这个字段，导入的记录 `college` 恒为 `null`。而**几乎所有统计接口都按 `college = #{college}` 过滤** → 导入的数据在学院维度的报表里**完全不可见**，等于「导进去了但查不出来」。
3. **`date` 型字段参与 SQL 的 `BETWEEN` 比较**（`date between #{startTime} and #{endTime}`），一旦日期解析成默认值（`null`）或错值，时间范围统计直接漏数。
4. **`arrival_rate` / `food_bring_rate` 是 `varchar` 却参与 `AVG()`**：MySQL 会做隐式转数，含 `%` 时行为依赖版本与 SQL Mode，属**既有隐患**（本方案不改结构，但见「七、边界」）。

---

## 二、解决方案设计：四道防线

核心思路一句话：**把「反射搬运 + 强转 + 分批裸提交」改成「显式映射 + 容错解析 + 单行失败隔离 + 整次导入一个事务 + 错误回执」。**

```
                 上传 Excel
        ┌────────────────────────────────────────────┐
        │ HTTP  POST /schedule/upload  (MultipartFile)│
        └───────────────────┬────────────────────────┘
                            ▼
        ┌────────────────────────────────────────────┐
        │ 1 解析层  CheckResultDataListener            │
        │   invoke(): 单行 try/catch，失败仅丢该行      │
        │   → 记 errorRows(行号, 原因)，不中断整批      │
        └───────────────────┬────────────────────────┘
                            ▼
        ┌────────────────────────────────────────────┐
        │ 2 映射层  convert(): 显式 setter，不用反射     │
        │   · 字符串字段直赋                            │
        │   · 数值：parseIntSafe() 空/“-”/带单位容错     │
        │   · 日期：多格式兼容 + 判空                    │
        │   · 是否：isYes() 统一 是/1/Y → 1             │
        │   → 该行失败：抛 RowParseException(行号,原因)  │
        └───────────────────┬────────────────────────┘
                            ▼
        ┌────────────────────────────────────────────┐
        │ 3 事务层  整次导入 = 一个事务                  │
        │   TransactionTemplate.executeWithoutResult   │
        │   · 任一行失败 → 整体回滚（零脏数据）          │
        │   · 成功 → 一次提交，回执 totalRows/savedRows  │
        └───────────────────┬────────────────────────┘
                            ▼
        ┌────────────────────────────────────────────┐
        │ 4 回执层  CheckResultImportResult            │
        │   success / totalRows / savedRows /          │
        │   failedRows / errors[{rowIndex, message}]   │
        │   + @Slf4j 结构化日志（不再 System.out）      │
        └────────────────────────────────────────────┘
```

### 防线 1：删掉 `BeanUtils.copyProperties`，改为显式映射

**这一行必须整体删除。** 理由同 T-01 删除 `truncateTable()`、T-02 删除 `OaRequestAPIUtils`：保留一个「看起来能自动搬字段、实际会静默丢字段」的写法，就是缺陷复发的种子。

显式映射的收益：

| 维度 | `BeanUtils.copyProperties`（原） | 显式 setter（新） |
|---|---|---|
| 类型不兼容 | **静默跳过**，无任何提示 | 编译期就要求手写转换，不会漏 |
| 遗漏字段（`college`） | 看不出来 | 一眼可见「这个字段没有来源，需要补」 |
| 新增字段 | 改了 Model 忘了映射 → 静默丢 | 编译不报错但 code review 可见（后续可用单测兜底） |
| 可调试 | 断点只能看结果 | 每字段一行，出错行号直接定位 |
| 与 `@NotNull` 约束的一致性 | 校验在实体上，复制时已丢值 | 映射时就校验，失败即报行号 |

```java
/** 单行 Model → 实体，显式映射；失败抛 RowParseException（带行号与原因） */
private CheckResult convert(CheckResultModel item, int rowIndex) {
    CheckResult r = new CheckResult();
    r.setId(snowFlowUtils.nextId());

    // —— 字符串字段：直接赋值，不用反射 ——
    r.setWeeks(requireText(item.getWeeks(), "周次", rowIndex));
    r.setSection(requireText(item.getSection(), "节次", rowIndex));
    r.setClassroom(requireText(item.getClassroom(), "教室名", rowIndex));
    r.setClasses(requireText(item.getClasses(), "上课班级", rowIndex));
    r.setArrivalRate(requireText(item.getArrivalRate(), "出勤率", rowIndex));
    r.setDiscipline(item.getDiscipline());
    r.setFoodBringRate(item.getFoodBringRate());
    r.setCounsellor(requireText(item.getCounsellor(), "辅导员", rowIndex));
    r.setTeacher(requireText(item.getTeacher(), "任课老师", rowIndex));
    r.setCheckPerson(requireText(item.getCheckPerson(), "巡查人", rowIndex));

    // —— 数值字段：容错解析（空/“-”/带单位/小数 均可）——
    r.setShouldArrival(parseIntSafe(item.getShouldArrival(), "应到人数", rowIndex));
    r.setArrival(parseIntSafe(item.getArrival(), "实到人数", rowIndex));
    r.setFoodBringPerson(parseIntSafeOrZero(item.getFoodBringPerson(), "带食物人数", rowIndex));

    // —— 日期：多格式兼容 + 判空 ——
    r.setDate(parseDate(item.getDate(), rowIndex));

    // —— 是否类字段：统一“是/1/Y”判定，空值按“否” ——
    r.setIsLate(isYes(item.getIsLate()) ? 1 : 0);
    r.setIsNormal(isYes(item.getIsNormal()) ? 1 : 0);
    r.setIsViolate(isYes(item.getIsViolate()) ? 1 : 0);

    // —— 备注：保留原始文字描述（长度 > 2 说明不是单纯的“是/否”）——
    StringJoiner joiner = new StringJoiner(",");
    addIfDetail(joiner, item.getIsLate());
    addIfDetail(joiner, item.getIsNormal());
    addIfDetail(joiner, item.getIsViolate());
    r.setRemark(joiner.toString());

    // —— 有来源但此前被漏掉的字段 ——
    r.setCollege(collegeResolver.resolve());   // 见防线 2 的说明；不可静默置 null
    r.setPeopleLeave(0);                       // Excel 无此列，显式给 0（原为 int 默认值，行为不变）
    r.setIsStand(1);
    r.setIsConsist(1);
    return r;
}
```

> **注意 `isStand` / `isConsist` / `peopleLeave`**：表结构里 `is_stand`、`is_consist` 的 `DEFAULT` 是 `1`，但实体是 `Integer` 且 `null` 时 MyBatis-Plus 默认**不写入该列**（依赖 `insertStrategy` 配置），从而由数据库默认值兜底——这是「碰巧对」。显式赋值后行为**完全确定**，不再依赖 DB 默认值与 ORM 策略的巧合。`peopleLeave` 实体是**基本类型 `int`**，本来就无法为 `null`，显式置 0 只是把隐含行为写明白。

### 防线 2：解析容错 —— 让「脏单元格」只影响它自己

原来的强转（`Integer.parseInt` / `SimpleDateFormat.parse`）遇到任何一种非常规输入就抛异常，而异常发生在 `stream().map()` 里 → **整批 100 条一起失败**。

新策略：**每个字段、每一行单独判定；判定失败不抛到批处理层，而是记为该行的失败原因。**

```java
/* —— 数值：容错解析 —— */
private static int parseIntSafe(String raw, String fieldName, int rowIndex) {
    if (raw == null || raw.trim().isEmpty()) {
        throw new RowParseException(rowIndex, fieldName + "为空");
    }
    // 去掉百分号、单位、全角空格、千分位等噪声，只留数字/负号/小数点
    String cleaned = raw.trim().replaceAll("[^0-9.\\-]", "");
    if (cleaned.isEmpty() || "-".equals(cleaned)) {
        throw new RowParseException(rowIndex, fieldName + "格式不正确：" + raw);
    }
    try {
        return (int) Double.parseDouble(cleaned);   // 兼容 "50.0" / "50%"
    } catch (NumberFormatException e) {
        throw new RowParseException(rowIndex, fieldName + "格式不正确：" + raw);
    }
}

/** 允许缺省为 0 的数值列（带食物人数） */
private static int parseIntSafeOrZero(String raw, String fieldName, int rowIndex) {
    if (raw == null || raw.trim().isEmpty()) {
        return 0;                                    // 缺失即 0，不视为错误
    }
    return parseIntSafe(raw, fieldName, rowIndex);
}

/* —— 日期：多格式兼容 —— */
private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-M-d"),      // 2024-5-5 / 2024-05-05
        DateTimeFormatter.ofPattern("yyyy/M/d"),      // 2024/5/5 / 2024/05/05
        DateTimeFormatter.ofPattern("yyyy.M.d"),      // 2024.5.5
        DateTimeFormatter.ofPattern("yyyy年M月d日"),   // 2024年5月5日
        DateTimeFormatter.ofPattern("yyyyMMdd")       // 20240505（与排课 SKRQ 同格式）
};

private static Date parseDate(String raw, int rowIndex) {
    if (raw == null || raw.trim().isEmpty()) {
        throw new RowParseException(rowIndex, "日期为空");
    }
    String s = raw.trim();
    for (DateTimeFormatter f : DATE_FORMATS) {
        try {
            LocalDate d = LocalDate.parse(s, f);
            return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
            // 继续尝试下一种格式
        }
    }
    throw new RowParseException(rowIndex, "日期格式不正确：" + raw
            + "（支持 2024-05-05 / 2024/5/5 / 2024年5月5日 / 20240505）");
}

/* —— 文本必填 —— */
private static String requireText(String raw, String fieldName, int rowIndex) {
    if (raw == null || raw.trim().isEmpty()) {
        throw new RowParseException(rowIndex, fieldName + "为空");
    }
    return raw.trim();
}

/* —— 是否类：不抛错，空值即“否” —— */
private static boolean isYes(String v) {
    if (v == null) {
        return false;
    }
    String t = v.trim();
    return "是".equals(t) || "1".equals(t) || "Y".equalsIgnoreCase(t) || "y".equals(t);
}

/** 备注只收录“有具体描述”的单元格（长度 > 2 说明不是单纯的“是/否”） */
private static void addIfDetail(StringJoiner joiner, String v) {
    if (v != null && v.trim().length() > 2) {
        joiner.add(v.trim());
    }
}
```

> **`DateTimeFormatter.ofPattern("yyyy-M-d")` 的坑要记一笔**：`M` / `d` 是「1~2 位」的宽松模式，能同时吃下 `2024-5-5` 和 `2024-05-05`；但**写死 `yyyy-MM-dd` 就只能吃补零格式**——这正是原来 `SimpleDateFormat("yyyy-MM-dd")` 对 `2024/5/5` 失败的原因。所以格式表里**不能**出现 `MM`/`dd` 这种严格位数写法，否则「补齐格式」反而被漏掉。

#### 关于 `college`（Excel 里没有这一列）——**本项目最容易踩空的地方**

`CheckResultModel` **完全没有** `college` 字段，而 `CheckResult` 的 `college` 自带 `@NotNull`，且**几乎所有统计 SQL 都按 `college` 分组/过滤**。这意味着一份「没有学院列」的巡查 Excel 导进来，数据在报表里是**隐形**的。

现有代码对此**没有任何处理**（`copyProperties` 复制不过去，就成了 `null`，静默入库）。本方案**拒绝**继续「静默置 null」，给出三种可选策略：

| 策略 | 做法 | 适用前提 |
|---|---|---|
| **S1 从上传上下文推导（推荐）** | 上传接口已知当前登录用户（`ScheduleController#uploadCourse` 里已有 `User user`），用 `user.getCollege()` 作为整份 Excel 的学院 | 巡查数据按「谁上传算谁学院」的组织惯例成立 |
| **S2 Excel 增加学院列** | `CheckResultModel` 增加 `@ExcelProperty(value = "学院", index = N)`，逐行读取 | **表头列序会变**，需要与出表模板/存量模板同步，且影响所有已发出去的旧模板 |
| **S3 保持为空 + 前端后续补充** | 导入后跳转到「补录学院」页面 | 只有在业务确实允许「先入库后归属」时才可接受 |

**推荐 S1**：改动最小、与现有权限模型一致（`ScheduleController` 里已经有 `user.getCollege()`）、且**不破坏 Excel 模板**。同时保留一条兜底：`user.getCollege()` 也为空时 → **抛异常拒绝整次导入**（而不是默默写 null，把问题推迟到报表对不上时才暴露）。

> 这一条必须由业务确认后落地（见「九、待确认项」）。本计划书的默认取向是 **S1 + 空学院拒绝导入**。

### 防线 3：行级失败隔离 —— 一行有问题，不牵连整批

即使字段容错做得再全，仍会有「表头列错位」「整行空白」「人数填了乱码」这类真实存在的数据问题。策略是**把失败粒度从「批」降到「行」**：

```java
@Override
public void invoke(CheckResultModel row, AnalysisContext ctx) {
    int rowIndex = ctx.readRowHolder().getRowIndex() + 1;    // Excel 行号（1-based，含表头）
    try {
        CheckResult entity = convert(row, rowIndex);          // ← 单行映射，失败抛 RowParseException
        data.add(entity);
        successCount++;
    } catch (RowParseException e) {
        failCount++;
        errorRows.add(e.getMessage());                        // "第 12 行：应到人数格式不正确：—"
        log.warn("[巡查导入] 第 {} 行解析失败，已跳过：{}", rowIndex, e.getReason());
    } catch (Exception e) {
        failCount++;
        errorRows.add("第 " + rowIndex + " 行：未知错误 - " + e.getMessage());
        log.error("[巡查导入] 第 {} 行未预期异常", rowIndex, e);
    }
}
```

**关键设计取舍：单行失败 → 跳过 vs 整次导入失败？**

| 方案 | 优点 | 缺点 | 采用 |
|---|---|---|---|
| 单行失败即整次导入失败 | 数据绝对干净 | 一个单元格填错，1000 行全白导，老师体验极差 | ✗ |
| 单行失败跳过，其余入库 | 体验好，错误可回执 | 「部分成功」语义需明确告知用户 | ✓ |

**采用「跳过 + 回执」**，但配合下面的防线 4 做一件事：**当失败行数 > 0 时，是否提交由配置决定**（`fail-fast-on-error`，默认 `false` 即「跳过并提交」）。这样默认体验好，风险高的场景（例如对外报送数据）可以一键切成「有错就不导」。

> **与防线 4 的关系**：`invoke` 阶段只做**累积**（`data` / `errorRows` 都在内存里），**不落库**；真正写库统一在 `doAfterAllAnalysed` 里完成。这样「跳过哪几行」在写库之前就确定了，事务边界清晰。

### 防线 4：整次导入 = 一个事务（收掉「半张表」）

原实现的落库节奏是「每满 100 条 `saveBatch` 一次」，并且**外层没有任何事务**（`CheckResultServiceImpl#uploadCheckResult` 里的 `transactionTemplate.execute` 只包住了 `excelUtils.readScheduleExcel(file)` 这个**调用**，EasyExcel 的 `invoke` 回调是**由 EasyExcel 内部驱动**的，与那次 `TransactionTemplate` 是否同一个事务需要实测确认——见「五、验收用例 C-2」，这是必须**先验证再动手**的一点）。

**新的落库策略：解析全部完成 → 一次性事务落库。**

```java
@Override
public void doAfterAllAnalysed(AnalysisContext ctx) {
    // 1) 内存中已完成全部映射，统计口径确定
    // 2) 是否允许“有错行仍提交”由配置决定
    if (failCount > 0 && importProperties.isFailFastOnError()) {
        throw new RowParseException(0, "存在 " + failCount + " 行无法解析，已按配置放弃整次导入（未写入任何数据）");
    }
    // 3) 单事务写入：任一行失败 → 整体回滚，零脏数据
    persistAtomically();
    log.info("[巡查导入] 解析完成：总行数={}，成功={}，失败={}", successCount + failCount, successCount, failCount);
}

private void persistAtomically() {
    if (data.isEmpty()) {
        log.warn("[巡查导入] 无有效数据行，未执行写入");
        return;
    }
    final int batchSize = importProperties.getBatchSize() > 0 ? importProperties.getBatchSize() : 500;
    transactionTemplate.executeWithoutResult(status -> {
        for (int i = 0; i < data.size(); i += batchSize) {
            int end = Math.min(i + batchSize, data.size());
            checkResultService.saveBatch(new ArrayList<>(data.subList(i, end)), batchSize);
        }
    });
    savedCount = data.size();
    data = ListUtils.newArrayListWithExpectedSize(batchSize);
}
```

**为什么必须是「整次一个事务」而不是「每批一个事务」**——沿用 T-01 的结论：

| 维度 | 分批提交（原） | 单事务整体提交（新） |
|---|---|---|
| 第 3 批失败 | 前 2 批**已落库**，留下脏数据 | 全部回滚，**表回到导入前状态** |
| 用户重试 | 重复导入 → 数据翻倍（**无唯一键，无法去重**） | 重试安全（上次没留下任何东西） |
| 失败原因定位 | 只能看堆栈，不知道已进了多少 | 回执明确「写入 0 条」 |
| 大文件内存 | 峰值低（逐批释放） | 峰值 = 全量（**代价，见下**） |

> **代价要说清楚**：单事务方案把「内存里持有的实体」从 100 条变成**全量**。按每行实体约 300~500 字节估算，**10 万行约 30~50MB**，对服务器可接受；但如果是几十万行的超大文件，需要改回「分批提交 + 断点/幂等」策略——这正是「七、边界」里记录的前提条件。
>
> 折中做法（可选增强）：**先全量解析到内存 → 校验总量 → 再分批写入同一个事务**。这与本方案一致（`persistAtomically` 就是「一个事务内分批 `saveBatch`」），只是**分批的粒度只影响 SQL 往返次数，不影响事务边界**，是必要时的性能调优点。

### 防线 5：统计口径与日志

| 项 | 原实现 | 新实现 |
|---|---|---|
| 解析完成日志 | `System.out.println("解析完毕，共" + (count - 1) + "条数据")` | `log.info("解析完成：总计 {} 行，成功 {}，失败 {}")` |
| `-1` 的由来 | 误以为表头行会进 `invoke`（**不会**），所以多减了 1 | 直接以 `successCount + failCount` 为准，**不减 1** |
| `count` 字段 | 非 `volatile` 的裸累加，且与 `data` 清空不同步 | 改为 `successCount` / `failCount` 两个语义明确的计数器 |
| 调试输出 | `System.out.println(checkResult + "---?")` | 删除；需要时 `log.debug`（R-04 一并处理） |
| 日期解析二义性 | 手工 `SimpleDateFormat` 解析字符串 | 见「七、边界」讨论：可改用 `@DateTimeFormat` 让 EasyExcel 直接产出 `Date` |

**关于 `count - 1` 的确认**：EasyExcel 的 `AnalysisEventListener#invoke` **只对数据行回调，表头行不回调**（`headRowNumber` 默认为 1，表头被消费掉）。`CheckResultModel` 的 `@ExcelProperty` 用的是 `index = 1..17`（**从 1 开始，第 0 列被跳过**），说明这份 Excel 的第 1 列（index 0）是一个「序号」列或空列。因此 `count` 就是数据行数，`-1` 是**纯粹的错误**。

> ⚠️ **一个必须实测的不确定点**：`@ExcelProperty(index = 1)` 起跳，意味着 EasyExcel 按 **index 定位**（不是按表头名）。如果上传的 Excel **列序与模板不一致**（例如前面多插了一列），所有字段会**整体错位**而**不报任何错**——这比 T-03 现象本身更隐蔽。本方案在「五、验收用例」里安排了专门用例（B-4）确认，并建议在「九、待确认项」里决定是否改为**按表头名匹配**（`converter` + `headRowNumber`）或做**列序校验**。

---

## 三、改动清单（计划，尚未执行）

### 3.1 新增文件（2 个）

| 文件 | 归属模块 | 作用 |
|---|---|---|
| `soft_common/.../config/CheckResultImportProperties.java` | `soft_common` | `check-result.import.*` 配置绑定（批量大小 / 是否全量事务 / 有错是否放弃 / 学院来源策略）。**只写 `@ConfigurationProperties`，不加 `@Component`**（沿用 T-01 已确立的约定，由 `@ConfigurationPropertiesScan` 注册） |
| `soft_common/.../exception/RowParseException.java` | `soft_common` | 行级解析异常，携带 `rowIndex` + `fieldName` + `reason`，供回执直接展示 |

> **模块归属说明**：`CheckResultDataListener` 与 `CheckResultModel` 都在 `soft_service`，`RowParseException` 只被它们使用，**优先放 `soft_service`**（避免把纯局部的异常类下沉到公共模块）；若将来 `soft_main` 的回执组装也要用到，再上移。`CheckResultImportProperties` 放 `soft_common` 是因为按项目约定「配置类统一在 soft_common」，且 `soft_service` 可以依赖 `soft_common`。

### 3.2 新增/扩展：导入结果回执模型

| 文件 | 归属模块 | 作用 |
|---|---|---|
| `soft_model/.../vo/CheckResultImportResult.java` | `soft_model` | 导入结果明细：`success` / `totalRows` / `savedRows` / `failedRows` / `errors`（`List<RowError>`） / `message` / `costMillis` |

> 参照 T-01 的 `CourseScheduleSyncResult` 的位置与命名，保持两个导入类接口的观感一致。

### 3.3 修改文件（5 个）

| 文件 | 改动 |
|---|---|
| `soft_service/.../listener/CheckResultDataListener.java` | **核心改造**：删除 `BeanUtils.copyProperties` 与全部 `System.out`；新增 `convert()` / `parseIntSafe()` / `parseDate()` / `isYes()` / `requireText()`；`invoke` 加行级 try/catch；`doAfterAllAnalysed` 改为「统计 + 单事务落库 + 结果回执」；`count` 改为 `successCount` / `failCount`；引入 `@Slf4j` |
| `soft_model/.../dto/excel/CheckResultModel.java` | 视防线 2 的策略而定：若选 **S2** 则新增 `@ExcelProperty("学院")` 字段；若选 **S1/S3** 则**不改**（推荐不改） |
| `soft_service/.../service/excel/CheckResultExcelImportService.java` | `readScheduleExcel` 返回 `CheckResultImportResult`；`EasyExcel.read(...)` 增加 `.headRowNumber(1)`（显式声明，避免版本差异）；不再只是 `doReadAll()` 丢弃 listener |
| `soft_service/.../impl/CheckResultServiceImpl.java` | `uploadCheckResult` 返回 `CheckResultImportResult`；厘清事务边界（**保留最外层 `TransactionTemplate`，与 listener 内的落库事务合并为一次**，避免嵌套产生「内层提交后外层回滚无效」的误导性写法） |
| `soft_main/.../controller/ScheduleController.java` | `/schedule/upload` 返回 `ApiResult.ok("导入完成", result)`，把成功/失败行数与错误清单透出给前端 |
| `soft_main/src/main/resources/application-{dev,test,prd}.yml` | 新增 `check-result.import` 节点（见「四、配置说明」） |

### 3.4 数据库

无 DDL、无数据订正。

> **为什么不加唯一索引防重复导入**：这属于「改表」路线，且**当前 `sys_check_result` 无任何业务唯一键**（`id` 是自增主键）。是否要引入业务唯一键（例如 `date + section + classroom + classes`）涉及存量数据去重评估，**本方案明确不做**，见「七、边界」。

---

## 四、配置说明（实施时写入）

```yaml
# application-{dev,test,prd}.yml
check-result:
  import:
    enabled: true                 # 总开关；false 时上传接口直接拒绝
    batch-size: 500               # 单事务内的分批写入大小（只影响 SQL 往返，不影响事务边界）
    fail-fast-on-error: false     # true=只要有解析失败行就放弃整次导入；false=跳过坏行、其余入库
    require-college: true         # 学院为空时拒绝整次导入（对应防线 2 的兜底）
    college-strategy: FROM_UPLOADER  # FROM_UPLOADER | FROM_EXCEL | MANUAL（对应 S1/S2/S3）
    max-rows: 50000               # 单次导入上限；超过则拒绝（见“七、边界”第 1 条）
    strict-head-check: false      # 列序错位时是否严格拦截；false=仅告警并写入回执
```

| 配置项 | dev | test | prd | 说明 |
|---|---|---|---|---|
| `fail-fast-on-error` | `false` | `false` | `false` | 默认「跳过坏行」，体验优先；对外报送场景可临时改 `true` |
| `college-strategy` | `FROM_UPLOADER` | `FROM_UPLOADER` | `FROM_UPLOADER` | **待业务确认**；若确认改为 `FROM_EXCEL` 则需同步改 Excel 模板 |
| `max-rows` | `50000` | `50000` | `50000` | 防「单事务全量内存」被超大文件打爆 |

---

## 五、验收用例（实施后执行）

### 5.1 字段映射（核心，直接对应「没读到数据」的误判）

| 用例 | 构造方式 | 断言 |
|---|---|---|
| A-1 全字段正常 | 一行完整数据的 Excel | 入库后**逐字段比对** `date/weeks/section/classroom/classes/should_arrival/arrival/arrival_rate/discipline/food_bring_person/food_bring_rate/is_late/is_violate/is_normal/counsellor/teacher/check_person/remark/college`，与 Excel **完全一致** |
| A-2 `date` 真的入库了 | 上条记录 `SELECT date FROM sys_check_result WHERE id=...` | 是 `DATE` 值（不是 `NULL`）——**证明 `BeanUtils` 那行确实是缺陷** |
| A-3 人数真的入库了 | 同上，查 `should_arrival`/`arrival` | 非 `0`、非 `NULL`，等于 Excel 值 |
| A-4 是否类字段 | Excel 填「是」/「否」/空 / `1` / `Y` | 分别得 `1` / `0` / `0` / `1` / `1` |
| A-5 `college` 不再为空 | 走 S1 策略，以 A 学院账号上传 | `college` = `A 学院`（**统计接口能查到**） |
| A-6 `remark` 拼接 | 「是否迟到」填「是，第 1 节迟到 10 分钟」 | `remark` 含该长文本；`is_late = 1` |

### 5.2 解析容错（原「整批中断」的复现与修复）

| 用例 | 构造方式 | 断言 |
|---|---|---|
| B-1 应到人数为空 | 某行「应到人数」留空 | 该行计入失败并给出「第 N 行：应到人数为空」；**其余行正常入库**；接口回执含错误清单 |
| B-2 应到人数为 `-` | 某行填 `-` | 同上，原因文案可读 |
| B-3 日期为 `2024/5/5` | 整列改成斜杠格式 | **全部成功**（原实现此处 100% 失败） |
| B-4 日期为 `2024年5月5日` | 中文字段 | 成功解析 |
| B-5 列序错位 | 在 Excel 第 2 列前插入一列「备注」 | **必须能发现问题**：确认 `@ExcelProperty(index=N)` 是否导致整体错位；若错位，则本条**由 B-6 的方案兜底** |
| B-6 表头与列序校验 | 用错模板上传 | 给出明确提示（「第 X 列表头应为『日期』，实际为『备注』」），不静默错位导入 |
| B-7 「是否迟到」列为空 | 清空该列 | **不抛 NPE**（原实现在 `item.getIsLate().length()` 处必崩） |
| B-8 空行 | 数据区夹一行完全空白 | 该行记为失败或直接忽略（需明确口径），**不影响其他行** |

### 5.3 事务与脏数据（原「半张表」的复现与修复）

| 用例 | 构造方式 | 断言 |
|---|---|---|
| C-1 写入中途失败 | 临时把 `classes` 列改为 `varchar(5)`，让长度超限的行插入报错 | 事务整体回滚，`SELECT COUNT(*)` **回到导入前**（原实现会留下前几批） |
| C-2 事务边界实测 | 在 `saveBatch` 前后打日志 + 查询另一会话可见性 | 确认「EasyExcel 回调驱动的事务」与「`TransactionTemplate` 开启的事务」**是同一个**；若实测发现在两个事务里，则必须把落库整体收进 listener 自己的 `TransactionTemplate` |
| C-3 重复上传幂等性 | 同一文件连传两次 | 明确记录当前行为（会重复入库）；作为「是否需要业务唯一键」的决策依据 |
| C-4 失败后重试安全 | 导入失败后再传一次正确文件 | 表中**只有正确的那一份**（因为失败那次已全部回滚） |

### 5.4 统计口径与日志

| 用例 | 构造方式 | 断言 |
|---|---|---|
| D-1 日志数字正确 | 10 行数据（含 1 行错误） | 日志为「总计 10 行，成功 9，失败 1」（**不是 9 行**，原 `count - 1` 结果为 9 会误导） |
| D-2 无 `System.out` | 全流程运行后检索 stdout | 无 `---?`、无「解析完毕，共」等裸打印 |
| D-3 错误可回执 | 构造 3 个坏行 | 接口返回 `errors` 数组含 3 条，每条含行号与原因 |
| D-4 日志级别 | 正常运行 | 成功走 `info`，坏行走 `warn`，未知异常走 `error` |

### 5.5 接口与前端联调

| 用例 | 构造方式 | 断言 |
|---|---|---|
| E-1 上传成功回执 | `POST /schedule/upload` | `{"code":200,"msg":"导入完成","data":{"success":true,"totalRows":615,"savedRows":615,"failedRows":0,...}}` |
| E-2 部分成功回执 | 含坏行 | `success=true`、`failedRows>0`、`errors` 非空，前端可展示「成功 614 条，失败 1 条：第 12 行 应到人数为空」 |
| E-3 开关关闭 | `enabled=false` | 上传被拒绝并给出提示，不写库 |

### 5.6 编译与打包

```bash
JAVA_HOME=D:/CodeEnvironment/JDK1.8 mvn -B compile -DskipTests   # 期望 BUILD SUCCESS
```

> 构建提示（沿用 T-01/T-02 结论）：`pom.xml` 中 `java.version=1.8`，**必须用 JDK 1.8 编译**。用 JDK 21 会因 Lombok 版本过旧报 `NoSuchFieldError: JCTree$JCImport.qualid`（与本次改动无关）。EasyExcel 版本为 **2.2.3**（`pom.xml` 的 `easyexcel.version`），`@ExcelProperty` / `headRowNumber` 均支持。

---

## 六、上线与回滚

### 6.1 上线顺序

1. 确认 `check-result.import` 配置已在 dev 生效，先用**测试文件**跑通 A/B/C 三组用例；
2. 在预发环境用**真实巡查 Excel**（如果历史文件里确实存在 `2024/5/5` 这种格式，正好验证）跑一次；
3. 发布本次代码（**无 DDL，可滚动发布，无需停服**）；
4. 发布后建议先 **`mysqldump` 留底**再让业务方正式导一次：

```bash
mysqldump -uroot -p soft_manage sys_check_result > /tmp/check_result_bak_$(date +%F%H%M).sql
```

5. 对比导入回执的 `savedRows` 与 `SELECT COUNT(*)` 增量是否一致。

### 6.2 回滚

- **代码回滚**：`git revert` 本次提交即可。数据库结构无变更，无需数据订正。
- **注意**：回滚后 `BeanUtils.copyProperties` 与强转解析会**一并回退**，即 T-03 现象复发（`date`/人数/是否类字段全部丢失）。因此**不建议长期保留回滚状态**。
- **降级开关**：若导入功能在生产出问题，可先把 `check-result.import.enabled` 置 `false` 冻结导入，而不是回滚代码。

### 6.3 值班关注点

| 日志关键字 | 含义 | 处理 |
|---|---|---|
| `第 N 行解析失败，已跳过` | 单行数据有问题，**其余已正常入库** | 核对回执的 `errors`，让上传方修表格 |
| `存在 N 行无法解析，已按配置放弃整次导入` | `fail-fast-on-error=true` 且存在坏行 | 属预期行为；修表格后重传 |
| `无有效数据行，未执行写入` | 文件里没有可解析的数据行 | 检查模板与列序 |
| `未预期异常` | 代码缺陷 | 看堆栈定位 |

---

## 七、预期实际效果

> 以下为方案落地后的**目标状态**；本文档编写阶段**未改动任何代码**，效果待实施验证。

| 维度 | 改造前 | 改造后（预期） |
|---|---|---|
| **`date` 入库** | 被 `BeanUtils` 静默跳过，恒为 `null` | 正确解析为 `DATE`，多格式兼容（`2024-05-05` / `2024/5/5` / `2024年5月5日` / `20240505`） |
| **三个人数列** | 被静默跳过，恒为 `0` | 正确写入；空值/`-`/带单位/小数均可解析 |
| **三个是否列** | 被静默跳过，恒为 `0` | 按 `是/1/Y` 判定；空值按「否」，**不抛 NPE** |
| **`college`** | 无来源，恒为 `null`，**统计报表里查不到** | 由上传人上下文推导（S1），或按配置拒绝导入（`require-college`） |
| **解析中断** | 一行坏数据 → 整批 100 条失败 | 一行坏数据 → **只丢该行**，其余入库，并给出可读错误清单 |
| **日期格式** | 只认 `yyyy-MM-dd`，`2024/5/5` 直接失败 | 5 种格式兼容，且错误提示列出支持的格式 |
| **事务** | 每 100 条独立提交，中途失败留下半张表 | **整次导入一个事务**，失败零脏数据，重试安全 |
| **统计口径** | `count - 1`（少算一行且口径混乱） | `successCount` / `failCount` 语义明确，与入库量一致 |
| **日志** | `System.out.println` 打印整对象与「`---?`」 | `@Slf4j` 结构化日志，`info/warn/error` 分级（**顺带解决 R-04 在本文件的残留**） |
| **前端体验** | 只有「上传成功 / 失败」二元信息 | 回执含总行数/成功数/失败数/错误清单，可精确定位到行 |
| **字段遗漏风险** | 改 Model 忘映射 → 静默丢 | 显式 setter，遗漏在 code review 与单测中可见 |

**一句话总结**：把「导入是否成功」从**靠肉眼比对报表**，变成**有明确回执、有行号、有原因的可核对结果**；并且把「一行填错就全盘失败 + 留下脏数据」这两个最伤人的行为**同时消除**。

---

## 八、与其他问题的边界

| 编号 | 问题 | 与 T-03 的关系 |
|---|---|---|
| **T-02** | OA 密钥明文硬编码 | **无关**。但 T-02 的「吞异常返回 null → 抛带语义异常」与本方案「行级解析失败 → 抛 `RowParseException`」是**同一种思想**（让失败可区分、可归因） |
| **R-04** | 大量 `System.out.println` 打印业务数据 | **本文件的 `System.out` 本次一并清除**；`CourseDataListener` / `UserDataListener` 等**其他 listener 的同类问题未处理**（建议同批推进） |
| **T-05 / T-06** | 权限校验 NPE 与角色判定口径 | **无关**。但 `/schedule/upload` 目前**无权限校验**（仅取 `Authentication` 验用户存在），是否限制为「巡查管理员」上传，属 T-05 系列的范围 |
| **T-04** | `insertIntoTask` 是否删除原数据 | **无关** |
| **R-02** | 权限异常用 `RuntimeException` → HTTP 500 | **相关但不做**：本方案的 `RowParseException` 目前仍会被 `ScheduleController` 包成 `RuntimeException` 返回 500；若 R-02 落地了全局 `@RestControllerAdvice`，本方案的回执可以直接走标准错误码，**两者互不阻塞** |

### 遗留的已知边界（需评估）

1. **单事务 = 全量内存**。`max-rows` 默认 5 万，超过即拒绝导入。若业务存在 10 万行以上的巡查文件，需要改为「分批提交 + 幂等键」策略（依赖下面的第 2 条）。
2. **`sys_check_result` 没有业务唯一键**，所以**无法做防重复导入**。重复上传同一文件会重复入库。要根治需加唯一索引（如 `date + section + classroom + classes + check_person`），属**改表**，且需先评估存量数据是否已有重复。**本次不做**。
3. **`arrival_rate` / `food_bring_rate` 是 `varchar` 却参与 `AVG()`**。MySQL 会隐式转数；`"96.15%"` 含 `%` 时的行为依赖版本与 `sql_mode`。现有数据（见 `soft_manage20251104.sql` 第 55 行起）确实存的是 `'96.15%'` 这种带 `%` 的字符串——**统计口径存在既有风险**，与本问题无关但建议单独评估（考虑清洗为 `DECIMAL` 或入库时去掉 `%`）。
4. **`@ExcelProperty(index = ...)` 按列序定位**。一旦上传的 Excel 列序与模板不一致，会**整体错位且不报错**。本方案安排了 B-5/B-6 用例；若需要更强的健壮性，应改为**按表头名匹配 + 表头校验**（属增强项，会与「按 index 跳过首列」的现状冲突，需一并设计）。
5. **`CheckResultModel` 用的是纯 POJO（无 Lombok）**，与项目其他 Model 风格不一致；本方案**不顺带重构**，避免改动面失控。
6. **`CheckResultExcelImportService.readScheduleExcel` 的注释写着「读取第二个 sheet 页」，但代码 `doReadAll()` 读的是所有 sheet**。属**注释与实现不符**，本次一并修正注释（不改行为）；若业务确实只想读第 2 个 sheet，需改为 `.sheet(1)`——**列为待确认项**。

---

## 九、待确认项

1. **`college` 的来源到底是哪个？**（对应防线 2 的 S1/S2/S3）——这是本方案唯一影响「Excel 模板是否需要改」的决策点，**必须先确认再实施**。
2. **巡查 Excel 里「第 0 列」是什么？**（`@ExcelProperty` 从 `index = 1` 起跳）——决定是否可以在 `index = 0` 处加「学院」列。
3. **实际生产上传的巡查 Excel 日期是什么格式？** 是 `2024-05-05` 还是 `2024/5/5`？这决定 B-3 是「修 bug」还是「防御性兼容」。
4. **是否允许「部分成功」？**（`fail-fast-on-error` 默认 `false`）如果这份数据用于对外报送，可能需要改成「有错即全部拒绝」。
5. **单次导入的最大行数是多少？** 决定 `max-rows` 阈值与是否需要「分批提交 + 幂等」。
6. **是否需要防重复导入？** 若需要，必须加业务唯一键（改表路线），需先评估存量重复情况。
7. **是否只读第二个 sheet？** 现有代码 `doReadAll()` 与注释不符。
8. **`CheckResultServiceImpl#uploadCheckResult` 里那次 `transactionTemplate.execute` 与 EasyExcel 回调是否同一事务？**（用例 C-2）——**这是动手前必须实测的一点**，会直接决定改造时事务边界放在哪一层。

---

## 附录：改动清单速览

**实际落地（生产代码）**

```
新增（3）
  soft_common/src/main/java/com/hnkjzyxy/ab/config/CheckResultImportProperties.java
  soft_common/src/main/java/com/hnkjzyxy/ab/exception/RowParseException.java
  soft_common/src/main/java/com/hnkjzyxy/ab/exception/ImportRejectedException.java
  soft_model/src/main/java/com/hnkjzyxy/ab/vo/CheckResultImportResult.java

修改（7）
  soft_service/src/main/java/com/hnkjzyxy/ab/service/listener/CheckResultDataListener.java   ← 核心
  soft_service/src/main/java/com/hnkjzyxy/ab/service/excel/CheckResultExcelImportService.java
  soft_service/src/main/java/com/hnkjzyxy/ab/service/CheckResultService.java
  soft_service/src/main/java/com/hnkjzyxy/ab/service/impl/CheckResultServiceImpl.java
  soft_service/pom.xml                                                                       ← 显式声明 soft_model
  soft_main/src/main/java/com/hnkjzyxy/ab/controller/ScheduleController.java
  soft_main/src/main/resources/application-dev.yml / application-test.yml / application-prd.yml

未改
  soft_model/.../dto/excel/CheckResultModel.java    ← 沿用 S1 策略，Excel 模板零改动

数据库：无 DDL
外部依赖：无（不依赖 OA、不依赖 OA 密钥、不改 Excel 模板）
```

**测试代码：已编写、已验证、随后按要求全部移除**

```
已移除（不在当前代码库中）
  soft_service/src/test/java/.../listener/CheckResultDataListenerTest.java      ← 25 条（纯逻辑）
  soft_main/src/test/java/.../CheckResultImportIntegrationTest.java             ← 6 条（真库）
  soft_main/src/test/resources/application-t03test.yml                          ← 集成测试 profile
  soft_main/src/test/resources/setup-t03test-db.sql                             ← 隔离库建表脚本
```

> ⚠️ **回归保护已随之移除**：上述 31 条用例曾全绿（含「真实 MySQL 事务整体回滚」的实测证据），
> 但代码删除后**这些结论无法再自动回归**。若后续要防止 T-03 相关缺陷复现，需重建测试；
> 重建时注意 `@ActiveProfiles` 替换语义、`table-prefix` 必须显式配置、以及 `ScheduleLoad`
> 对 `sys_schedule_task` 的启动期依赖（详见上方第四轮说明）。

> **与 T-01 / T-02 的衔接**：三者共享同一套「**让失败可见、让数据不被静默破坏**」的工程原则。
> - **T-01** 保证「拿到数据后不丢」；
> - **T-02** 保证「密钥安全 + 拿不到时说得清原因」；
> - **T-03** 保证「拿到了数据后搬得对、搬不全时说得出是哪一行」。
>
> 三个方案都遵循同一条铁律：**宁可明确失败，也不静默降级**——这正是「`BeanUtils.copyProperties` 静默丢字段」这个 TODO 长期被误判为「没读到数据」的根本教训。
