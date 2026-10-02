# 项目 TODO 清单分析与解决方案

> 扫描对象：`assessment`（教学考核评价系统后端）
> 扫描时间：2026-09-26
> 扫描方式：全仓 `*.java / *.xml / *.yml / *.sql / *.properties` 正则匹配 `TODO|FIXME|XXX|HACK`，并人工核对每处上下文
> 说明：`assessment-back` 与 `assessment-back-v2` 的 Java 源码逐文件一致，本文结论对两份代码同时有效

---

## 一、总览

共命中 **14 处** TODO 标记。按性质分为四类：

| 类别 | 数量 | 说明 |
|---|---|---|
| A. 真实功能缺陷 / 隐患 | 8 | 存在线上事故、数据丢失、权限绕过、导入失败等实际风险 |
| B. 需要业务决策 | 1 | 语义不明确，需产品/业务方拍板 |
| C. 残留占位注释（已实现） | 3 | 代码早已实现，注释未清理，属技术债 |
| D. 类注释模板占位 | 2 | `@description: TODO`，仅需补文档 |

### 1.1 明细表

| 编号 | 位置 | 原文 | 类别 | 优先级 |
|---|---|---|---|---|
| T-01 | `soft_main/.../config/CourseScheduleSyncJob.java:47` | `// TODO: 在这里添加您需要执行的业务逻辑` | A | **P0** |
| T-02 | `soft_common/.../utils/OaRequestAPIUtils.java:53` | `//TODO 构建一个json对象，key,secret的值在应用查看中获取(OA系统申请的)` | A | **P0** |
| T-03 | `soft_service/.../listener/CheckResultDataListener.java:87` | `//TODO 这里应该是没有读到数据的，需要排查原因` | A | P1 |
| T-04 | `soft_main/.../controller/ProjectController.java:613` | `//TODO 是否要删除原来的projectItem表中的数据？` | B | P1 |
| T-05 | `soft_service/.../impl/ResultServiceImpl.java:214` | `//TODO 只有院长才能查看和修改` | A | P1 |
| T-06 | `soft_service/.../impl/ResultServiceImpl.java:234` | `//TODO 只有院长才能查看和修改` | A | P1 |
| T-07 | `soft_common/.../utils/PdfToWordConverter.java:67` | `TODO 文件的路径问题` | A | P2 |
| T-08 | `soft_common/.../utils/PdfToWordConverter.java:128` | `TODO 目前只考虑渲染PDF文件第一页为图像…` | A | P2 |
| T-09 | `soft_common/.../utils/PdfToWordConverter.java:71` | `//TODO 调整比率` | A | P2 |
| T-10 | `soft_service/.../impl/ProjectServiceImpl.java:631` | `//TODO 分页的代码` | A | P2 |
| T-11 | `soft_service/.../impl/ProjectServiceImpl.java:540` | `//TODO`（裸标记） | C | P3 |
| T-12 | `soft_service/.../impl/FlowServiceImpl.java:560` | `//TODO 判断是否已审批` | C | P3 |
| T-13 | `soft_service/.../impl/CourseServiceImpl.java:26` | `* @description: TODO` | D | P3 |
| T-14 | `soft_service/.../impl/CheckResultServiceImpl.java:26` | `* @description: TODO` | D | P3 |

### 1.2 建议处理顺序

```
P0（本迭代必做）
  └─ T-01 定时任务启动即清空课表  →  数据丢失风险，必须最先处理
  └─ T-02 OA 凭据硬编码明文       →  安全合规问题

P1（本迭代应做）
  └─ T-03 Excel 导入映射/解析缺陷
  └─ T-05 / T-06 权限判定空指针
  └─ T-04 子项转任务幂等性（需业务确认）

P2（下迭代排期）
  └─ T-07 / T-08 / T-09 PDF→Word 工具
  └─ T-10 内存分页性能

P3（随手清理，可随 P1/P2 改动一并提交）
  └─ T-11 / T-12 / T-13 / T-14
```

---

## 二、A 类：真实缺陷与隐患

### T-01 【P0】定时任务在项目启动时清空课表 —— 存在数据丢失风险

**位置**：`soft_main/src/main/java/com/hnkjzyxy/ab/job/CourseScheduleSyncJob.java`

```java
@PostConstruct
public void executeOnStartup() {
    Thread.sleep(5000);
    performTask();          // ← 启动即执行
}

@Scheduled(cron = "0 0 3 * * ?")
public void executeDailyAtNoon() {
    // TODO: 在这里添加您需要执行的业务逻辑
    performTask();
}

private void performTask() {
    courseScheduleService.truncateTable();     // ① 先清空
    String dataJson = getClassBoardData("", "");  // ② 再拉取
    if (dataJson == null || dataJson.isEmpty()) {
        return;                                 // ③ 拉不到就返回 —— 表已空！
    }
    ...
}
```

**问题分析**

1. **清空与拉取顺序不可逆**：先 `TRUNCATE` 再调用外部 OA 接口。若 OA 接口超时、返回空、返回非 200，方法直接 `return`，`sys_course_schedule` 已被清空且**没有任何回滚**，门锁/班牌的排课数据全部丢失。
2. **`@PostConstruct` 执行时机危险**：`Thread.sleep(5000)` 阻塞 Bean 初始化线程；若此时数据源或 OA 未就绪，等于每次重启都做一次"清空赌运气"。
3. **无幂等/无锁**：`performTask` 未加分布式锁，多实例部署时 3 点会并发 `TRUNCATE` + 批量插入，产生主键冲突或数据错乱。
4. **无事务**：批量 `saveBatch` 分批提交（每 100 条），中途异常会留下半张表的数据。
5. **`Thread.sleep` 在 `@PostConstruct` 中违反并发规范**，容器的优雅启停会被拖慢。

**解决方案**

**（1）改为"先写临时表 / 先校验，再原子切换"**

最稳妥的做法是**不做 TRUNCATE，改为按业务主键 upsert + 差异删除**；若必须全量替换，则先落库到影子表，校验通过后再原子改名：

```java
private void performTask() {
    // 1. 拉数据（失败直接抛出，绝不碰正式表）
    String dataJson = getClassBoardData("", "");
    if (StringUtils.isBlank(dataJson)) {
        throw new IllegalStateException("OA 返回空数据，本次同步放弃，正式表保持不变");
    }
    List<CourseSchedule> schedules = parseAndValidate(dataJson);
    if (schedules.isEmpty()) {
        throw new IllegalStateException("解析后课程数据为 0 条，疑似接口异常，放弃同步");
    }

    // 2. 开启事务，整体替换
    transactionTemplate.execute(status -> {
        courseScheduleService.remove(new QueryWrapper<>());       // DELETE 而非 TRUNCATE，可回滚
        for (List<CourseSchedule> batch : Lists.partition(schedules, 500)) {
            courseScheduleService.saveBatch(batch);
        }
        return null;
    });
}
```

**（2）加分布式锁，保证多实例只跑一次**

```java
private static final String LOCK_KEY = "lock:schedule:sync";
private static final long LOCK_TTL_MS = 30 * 60 * 1000L;

String token = UUID.randomUUID().toString();
Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(LOCK_KEY, token, LOCK_TTL_MS, TimeUnit.MILLISECONDS);
if (!Boolean.TRUE.equals(locked)) {
    log.warn("其他实例正在同步课表，本次跳过");
    return;
}
try {
    performTask();
} finally {
    // Lua 脚本比对 token 后释放，避免误删他人锁
    redisTemplate.execute(RELEASE_LOCK_LUA, Collections.singletonList(LOCK_KEY), token);
}
```

**（3）`@PostConstruct` 改造**

```java
// 方案 A（推荐）：启动同步改为 ApplicationRunner + 条件开关，默认关闭
@Component
@ConditionalOnProperty(name = "schedule.sync-on-startup", havingValue = "true")
public class ScheduleSyncRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        performTask();     // 不再 sleep，容器已完全启动
    }
}

// 方案 B：保留 @PostConstruct 但用异步线程池，绝不阻塞启动
@PostConstruct
public void init() {
    applicationEventPublisher.publishEvent(new ScheduleSyncEvent());
}
```

**（4）清理残留注释**：`executeDailyAtNoon` 内的 `// TODO: 在这里添加您需要执行的业务逻辑` 删除，业务逻辑即 `performTask()`，注释已失效。

**（5）补充配置项与监控**

```yaml
schedule:
  sync:
    enabled: true
    sync-on-startup: false     # 生产默认关闭，避免"重启即清表"
    batch-size: 500
```

**验收方式**

- 构造 OA 接口超时/返回 `{}`/返回 HTTP 500 三种用例，断言 `sys_course_schedule` **行数不变**。
- 双实例同时触发 3 点任务，断言只有一台执行、另一台打印跳过日志。

---

### T-02 【P0】OA 接口密钥明文硬编码在源码中

**位置**：`soft_common/src/main/java/com/hnkjzyxy/ab/utils/OaRequestAPIUtils.java:53`

```java
//TODO 构建一个json对象，key,secret的值在应用查看中获取(OA系统申请的)
String input = "{\"key\":\"20250622398919366238010517831682779\",\"secret\":\"e6c226b86065d7a8df88a5e3119ba6d0491b791a\"}";
```

**问题分析**

1. **凭据泄露**：`key` / `secret` 已随源码提交进 Git 历史，任何拿到仓库的人都能直接调用 OA 开放平台接口，属明确的**安全事件**（密钥需要 OA 侧吊销重发）。
2. **无法多环境切换**：测试/生产共用同一份密钥，且写死在 `static` 工具方法内，无法通过配置覆盖。
3. **字符串拼 JSON**：手写字符串拼接 JSON，`key` 中含引号/反斜杠即产生非法 JSON；注释本身也要求"构建一个 json 对象"。
4. **工具类是静态方法 + 静态常量**，无法注入 Spring 配置（`static` 字段不能用 `@Value` 注入实例字段）。

**解决方案**

**（1）密钥外置到配置文件，并用环境变量兜底**

```yaml
# application.yml
oa:
  base-url: https://dmp.hnkjxy.net.cn
  key: ${OA_APP_KEY:}
  secret: ${OA_APP_SECRET:}
  token-url: ${oa.base-url}/open_api/authentication/get_access_token
  connect-timeout: 10000
  read-timeout: 30000
```

```java
@Component
@ConfigurationProperties(prefix = "oa")
@Data
public class OaProperties {
    private String baseUrl;
    private String key;
    private String secret;
    private int connectTimeout = 10000;
    private int readTimeout = 30000;
}
```

**（2）把静态工具类改造为 Spring Bean，用 ObjectMapper 构造 JSON**

```java
@Component
@RequiredArgsConstructor
public class OaApiClient {

    private final OaProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 获取 OA 访问令牌 */
    public String getAccessToken() {
        try {
            JSONObject body = new JSONObject();
            body.put("key", props.getKey());
            body.put("secret", props.getSecret());

            URL url = new URL(props.getBaseUrl() + "/open_api/authentication/get_access_token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(props.getConnectTimeout());
            conn.setReadTimeout(props.getReadTimeout());

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (conn.getResponseCode() != 200) {
                throw new IllegalStateException("获取 OA token 失败，HTTP " + conn.getResponseCode());
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                JsonNode root = objectMapper.readTree(reader);
                return root.path("result").path("access_token").asText(null);
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            throw new IllegalStateException("调用 OA 鉴权接口异常", e);
        }
    }
}
```

**（3）先缓冲 token，避免每次调用都取一次**

```java
/** 缓存 access_token，提前 5 分钟过期 */
private volatile String cachedToken;
private volatile long expireAt;

public String getToken() {
    if (cachedToken != null && System.currentTimeMillis() < expireAt) {
        return cachedToken;
    }
    synchronized (this) {
        if (cachedToken != null && System.currentTimeMillis() < expireAt) {
            return cachedToken;
        }
        cachedToken = getAccessToken();
        expireAt = System.currentTimeMillis() + 55 * 60 * 1000L;
        return cachedToken;
    }
}
```

**（4）同步调用方**：`CourseScheduleSyncJob`、以及任何引用 `OaRequestAPIUtils.getClassBoardData(...)` 的静态调用点，全部改为注入 `OaApiClient`。

**（5）必须执行的收尾动作**

- 用 `git log -S "e6c226b86065"` 确认密钥出现在哪些历史提交中。
- **联系 OA 平台吊销当前 key/secret 并重新签发**（仅从代码里删掉不足以消除风险）。
- 把 `OA_APP_KEY` / `OA_APP_SECRET` 配到部署环境的环境变量或配置中心，`.gitignore` 排除本地覆盖文件。

**（6）顺带修复**：`getAccessToken` 内的 `System.out.println` 会打印 token 到日志，改为 `log.debug` 且**不要打印完整 token**。

---

### T-03 【P1】Excel 巡查结果导入：字段映射失效、解析易中断

**位置**：`soft_service/src/main/java/com/hnkjzyxy/ab/service/listener/CheckResultDataListener.java:87`

```java
//TODO 这里应该是没有读到数据的，需要排查原因
BeanUtils.copyProperties(item, checkResult);
```

**根因分析（已定位，不是"没读到数据"，而是类型不匹配导致静默丢字段）**

`BeanUtils.copyProperties`（Spring 版）**只在属性名相同且类型可赋值时才复制**，类型不同会被**静默跳过**。对照两个类：

| 字段 | `CheckResultModel`（Excel 行） | `CheckResult`（实体） | copy 是否生效 |
|---|---|---|---|
| `date` | `String` | `Date` | ❌ 类型不兼容 |
| `weeks` / `section` / `classroom` / `classes` | `String` | `String` | ✅ |
| `arrivalRate` / `discipline` / `foodBringRate` | `String` | `String` | ✅ |
| `counsellor` / `teacher` / `checkPerson` | `String` | `String` | ✅ |
| `shouldArrival` / `arrival` / `foodBringPerson` | `String` | `Integer` | ❌ 类型不兼容 |
| `isLate` / `isViolate` / `isNormal` | `String` | `Integer` | ❌ 类型不兼容 |
| `college` | 无 | `String` | ❌ 无来源 |
| `peopleLeave` | 无 | `int` | ❌ 无来源 |

所以 `copyProperties` 实际只搬了 8 个字符串字段，**其余全部为默认值**——开发者打印对象时看到一堆 `null`/`0`，误判为"数据没读到"。事实上下面已经手工赋值了这些字段，因此这一行**既无用又误导**。

**其余隐藏缺陷（同一处上下文）**

1. `Integer.parseInt(item.getShouldArrival())`：Excel 单元格为空 / 填了"-" / 含空格，直接抛 `NumberFormatException`，`saveData()` 整体失败，**整批导入中断**。
2. 日期 `new SimpleDateFormat("yyyy-MM-dd").parse(...)`：中文 Excel 默认输出 `2024/5/5`，会走到 `throw new RuntimeException`，导入失败。
3. `doAfterAllAnalysed` 里 `System.out.println("解析完毕，共" + (count - 1) + "条数据")`：EasyExcel 不会把表头行传给 `invoke`，`count` 就是数据行数，**`-1` 是错的（少统计一行）**。
4. `count` 字段非 `volatile`，且 `saveData()` 后 `data` 被重置而 `count` 继续累加，报告数字与实际入库数量口径不一致。
5. `BATCH_COUNT` 达到即 `saveData()`，但**导入方法外层没有事务**，第 3 批失败会留下前 2 批的脏数据。

**解决方案**

**（1）删掉误导性的 `copyProperties`，改为显式映射 + 容错解析**

```java
public void saveData() {
    List<CheckResult> checkResults = data.stream()
            .map(this::convert)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    if (!checkResults.isEmpty()) {
        checkResultService.saveBatch(checkResults);
    }
    savedCount += checkResults.size();
    data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
}

private CheckResult convert(CheckResultModel item) {
    CheckResult r = new CheckResult();
    r.setId(snowFlowUtils.nextId());

    // 字符串字段：显式赋值，不用反射复制
    r.setWeeks(item.getWeeks());
    r.setSection(item.getSection());
    r.setClassroom(item.getClassroom());
    r.setClasses(item.getClasses());
    r.setArrivalRate(item.getArrivalRate());
    r.setDiscipline(item.getDiscipline());
    r.setFoodBringRate(item.getFoodBringRate());
    r.setCounsellor(item.getCounsellor());
    r.setTeacher(item.getTeacher());
    r.setCheckPerson(item.getCheckPerson());

    // 数值字段：容错解析
    r.setShouldArrival(parseIntSafe(item.getShouldArrival(), "应到人数"));
    r.setArrival(parseIntSafe(item.getArrival(), "实到人数"));
    r.setFoodBringPerson(parseIntSafe(item.getFoodBringPerson(), "带食物人数"));

    // 日期：多格式兼容
    r.setDate(parseDate(item.getDate()));

    // 是否类字段：统一"是"/"否"判定
    r.setIsLate(isYes(item.getIsLate()) ? 1 : 0);
    r.setIsNormal(isYes(item.getIsNormal()) ? 1 : 0);
    r.setIsViolate(isYes(item.getIsViolate()) ? 1 : 0);

    // 备注：保留原始文字描述（长度 > 2 说明不是单纯的"是/否"）
    StringJoiner joiner = new StringJoiner(",");
    addIfDetail(joiner, item.getIsLate());
    addIfDetail(joiner, item.getIsNormal());
    addIfDetail(joiner, item.getIsViolate());
    r.setRemark(joiner.toString());
    return r;
}

private static int parseIntSafe(String raw, String fieldName) {
    if (raw == null || raw.trim().isEmpty()) {
        return 0;
    }
    String cleaned = raw.trim().replaceAll("[^0-9.-]", "");
    if (cleaned.isEmpty()) {
        throw new IllegalArgumentException(fieldName + "格式不正确：" + raw);
    }
    return (int) Double.parseDouble(cleaned);
}

private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ofPattern("yyyy年M月d日")
};

private static Date parseDate(String raw) {
    if (raw == null || raw.trim().isEmpty()) {
        throw new IllegalArgumentException("日期不能为空");
    }
    String s = raw.trim();
    for (DateTimeFormatter f : DATE_FORMATS) {
        try {
            LocalDate d = LocalDate.parse(s, f);
            return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
            // 尝试下一种格式
        }
    }
    throw new IllegalArgumentException("日期格式不正确，请使用 2024-05-05 格式，实际为：" + raw);
}

private static boolean isYes(String v) {
    return "是".equals(v) || "1".equals(v) || "Y".equalsIgnoreCase(v);
}

private static void addIfDetail(StringJoiner joiner, String v) {
    if (v != null && v.trim().length() > 2) {
        joiner.add(v.trim());
    }
}
```

**（2）改为"解析失败行不影响整批"的容错策略**

建议 `invoke` 中捕获单行异常，收集错误行号与原因，导入结束后返回错误清单给前端，而不是整批失败：

```java
@Override
public void invoke(CheckResultModel objects, AnalysisContext ctx) {
    try {
        data.add(objects);
        if (data.size() >= BATCH_COUNT) {
            saveData();
        }
    } catch (Exception e) {
        errorRows.add("第 " + (ctx.readRowHolder().getRowIndex() + 1) + " 行：" + e.getMessage());
    }
}
```

**（3）修正统计口径与日志**

- 删除 `count - 1`，改为记录 `successCount` / `failCount`。
- 全部 `System.out.println` 替换为 `@Slf4j` 的 `log.info/warn/error`。
- 建议改为 `@DateTimeFormat` + `Date` 字段直接交给 EasyExcel 解析 Excel 日期单元格，比字符串二次解析更稳。

**（4）给导入方法加事务边界**：在调用方 Service 上对整次导入加 `@Transactional`，或明确采用"分批提交 + 错误行回执"策略并写进文档。

**验收方式**：用 3 组测试文件覆盖——(a) 全字段正常；(b) 应到人数为空 / 填"-"； (c) 日期为 `2024/5/5`。要求 (b)(c) 给出可读错误提示且不产生脏数据，(a) 入库字段与 Excel 完全一致。

---

### T-05 / T-06 【P1】权限校验存在空指针，且角色判定口径不统一

**位置**：`soft_service/src/main/java/com/hnkjzyxy/ab/service/impl/ResultServiceImpl.java:214` 与 `:234`

```java
//TODO 只有院长才能查看和修改
Role role = userRoleMapper.getRoleWeight(userId);   // 可能返回 null
if (!HnkjzyEncode.DEAN.getName().equals(role.getRoleName())) {
    throw new RuntimeException("该列表只有院长可以查看");
}
```

`UserRoleMapper.getRoleWeight` 的实现是：

```sql
select * from sys_role
where role_status = 1
  and role_id in (select role_id from sys_user_role where user_id = #{userId})
order by weight desc limit 1
```

**问题分析**

1. **空指针（最严重）**：用户**未分配任何角色**、或角色被停用（`role_status != 1`）时，SQL 返回空集，`getRoleWeight` 返回 `null`，紧接着 `role.getRoleName()` 抛 NPE，接口返回 500 而非"无权限"的 403/业务提示。这在教师账号未配权限时是**必现**的。
2. **判定用"角色名"而非"权重/角色码"**：`HnkjzyEncode` 存在**重名与同权重**情况——`DEAN(9,"院长")` 与 `CLERK(9,"书记")` 权重相同。用 `roleName` 字符串比较，一旦别名改为"二级学院院长"就静默失效；且角色名是数据，不应作为权限判定依据。
3. **两处口径不一致**：`getList` 只允许 `院长`；`getLists` 允许 `超级用户` 或 `院长`。注释都写"只有院长"，与实现不符——到底谁有权，需要明确。
4. **"查看和修改"未区分**：两个方法都只有查看语义，方法名（`getList` / `getLists`）与注释中的"修改"没有对应实现。
5. **`userId` 变量定义了但只用于查角色**，`getLists` 中甚至未使用。

**解决方案**

**（1）抽取统一的安全断言工具，杜绝 NPE**

```java
@Component
@RequiredArgsConstructor
public class RoleAssert {

    private final UserRoleMapper userRoleMapper;

    /** 获取用户最高权重角色，无角色时抛业务异常而非 NPE */
    public Role requireRole(Integer userId) {
        Role role = userRoleMapper.getRoleWeight(userId);
        if (role == null) {
            throw new BusinessException("当前账号未分配任何角色，请联系管理员");
        }
        return role;
    }

    /** 断言用户属于指定角色集合（按权重判定，不依赖角色名） */
    public void requireAnyOf(Integer userId, HnkjzyEncode... allowed) {
        Role role = requireRole(userId);
        Set<Integer> codes = Arrays.stream(allowed)
                .map(HnkjzyEncode::getCode)
                .collect(Collectors.toSet());
        if (!codes.contains(role.getWeight())) {
            throw new BusinessException("无权限执行该操作");
        }
    }
}
```

**（2）按权重（`weight`）而非角色名判定**

```java
@Override
public Map<String, Object> getList(SubTaskDto dto, User user) {
    // 仅院长（weight = 9）可查看
    roleAssert.requireAnyOf(user.getUserId(), HnkjzyEncode.DEAN);

    HashMap<String, Object> map = new HashMap<>();
    List<SubTaskVo> score = resultMapper.getUsersSubTaskScore(
            dto.getTitle(), dto.getTaskName(), dto.getTaskCategory());
    map.put("total", score.size());
    map.put("list", score);
    return map;
}

@Override
public Map<String, Object> getLists(SubTaskIdDto dto, User user) {
    // 超管或院长可查看（与 getList 的口径差异在此显式声明）
    roleAssert.requireAnyOf(user.getUserId(), HnkjzyEncode.ADMIN, HnkjzyEncode.DEAN);

    HashMap<String, Object> map = new HashMap<>();
    List<SubTaskVo> score = resultMapper.getUsersSubTaskScoreById(dto);
    map.put("total", score.size());
    map.put("list", score);
    return map;
}
```

> **注意**：`DEAN(9)` 与 `CLERK(9,"书记")` 权重相同，若业务上"书记"不应看到该列表，则**不能只按权重判断**，必须在 `HnkjzyEncode` 中引入独立的 `roleCode` 或按 `roleId` 白名单判定。这一点需要与业务确认后落地，见下方"待确认项"。

**（3）明确"查看/修改"权限边界**

- 若确实存在修改入口（如修改巡查成绩），补充独立的 `updateXxx` 方法并在其中断言角色，不要让"查看"接口隐式承担"修改"语义。
- 建议引入 Spring Security 的 `@PreAuthorize("hasAnyRole('DEAN','ADMIN')")`，把权限声明在方法签名上，比散落在方法体内的 `if` 更易审计。

**（4）补充全局异常处理**：将 `BusinessException` 统一映射为 `403 + 业务错误码`，避免 NPE 变成 500。

**验收方式**：构造（a）无角色用户、(b) 角色已停用用户、(c) 书记角色、(d) 院长角色 四类账号，断言 a/b/c 得到明确的"无权限"业务提示（HTTP 4xx），d 正常返回。

---

### T-07 / T-08 / T-09 【P2】PDF 转 Word 工具：路径、多页、比例三个问题

**位置**：`soft_common/src/main/java/com/hnkjzyxy/ab/utils/PdfToWordConverter.java`
调用方：`EvidenceWordService#generate` 生成证据材料 Word，`EvidenceWordExporter#export` 负责 HTTP 下载及临时文件回收（2026-10-02 职责拆分更新）。

**问题分析**

| 编号 | 行 | 问题 |
|---|---|---|
| T-07 | 67-68 | `path + pathSeparator + filePath` 手工拼路径：`path` 若本身以分隔符结尾会得到 `D:/upload//123.pdf`；`filepath.list()` 在目录不存在或无权限时**返回 `null`**，随后 `for` 循环抛 NPE |
| T-08 | 128-130 | `renderer.renderImage(0)` 只渲染第 1 页，**多页 PDF 后续页面全部丢失**；且只取 `0` 而不看 `document.getNumberOfPages()` |
| T-09 | 71-78 | 插入图片时宽高写死 `400pt × 600pt`，与图片真实宽高比不符时会**拉伸变形**；且 `FileInputStream` 未用 try-with-resources，异常时文件句柄泄漏，导致后续"删除图片"失败 |

**其他关联隐患**：`list()` 返回的文件名不判断扩展名，目录里混入非 PDF 文件会走到 `PDDocument.load` 抛异常；`.png` 与 PDF 同名同目录，若 PDF 名为 `a.png.pdf` 会覆盖同名图片。

**解决方案**

```java
public static void pdfFilesToWordFile(String path, String wordFilePath) throws Exception {
    File dir = new File(path);
    if (!dir.isDirectory()) {
        throw new IllegalArgumentException("PDF 目录不存在或不是目录：" + path);
    }
    // T-07：用 File 拼接，且过滤出真正的 pdf 文件
    File[] pdfFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
    if (pdfFiles == null || pdfFiles.length == 0) {
        throw new IllegalArgumentException("目录下没有 PDF 文件：" + path);
    }
    Arrays.sort(pdfFiles, Comparator.comparing(File::getName));   // 保证顺序稳定

    XWPFDocument doc = new XWPFDocument();
    XWPFParagraph p = doc.createParagraph();
    List<String> tempImages = new ArrayList<>();

    try {
        for (File pdf : pdfFiles) {
            // T-08：转换并返回多页图片路径
            List<String> imagePaths = convertPdfToImages(pdf.getAbsolutePath());
            tempImages.addAll(imagePaths);

            for (String imagePath : imagePaths) {
                try (FileInputStream fis = new FileInputStream(imagePath)) {   // T-09：自动关闭
                    BufferedImage img = ImageIO.read(new File(imagePath));
                    // T-09：按 PDF 页面原始宽高比等比缩放，最长边限制为 600pt
                    float[] size = scaleToFit(img.getWidth(), img.getHeight(), 600f);
                    XWPFRun r = p.createRun();
                    r.addPicture(fis, XWPFDocument.PICTURE_TYPE_PNG, imagePath,
                            Units.toEMU(size[0]), Units.toEMU(size[1]));
                    r.addBreak();     // 每页图片之间换行，避免堆叠
                }
            }
        }
        try (FileOutputStream out = new FileOutputStream(wordFilePath)) {
            doc.write(out);
        }
    } finally {
        doc.close();
        // 清理临时图片
        tempImages.forEach(ip -> {
            File f = new File(ip);
            if (f.exists() && !f.delete()) {
                log.warn("临时图片删除失败：{}", ip);
            }
        });
    }
}

/** 等比缩放，返回 [宽pt, 高pt] */
private static float[] scaleToFit(int imgW, int imgH, float maxSide) {
    float ratio = Math.min(maxSide / imgW, maxSide / imgH);
    return new float[]{imgW * ratio, imgH * ratio};
}

/** T-08：渲染 PDF 全部页面 */
private static List<String> convertPdfToImages(String pdfPath) throws IOException {
    List<String> result = new ArrayList<>();
    File pdfFile = new File(pdfPath);
    String baseName = getFileNameWithoutExtension(pdfFile);
    String parent = pdfFile.getParent();

    try (PDDocument document = PDDocument.load(pdfFile)) {
        PDFRenderer renderer = new PDFRenderer(document);
        int pages = document.getNumberOfPages();
        for (int i = 0; i < pages; i++) {
            // 临时文件加 UUID 前缀，彻底避免同名覆盖
            File png = new File(parent, baseName + "_" + i + "_" + UUID.randomUUID() + ".png");
            BufferedImage image = renderer.renderImage(i, 2.0f);   // 2 倍缩放，提升清晰度
            ImageIO.write(image, "PNG", png);
            result.add(png.getAbsolutePath());
        }
    }
    return result;
}
```

**性能提示**：`renderImage(i, 2.0f)` 会显著增加内存占用与耗时。证据导出通常页数不多，可接受；若 PDF 页数可能很大，建议加页数上限（如 100 页）并对超出部分给出提示，避免 OOM。大文件导出建议改为异步任务 + 下载链接，避免 HTTP 线程长时间阻塞。

---

### T-10 【P2】优秀项目考核列表：全量查内存后排序分页

**位置**：`soft_service/src/main/java/com/hnkjzyxy/ab/service/impl/ProjectServiceImpl.java:631`

```java
projectVos.sort((a1, a2) -> a2.getScore().compareTo(a1.getScore()));
int rank = 1;
for (ProjectVo projectVo : projectVos) {
    projectVo.setRank(rank++);
}
//TODO 分页的代码
Map<String, Object> page = PageUtils.page(projectVos, param.getPage(), param.getLimit());
```

**问题分析**

1. 分页**已经实现**（`PageUtils.page` 做了 `subList`），因此该 TODO 属**残留注释**，应删除。但它暴露了两个真实问题：
2. **全量加载 + 内存分页**：`getProjectAssessList` 会把该校/该院所有教师所有项目的结果**一次性查进内存**再排序、再 `subList`。数据量增长后，单次请求的内存与耗时线性上升，是典型的"早该下推到 SQL"的模式。
3. **`rank` 语义在分页下是错的**：排名按**全量排序后**的序号赋值，分页只截取其中一页——分页参数一改，同一名教师在不同页看到的 `rank` 数值会变化（第 2 页第一条会显示 `rank = limit+1`）。如果需求是"全局排名"，这个做法是对的（因为 rank 在截取**之前**已赋值）；但如果是"当前页排名"，则逻辑错误。**需要确认语义**。
4. `PageUtils` 的 `totalPage` 计算出来后**没有放进返回结果**，前端拿不到总页数。
5. `projectVos.sort` 中 `a2.getScore().compareTo(a1.getScore())` 若 `score` 为 `null` 会 NPE（虽然上面有 `score != null ? score : 0` 兜底，但 `addProjectVos` 分支与普通用户分支都设了，暂无风险，仍建议显式判空）。

**解决方案**

**（1）优先方案：把过滤 + 排序 + 分页下推到 SQL**

在各 `checkXxxRoles` 的权限条件能转换成 SQL 的前提下，改为 MyBatis-Plus 分页：

```java
@Override
public ApiResult getProjectAssessList(ProjectQueryParam param, User user) {
    Role role = roleAssert.requireRole(user.getUserId());

    // 1. 先算出"可见用户 id 集合"（现有逻辑保留，但只取 id，不组装 VO）
    Set<Integer> visibleUserIds = resolveVisibleUserIds(role, user, param);
    if (visibleUserIds.isEmpty()) {
        return ApiResult.ok(PageUtils.empty());
    }

    // 2. 用 IPage 下推分页，排序交给数据库
    Page<ProjectVo> page = new Page<>(param.getPage(), param.getLimit());
    IPage<ProjectVo> result = projectMapper.selectAssessPage(
            page, param, visibleUserIds);

    // 3. 计算全局排名：= (当前页 - 1) * limit + 行号
    long base = (page.getCurrent() - 1) * page.getSize();
    int offset = 0;
    for (ProjectVo vo : result.getRecords()) {
        vo.setRank((int) (base + ++offset));
    }

    Map<String, Object> data = new HashMap<>();
    data.put("total", result.getTotal());
    data.put("totalPage", result.getPages());
    data.put("list", result.getRecords());
    return ApiResult.ok(data);
}
```

配合 Mapper：

```xml
<select id="selectAssessPage" resultType="com.hnkjzyxy.ab.vo.ProjectVo">
    select u.user_id       as userId,
           u.nick_name     as nickName,
           u.user_name     as userName,
           u.major         as major,
           p.id            as projectId,
           p.project_name  as projectName,
           p.start_time    as startTime,
           p.end_time      as endTime,
           ifnull(r.total_score, 0) as score
    from sys_project p
    join sys_flow_task ft on ft.project_id = p.id
    join sys_user u on u.user_id in (${visibleUserIds})
    left join sys_result r on r.p_id = p.id and r.u_id = u.user_id
    where p.start_time &gt;= #{param.startTime}
      and p.end_time   &lt;= #{param.endTime}
      <if test="param.projectId != null"> and p.id = #{param.projectId} </if>
      <if test="param.nickName != null and param.nickName != ''">
        and u.nick_name like concat('%', #{param.nickName}, '%')
      </if>
    order by score desc
</select>
```

> ⚠️ **务必避免 `${}` 拼接 SQL 造成注入**：`visibleUserIds` 应通过 `foreach` 展开为 `in (...)`，而不是 `${}`。上面的 `${visibleUserIds}` 仅示意，落地时必须改为：

```xml
and u.user_id in
<foreach collection="visibleUserIds" item="uid" open="(" separator="," close=")">
    #{uid}
</foreach>
```

**（2）折中方案（改动最小，立刻止血）**

若短期无法改造 SQL，至少：

- 删除 `//TODO 分页的代码`（功能已存在）。
- 把 `sort` 的比较器改为 `Comparator.comparing(ProjectVo::getScore, Comparator.nullsLast(Comparator.reverseOrder()))`。
- 在 `PageUtils` 返回中补 `totalPage`。
- 明确并固化 `rank` 语义，写入接口文档（推荐"全局排名"）。

**（3）让 `PageUtils` 补齐返回字段**

```java
map.put("total", list.size());
map.put("totalPage", totalPage);
map.put("page", page);
map.put("limit", rows);
map.put("list", listSort);
```

**验收方式**：构造 1 万名教师的项目结果数据，对比改造前后单次接口耗时与内存占用；验证第 2 页首条的 `rank` 是否符合约定的排名语义。

---

## 三、B 类：需要业务决策

### T-04 【P1】项目子项导入为任务时，是否清理原有数据

> 2026-10-02 包结构更新：本节保留最初的问题代码及方案示例；当前实现使用 `params.ProjectItemImportParam` 接收选中项、`params.ProjectItemSaveParam` 接收维护参数，`vo.ProjectItemVo` 仅用于查询返回。实际导入策略及职责以 `ProjectTaskImportService` 和《T-04 完整解决方案》为准。

**位置**：`soft_main/src/main/java/com/hnkjzyxy/ab/controller/ProjectController.java:613`

```java
@PostMapping("/project/item/insertIntoTask")
public void insertIntoTask(@RequestBody List<ProjectItemVo> projectItems) {
    //TODO 是否要删除原来的projectItem表中的数据？

    Integer projectId = projectItems.get(0).getProjectId();
    List<Task> tasks = projectItems.stream().map(projectItem -> {
        Task task = new Task();
        BeanUtils.copyProperties(projectItem, task);
        task.setTaskName(projectItem.getPname());
        task.setPId(projectId);
        return task;
    }).collect(Collectors.toList());
    taskService.saveBatch(tasks);
}
```

**问题分析**

1. **注释问错了对象**：`projectItem` 是"项目子项模板"，`task` 是"考核任务实例"。真正需要决策的是**是否清理该项目下已生成的历史 task**，而不是 `projectItem`。`projectItem` 作为模板应保留。
2. **不幂等**：同一项目反复点击"导入为任务"，会**重复插入 N 批 task**，导致教师端任务列表出现重复项。
3. **无事务**：`saveBatch` 中途失败留下半批数据。
4. **空集合越界**：`projectItems.get(0)` 在请求体为空数组时抛 `IndexOutOfBoundsException`（应返回 400）。
5. **无权限校验**：接口未校验调用者是否有该项目的管理权限（`Authentication` 参数甚至没接收）。
6. **`BeanUtils.copyProperties` 与 T-03 同类风险**：`ProjectItemVo` → `Task` 的字段名/类型若不一致会静默丢字段，需逐一核对后用显式映射。

**解决方案（需业务先确认语义，再落地）**

**第一步：确认业务语义（三选一）**

| 方案 | 行为 | 适用场景 |
|---|---|---|
| ① 全量替换（推荐） | 导入前删除该项目下**未产生结果**的旧 task，再插入 | 子项模板改动后重新下发，且已有人填过结果 |
| ② 差量合并 | 按（projectId + taskName）唯一键 upsert，新增不重不漏 | 增量下发，不动已有数据 |
| ③ 禁止重复 | 存在 task 即报"已导入，请先清理" | 导入为一次性动作 |

**第二步：无论选哪种，都要补上幂等与事务**

```java
/**
 * 将项目子项导入为考核任务
 *
 * @param projectItems 项目子项列表
 * @param authentication 当前登录用户
 */
@PostMapping("/project/item/insertIntoTask")
@Transactional(rollbackFor = Exception.class)
public ApiResult insertIntoTask(@RequestBody @NotEmpty List<ProjectItemVo> projectItems,
                                Authentication authentication) {
    if (CollectionUtils.isEmpty(projectItems)) {
        return ApiResult.fail("项目子项列表不能为空");
    }
    Integer projectId = projectItems.get(0).getProjectId();
    // 1. 权限校验：仅项目管理员 / 超管可导入
    projectService.checkProjectManagePermission(projectId, authentication);

    // 2. 幂等策略（示例为"全量替换，保留已有结果的 task"）
    projectService.rebuildTasksFromItems(projectId, projectItems);

    return ApiResult.ok();
}
```

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void rebuildTasksFromItems(Integer projectId, List<ProjectItemVo> items) {
    // 已被教师提交过结果的任务不能删（外键 / 结果表引用）
    Set<Integer> lockedTaskIds = resultMapper.findTaskIdsByPId(projectId);

    List<Task> oldTasks = taskMapper.selectList(
            new QueryWrapper<Task>().eq("p_id", projectId));
    List<Integer> deletable = oldTasks.stream()
            .map(Task::getId)
            .filter(id -> !lockedTaskIds.contains(id))
            .collect(Collectors.toList());
    if (!deletable.isEmpty()) {
        taskMapper.deleteBatchIds(deletable);
    }

    // 显式映射，不依赖 BeanUtils 反射
    List<Task> tasks = items.stream().map(item -> {
        Task task = new Task();
        task.setTaskName(item.getPname());
        task.setPId(projectId);
        task.setCategoryId(item.getCategoryId());
        // ... 其余字段逐一 set，避免类型不匹配静默丢失
        return task;
    }).collect(Collectors.toList());

    taskService.saveBatch(tasks);
}
```

**第三步：注释与接口文档同步**

- 在 Controller 的 Javadoc 中写明采用的是 ①②③ 中哪种策略（团队使用 ApifoxHelper，**必须用 Javadoc 块注释**，不能用 `//`）。
- 删除原 `//TODO 是否要删除原来的projectItem表中的数据？`，改为明确说明"保留 projectItem 模板，重建 task 实例"。

**待业务确认项**

- 已产生考核结果的 task 被重新导入时如何处理？（保留 / 归档 / 报错）
- 导入是否需要审批或二次确认？
- 是否需要记录导入操作日志（谁、何时、导入了多少条）？

---

## 四、C 类：残留占位注释（已实现，仅需清理）

### T-11 `ProjectServiceImpl.java:540` —— 裸 `//TODO`

```java
List<ProjectVo> projectVos = new ArrayList<>();
//查询所有项目接收的所有用户id
//TODO
if (role.getWeight().intValue() >= HnkjzyEncode.LEADER.getCode()) {
```

**分析**：紧邻的四段 `if / else if`（校级领导 → 院长 → 教研室主任 → 普通教师）已经把"查询所有项目接收的用户 id + 按角色收窄可见范围"实现了，权限分支完整。该 TODO 是开发过程的草稿标记。

**处理**：

1. 删除 `//TODO` 这行，保留 `//查询所有项目接收的所有用户id` 这行有效说明。
2. 顺带修掉本方法另外两处真实隐患（与 T-05 同源）：
   - `Role role = userRoleMapper.getRoleWeight(...)` 可能为 `null` → 改为 `roleAssert.requireRole(...)`。
   - `role.getWeight().intValue() == HnkjzyEncode.DEAN.getCode()` 与 `role.getWeight().equals(HnkjzyEncode.DIRECTOR.getCode())` **风格不一致**（一个走 `intValue()`，一个走 `equals`），`Integer.equals` 对超范围值不受缓存影响但现在阈值都在 `[-128,127]`，风险低；仍建议统一为 `intValue() == xxx.getCode()` 以避免后续踩坑。

### T-12 `FlowServiceImpl.java:560` —— `//TODO 判断是否已审批`

```java
//TODO 判断是否已审批
LambdaQueryWrapper<ResultItem> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(ResultItem::getPId, resultItem.getPId())
        .eq(ResultItem::getUId, resultItem.getUId())
        .eq(ResultItem::getUserId, user.getUserId())
        .eq(ResultItem::getStatus, 1)
        .eq(ResultItem::getStep, vo.getSort());
Integer count = resultItemService.count(wrapper);
if (count > 0) {
    throw new RuntimeException("已进行审批，不能再重复审批！");
}
```

**分析**：**重复审批校验已经实现**，TODO 是残留注释。同时发现两处真实问题：

1. **裸抛 `RuntimeException`**：与项目其他地方的 `BusinessException` 不统一，前端拿不到稳定错误码。
2. **校验维度不完整（需业务确认）**：当前只拦截"**同一审批人**在同一 `step` 重复审批"。若同一 `step` 配置了多个审批人，**不同审批人各点一次是允许的**（这通常是设计意图，无需修改）；但若业务要求"同一 step 只能有一人审批"，则需要额外加 `step` 维度的唯一性校验。
3. **并发窗口**：`count` 查询与 `resultItemMapper.insert` 之间没有唯一约束保护，两人同时提交仍可能双写。建议在 `sys_result_item` 上对 `(p_id, u_id, user_id, step, status=1)` 加**唯一索引**，让数据库兜底；或对 `submitApprove` 加基于 `(pId,uId,step)` 的分布式锁。

**处理建议**：

```java
// 1. 删除 TODO 注释，改为说明性注释
// 校验：同一审批人在同一审批步骤不得重复审批
LambdaQueryWrapper<ResultItem> wrapper = new LambdaQueryWrapper<>();
...

// 2. 使用业务异常
if (count > 0) {
    throw new BusinessException(ErrorCode.ALREADY_APPROVED, "已进行审批，不能再重复审批！");
}

// 3. 数据库层兜底（迁移脚本）
// ALTER TABLE sys_result_item
//   ADD UNIQUE KEY uk_approve_once (p_id, u_id, user_id, step, is_flag);
```

### （T-13 / T-14）类注释中的 `@description: TODO`

**位置**：`CourseServiceImpl.java:26`、`CheckResultServiceImpl.java:26`

```java
/**
 * @version 1.0
 * @projectName: assessment
 * @author: Lucas
 * @description: TODO      ← 模板占位，未填写
 * @date: 2024/4/23 20:25
 */
```

**分析**：纯模板占位，无功能影响。但按团队约定（ApifoxHelper 解析规范），**类级 Javadoc 第一行会被当作 Apifox 目录名**，`@description` 留空会让文档可读性下降。

**处理**：

```java
/**
 * 课程管理
 * 负责课程信息维护、课表 Excel 导入与教务数据同步
 *
 * @version 1.0
 * @projectName: assessment
 * @author: Lucas
 * @date: 2024/4/23 20:25
 */
```

```java
/**
 * 教学巡查结果管理
 * 负责巡查记录 Excel 导入、结果查询与统计
 *
 * @version 1.0
 * @projectName: assessment
 * @author: Lucas
 * @date: 2024/4/23 20:25
 */
```

---

## 五、关联发现（非 TODO，但同批建议处理）

扫描过程中发现以下与上述 TODO 同源或紧邻的问题，建议一并排期：

| 编号 | 位置 | 问题 | 建议 |
|---|---|---|---|
| R-01 | `HnkjzyEncode` | `DEAN(9,"院长")` 与 `CLERK(9,"书记")` **权重相同**，`DIRECTOR(5,"教研室主任")` 与 `DEPARTMENT(2,"教研室")` / `COUNSELLOR(2,"辅导员")` 存在权重重叠 | 明确每个枚举的 `roleCode`，权限判定改用 `roleCode` 或 `roleId`，不要用权重做唯一区分（直接影响 T-05/T-06/T-11） |
| R-02 | `ResultServiceImpl` | 权限异常以 `RuntimeException` 抛出 → HTTP 500 | 引入 `BusinessException` + 全局 `@RestControllerAdvice`，统一返回 4xx + 业务码 |
| R-03 | `PageUtils` | 返回体缺 `totalPage` | 补齐，见 T-10 |
| R-04 | `OaRequestAPIUtils` / `CourseScheduleSyncJob` / `CheckResultDataListener` | 大量 `System.out.println` 打印业务数据（含 token） | 统一换 `@Slf4j`，走日志级别与脱敏 |
| R-05 | `CourseScheduleSyncJob` | `TruncateTable` 使用的 DDL 语句无法回滚 | 改用 `DELETE`，或明确标注该方法**必须在事务外且带备份** |
| R-06 | `ProjectServiceImpl:251/267` | 两处 `//未完成` 注释后接 `return 0`，与 `isFinish` 状态码含义耦合，缺乏常量 | 抽出 `ProjectStatusEnum` 常量类，替换魔法数字 `0/1/2/3/4/5` |

---

## 六、落地清单（可直接作为任务卡）

| 任务卡 | 内容 | 关联 TODO | 预估 | 依赖 |
|---|---|---|---|---|
| IMP-01 | `OaApiClient` 化改造 + 密钥外置，**并吊销历史密钥** | T-02 | 0.5d | 需 OA 侧配合重新签发 |
| IMP-02 | `CourseScheduleSyncJob` 改为"校验后整体替换 + 分布式锁 + 可配置开关" | T-01 | 0.5d | IMP-01（注入 `OaApiClient`） |
| IMP-03 | `CheckResultDataListener` 显式映射 + 容错解析 + 错误行回执 | T-03 | 1d | 无 |
| IMP-04 | `RoleAssert` 工具 + 全局异常处理；改造 `ResultServiceImpl`、`ProjectServiceImpl` 权限点 | T-05 / T-06 / T-11(R-02) | 1d | 需先确认 R-01 角色口径 |
| IMP-05 | `insertIntoTask` 幂等 + 事务 + 权限校验（**先定策略**） | T-04 | 0.5d | 需业务确认 |
| IMP-06 | `PdfToWordConverter` 多页 / 等比 / 路径 / 资源释放重构 | T-07 / T-08 / T-09 | 1d | 无 |
| IMP-07 | 优秀项目列表分页下推 SQL + `rank` 语义固化 | T-10 | 1.5d | 需接口契约确认 |
| IMP-08 | `submitApprove` 重复审批并发兜底（唯一索引 + 业务异常） | T-12 | 0.5d | 含 DDL |
| IMP-09 | 清理全部残留 TODO 与类注释占位；补 `PageUtils.totalPage` | T-11~T-14 / R-03 | 0.3d | 随对应改动合并提交 |
| IMP-10 | 增加 CI 卡口：禁止新增无归属 TODO | — | 0.2d | 见下方约定 |

---

## 七、防止 TODO 再次堆积的长效约定

1. **TODO 必须带归属信息**，格式统一为：

   ```java
   // TODO(负责人, 期待日期, 关联需求/缺陷号): 具体待办事项
   // 例：// TODO(zhangsan, 2026-10-15, #1234): 待接入新的教务接口
   ```

2. **CI 卡口**：在流水线加一个轻量校验脚本，命中 `//TODO`（不含上述规范格式）即告警，`//TODO` 出现在 `release` 分支即失败。

   ```bash
   # 建议加入 pre-commit / CI
   grep -rnE '//\s*TODO(?![(（])' --include='*.java' soft_*/src/main/java || true
   ```

3. **禁止把"已实现"的注释留在代码里**：本次 14 处中有 3 处（T-11/T-12 及类注释）就是"实现后忘删"造成的噪音，评审时明确要求清理。

4. **代码评审检查项**：
   - 新增的 `truncate` / `delete` 类操作是否可回滚、是否有前置校验？
   - 新增的密钥/账号是否走了配置或环境变量？
   - 新增的 `BeanUtils.copyProperties` 是否核对过字段名与类型？
   - 新增的权限判定是否使用 `roleCode` 而非角色名字符串？是否处理了"无角色"分支？

---

## 八、待确认项（需业务 / 架构方答复）

1. `getList` 与 `getLists` 的**准确授权范围**：超管是否可见？"书记"（权重同为 9）是否等同院长？（影响 IMP-04）
2. 优秀项目列表的 `rank` 语义：**全局排名**还是**当前页排名**？（影响 IMP-07）
3. `insertIntoTask` 采用"全量替换 / 差量合并 / 禁止重复"哪一种？已产生结果的 task 如何处理？（影响 IMP-05）
4. 同一审批 `step` 配置多个审批人时，是否允许**每人都提交一次**？（影响 IMP-08）
5. `sys_course_schedule` 的同步是否允许"接口异常时保留上一次数据"（本文默认方案），还是必须"如实清空"？（影响 IMP-02）

---

## 附录：扫描命令与结果对照

```bash
# 全量扫描
grep -rnE '(TODO|FIXME|XXX|HACK)' \
  --include='*.java' --include='*.xml' --include='*.yml' \
  --include='*.sql' --include='*.properties' \
  --exclude-dir=target .

# 仅看真实待办（排除 README 示例）
grep -rnE '//\s*TODO' --include='*.java' --exclude-dir=target .
```

| 文件 | 行 | 对应编号 |
|---|---|---|
| `soft_main/.../config/CourseScheduleSyncJob.java` | 47 | T-01 |
| `soft_common/.../utils/OaRequestAPIUtils.java` | 53 | T-02 |
| `soft_service/.../listener/CheckResultDataListener.java` | 87 | T-03 |
| `soft_main/.../controller/ProjectController.java` | 613 | T-04 |
| `soft_service/.../impl/ResultServiceImpl.java` | 214 / 234 | T-05 / T-06 |
| `soft_common/.../utils/PdfToWordConverter.java` | 67 / 71 / 128 | T-07 / T-09 / T-08 |
| `soft_service/.../impl/ProjectServiceImpl.java` | 540 / 631 | T-11 / T-10 |
| `soft_service/.../impl/FlowServiceImpl.java` | 560 | T-12 |
| `soft_service/.../impl/CourseServiceImpl.java` | 26 | T-13 |
| `soft_service/.../impl/CheckResultServiceImpl.java` | 26 | T-14 |

> 构建提示：本项目 `pom.xml` 中 `java.version=1.8`，**必须用 JDK 1.8 编译**：
> `JAVA_HOME=D:/CodeEnvironment/JDK1.8 mvn -B compile -DskipTests`
> 使用 JDK 21 会因 Lombok 版本过旧报 `NoSuchFieldError: JCTree$JCImport.qualid`（与本次改动无关）。
