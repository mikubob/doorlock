# T-02 OA 接口密钥明文硬编码 —— 完整解决方案

> 对应问题：`项目TODO清单分析与解决方案.md` 中 **T-02【P0】**
> 原 TODO 位置：`soft_common/src/main/java/com/hnkjzyxy/ab/utils/OaRequestAPIUtils.java:53`
> 关联任务卡：**IMP-01**（`OaApiClient` 化改造 + 密钥外置，并吊销历史密钥）
> 计划编写时间：2026-10-01
> 本文状态：**仅计划，未改动任何代码**
> 编译/验证方式（实施时执行）：`JAVA_HOME=D:/CodeEnvironment/JDK1.8 mvn -B compile -DskipTests`
> 数据库变更：**无 DDL**（零改表）

---

## 一、问题定性

原 TODO 注释写着「构建一个 json 对象，key、secret 的值在应用查看中获取」，看起来只是一处「写法待优化」的提醒。但真正的问题是三层叠加的：

1. **生产密钥明文进了 Git**（安全事件，需要吊销）；
2. **密钥写死在 `static` 常量里、无法按环境切换**（工程缺陷）；
3. **整个 OA 调用工具类是静态方法 + 吞异常**（架构缺陷，让 T-01 的「null 视为失败」只能兜底而无法根治）。

三者互相纠缠：不把它改成 Spring Bean，就无法注入配置；不注入配置，密钥就只能继续硬编码。

### 1.1 原代码的问题链

```java
public class OaRequestAPIUtils {                       // ① 全静态工具类，非 Spring Bean

    private static final String tokenURL =
        "https://dmp.hnkjxy.net.cn/open_api/authentication/get_access_token";   // ② URL 也硬编码

    public static String getAccessToken() {
        String accessToken = "";
        try {
            ...
            //TODO 构建一个json对象，key,secret的值在应用查看中获取(OA系统申请的)
            String input = "{\"key\":\"20250622398919366238010517831682779\"," +
                           "\"secret\":\"e6c226b86065d7a8df88a5e3119ba6d0491b791a\"}"; // ③ 密钥明文
            OutputStream outputStream = httpConnection.getOutputStream();
            outputStream.write(input.getBytes());           // ④ 未指定字符集
            if (httpConnection.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + ...);
            }
            ...
            System.out.println(accessToken);                // ⑤ 把 token 打进标准输出（R-04）
            httpConnection.disconnect();                    // ⑥ 未放在 finally，异常时连接泄漏
        } catch (Exception e) {
            e.printStackTrace();                            // ⑦ 吞掉异常，返回空串
        }
        return accessToken;                                 // ⑧ 调用方拿到 ""，无法区分「失败」与「没数据」
    }

    public static String getClassBoardData(String xq, String jzwmc) {
        ...
        String accessToken = getAccessToken();              // ⑨ token 为空串照样拼进 URL
        URL targetUrl = new URL(classBoardURL + accessToken + "&per_page=1000");
        ...
        } catch (Exception e) {
            System.err.println("调用接口或解析数据时发生异常：" + e.getMessage());
            e.printStackTrace();
        }
        return dataJson;                                    // ⑩ 失败时返回 null
    }
}
```

| 序号 | 风险 | 实际后果 |
|---|---|---|
| ③ | **生产 `key`/`secret` 明文提交进 Git** | 任何拿到仓库（含历史）的人都能直接调用 OA 开放平台接口，属明确**安全事件** |
| ③ | 密钥写死、无配置项 | 测试 / 生产共用同一份密钥，无法按环境切换，无法轮换 |
| ① | 静态方法 + 静态常量 | `static` 字段无法用 `@Value` / `@ConfigurationProperties` 注入，**改造必须先破「静态」** |
| ② | URL 也硬编码在常量里 | `base-url`、`per_page` 无法配置化（`per_page=1000` 是 T-01 分页截断的根因，见 T-01 七、边界） |
| ④ | `input.getBytes()` 未指定字符集 | 依赖平台默认编码，跨环境可能乱码 |
| ⑤ | `System.out.println(accessToken)` | **token 打入日志**（R-04），日志被读取即等于凭据泄露 |
| ⑥ | `disconnect()` 不在 `finally` | HTTP 连接可能泄漏 |
| ⑦⑧⑩ | 异常吞掉 | 「网络失败 / 鉴权失败 / 业务码错误 / 真的没数据」四种情况调用方**完全无法区分** |

**关键放大器**：`getClassBoardData` 把异常全部吞掉、失败返回 `null`。T-01 只能约定「`null` 即失败」，但这仍是**猜**——`null` 既可能是网络超时，也可能是 OA 当天真的没有排课数据。**T-01 的兜底解决了「不丢数据」，但没有解决「分不清原因」**，这个尾巴必须由 T-02 收掉。

### 1.2 影响面

| 维度 | 影响 |
|---|---|
| **凭据安全** | `soft_common` 是基础模块，被打进 fat jar 分发；密钥同时存在于**源码、编译产物、Git 历史**三处。仅从当前代码删除**不足以消除风险**，必须在 OA 侧吊销重发 |
| **业务连续性** | `getClassBoardData` 被课表同步（T-01）依赖；`queryHotelDataByToday` 被 `ScheduleController#checkResultAddOrEdit`（巡查记录新增）依赖。改造若破坏这两条链路，对应**课表同步与巡查录入会同时受影响** |
| **合规** | 密钥明文属于审计明确的扣分项，也影响后续接入其他开放平台时的信任评估 |

### 1.3 现状调用点（全仓已核对，共 2 处业务调用）

| 调用点 | 位置 | 调用的方法 |
|---|---|---|
| 课表同步 | `soft_service/.../impl/CourseScheduleServiceImpl.java:26`（`import static ...getClassBoardData`），实际使用在 `doSync` 第 196 行 | `getClassBoardData("", "")` |
| 巡查记录新增 | `soft_main/.../controller/ScheduleController.java:11,80` | `OaRequestAPIUtils.queryHotelDataByToday(classes)` |

> 另有一处 `main` 方法（`OaRequestAPIUtils.java:219`）为本地测试入口，改造时一并移除。
> **范围可控**：业务调用仅 2 处，是「先改基础设施、再改调用方」这种标准节奏的理想规模。

---

## 二、解决方案设计

核心思路一句话：**把「密钥硬编码的静态工具类」改造成「配置驱动的 Spring Bean」，并把「吞异常返回 null」改成「抛出带语义的业务异常」。**

```
        ┌──────────────────────────────────────────────┐
        │  配置层  OaProperties(@ConfigurationProperties) │
        │  oa.base-url / oa.app-key / oa.app-secret ...  │
        │  值来源：环境变量 OA_APP_KEY / OA_APP_SECRET    │
        └────────────────────┬─────────────────────────┘
                             ▼ 注入
        ┌──────────────────────────────────────────────┐
        │  客户端层  OaApiClient(@Component)              │
        │  · ObjectMapper 构造 JSON（不再手拼字符串）      │
        │  · token 内存缓存 + 提前 5 分钟过期             │
        │  · 失败抛 OaApiException（不再返回 null / ""）   │
        │  · 日志脱敏（不打印完整 token）                  │
        └────────────────────┬─────────────────────────┘
                             ▼ 注入
        ┌──────────────────────┐   ┌──────────────────────────┐
        │ 调用方 1：课表同步     │   │ 调用方 2：巡查记录录入     │
        │ CourseScheduleServiceImpl │ ScheduleController        │
        └──────────────────────┘   └──────────────────────────┘
                             ▼
        ┌──────────────────────────────────────────────┐
        │  旧类  OaRequestAPIUtils  →  整体删除           │
        │  （含 main 测试入口；保留一个静态调用点就是复发种子）│
        └──────────────────────────────────────────────┘
```

### 防线 1（最重要）：密钥完全外置，源码中不再出现任何凭据

**原则：仓库里不允许出现真实的 `key`/`secret` 值。**

| 层 | 做法 |
|---|---|
| 配置文件 | `application-*.yml` 只写占位符 `${OA_APP_KEY:}`，**默认值为空**，不写真实值 |
| 运行环境 | 通过**环境变量**（或配置中心）注入 `OA_APP_KEY` / `OA_APP_SECRET` |
| 本地覆盖 | 允许 `application-local.yml` 等本地文件承载，但必须被 `.gitignore` 排除 |
| 启动校验 | 密钥为空时**启动即快速失败**（见防线 6），不做「静默降级为匿名调用」 |

```yaml
# application.yml —— 只放占位符，绝不出现真实密钥
oa:
  base-url: https://dmp.hnkjxy.net.cn
  app-key: ${OA_APP_KEY:}
  app-secret: ${OA_APP_SECRET:}
  connect-timeout: 10000
  read-timeout: 30000
  token-cache-seconds: 3300       # 55 分钟，提前 5 分钟过期
  page-size: 1000                 # 从硬编码常量上移，供 T-01 分页闸门共用
```

> **为什么默认值留空、并让空值启动失败**：如果给一个「能跑通」的默认值，就会掩盖「环境没配好」这件事，让问题推迟到线上第一次同步才暴露。空值 + 启动校验 = 把配置问题挡在启动期。

### 防线 2：静态工具类 → Spring Bean（`OaApiClient`）

**为什么要改静态**：`static` 字段不能被 Spring 注入。只要还是 `static`，密钥就只能写死在代码里——这是硬约束，不是风格偏好。

| 维度 | 静态工具类（原） | Spring Bean（新） |
|---|---|---|
| 配置注入 | ❌ 不可能 | ✅ 构造器注入 `OaProperties` |
| 可替换 / 可测试 | ❌ 只能调真实 OA | ✅ 可注入 mock，单测不打外网 |
| 生命周期 | ❌ 无，随处可调 | ✅ 由容器管理，天然单例可缓存 token |
| 依赖显式化 | ❌ `import static` 隐式耦合 | ✅ 构造器参数即依赖声明 |

### 防线 3：手拼 JSON → `ObjectMapper`

原实现用字符串拼接构造请求体，`key`/`secret` 中一旦出现 `"` 或 `\` 就会生成**非法 JSON**（也恰好是那条 TODO 注释想表达的事）。

新实现统一用项目已有的 Jackson：

```java
private final ObjectMapper objectMapper = new ObjectMapper();

Map<String, String> body = new HashMap<>();
body.put("key", props.getAppKey());
body.put("secret", props.getAppSecret());
byte[] payload = objectMapper.writeValueAsBytes(body);   // 自动转义，无手拼
```

> 顺带修掉 ④：写入时**显式指定 UTF-8**，不再依赖平台默认编码。
> 注意：原代码引用了 `org.json.JSONObject` / `JSONArray`，项目同时存在 Jackson 与 org.json 两套 JSON 库；本次统一走 Jackson，**不引入新依赖**。

### 防线 4：token 缓存 + 提前过期

原实现**每次调用接口都重新取一次 token**。课表同步与巡查录入都会触发，既浪费 OA 配额也拉长响应。

```java
/** 缓存 access_token，提前 5 分钟过期，避免边界失效 */
private volatile String cachedToken;
private volatile long expireAt;

public String getToken() {
    if (cachedToken != null && System.currentTimeMillis() < expireAt) {
        return cachedToken;
    }
    synchronized (this) {                                    // 双检，避免并发重复取 token
        if (cachedToken != null && System.currentTimeMillis() < expireAt) {
            return cachedToken;
        }
        String token = requestAccessToken();
        cachedToken = token;
        expireAt = System.currentTimeMillis() + props.getTokenCacheSeconds() * 1000L;
        return token;
    }
}
```

> 缓存时长取 `token-cache-seconds` 配置（默认 3300 秒），**不写死**，便于 OA 侧调整有效期后无需改代码。

### 防线 5：吞异常 → 抛 `OaApiException`（含语义）

这是收掉 T-01 尾巴的关键。

| 场景 | 原行为 | 新行为 |
|---|---|---|
| 网络超时 | 返回 `""` / `null` | 抛 `OaApiException("调用 OA 鉴权接口超时", e)` |
| HTTP 非 200 | 抛 `RuntimeException`（被 catch 吞掉） | 抛 `OaApiException("OA 鉴权失败，HTTP " + code)` |
| 业务码非 10000 | `System.err` 打印后返回 `null` | 抛 `OaApiException("OA 返回业务错误 code=..., message=...")` |
| 真的没有排课数据 | 返回 `null`（与失败无法区分） | 返回**空数组 `[]`**，与失败**语义分离** |

改造后，T-01 的 `doSync` 第一段可以写成**不带猜测**的判断：

```java
// 失败会直接抛 OaApiException（带原因），走到这里的必定是「拿到了合法响应」
JsonNode data = oaApiClient.getClassBoardData("", "");   // 保证非 null，可能为空数组
// 空数组交给 T-01 的「数据量闸门」判断，而不是在这里 return
```

> **边界说明**：HttpClient 层面仍建议保留「抛异常」而非「返回 null」的语义——**空数组是业务事实，异常是技术故障，二者绝不相同**。

### 防线 6：启动即校验密钥（快速失败）

密钥缺失是**部署配置问题**，不是运行期问题，必须挡在启动期：

```java
@Component
public class OaPropertiesValidator implements InitializingBean {
    private final OaProperties props;
    @Override
    public void afterPropertiesSet() {
        if (!StringUtils.hasText(props.getAppKey()) || !StringUtils.hasText(props.getAppSecret())) {
            throw new IllegalStateException(
                "OA 应用凭据未配置：请通过环境变量 OA_APP_KEY / OA_APP_SECRET 注入（禁止写回源码）");
        }
        log.info("OA 凭据已加载（key={}）", mask(props.getAppKey()));   // 脱敏展示
    }
}
```

> **与 T-01 的「Lua 脚本漏打包启动即失败」同一思想**：宁可启动失败，也不要在凌晨 3 点定时同步时才报错。

### 防线 7：日志脱敏（顺带处理 R-04）

| 位置 | 原行为 | 新行为 |
|---|---|---|
| 取 token | `System.out.println(accessToken)` **完整打印** | `log.debug("已获取 OA token，前 6 位：{}", mask(token))` |
| 拉数据 | `System.out.println(dataJson)` 打印**全部报文** | `log.debug("拉取到 OA 数据 {} 条", count)`，正文不打 |
| 异常 | `System.err` + `printStackTrace` | `log.error("...", e)`，走日志框架级别控制 |

脱敏工具统一保留首尾 4 位、中间以 `****` 替代：

```java
private static String mask(String s) {
    if (s == null || s.length() <= 8) { return "****"; }
    return s.substring(0, 4) + "****" + s.substring(s.length() - 4);
}
```

> ⑤ 的 `System.out` 是**真问题**：日志系统通常会被集中采集、长期留存，token 打进日志等于凭据长期可被读取。

### 防线 8：连接管理

- `disconnect()` **移入 `finally`**（原来只在成功路径调用，异常时连接泄漏）；
- 流读取使用 try-with-resources（原实现手工管理，异常路径同样泄漏）；
- 若项目后续引入统一 HTTP 客户端（如 OkHttp / RestTemplate + 连接池），`OaApiClient` 的**接口不变**，只换内部实现——这正是「Bean 化」带来的收益。

---

## 三、改动清单（计划，尚未执行）

### 3.1 新增文件（4 个）

| 文件 | 作用 |
|---|---|
| `soft_common/.../config/OaProperties.java` | `oa.*` 配置绑定（`@ConfigurationProperties`，由 `@ConfigurationPropertiesScan` 注册，**不加 `@Component`**，遵循 T-01 已确立的约定） |
| `soft_common/.../client/OaApiClient.java` | OA 调用客户端 Spring Bean：JSON 构造 / token 缓存 / 异常语义化 / 日志脱敏 |
| `soft_common/.../exception/OaApiException.java` | OA 调用异常（携带 HTTP 码 / 业务码 / 原始 message，便于排查与告警） |
| `soft_main/.../init/validation/OaPropertiesValidator.java` | 启动期校验凭据非空，缺失即快速失败（含脱敏日志） |

> **模块归属说明**：三个类都放 `soft_common`，因为要被 `soft_service`（课表同步）和 `soft_main`（Controller）同时使用。遵循 T-01 已确认的依赖方向：`soft_service` → `soft_mapper` + `soft_common`（**不依赖 soft_main**），所以基础能力应下沉到 `soft_common`。校验器只服务启动流程，放 `soft_main`。

### 3.2 修改文件（4 个）

| 文件 | 改动 |
|---|---|
| `soft_common/.../utils/OaRequestAPIUtils.java` | **整体删除**（含 `main` 入口）。保留一个「密钥写死」的类就是事故复发的种子——同 T-01 删除 `truncateTable()` 的处理方式 |
| `soft_service/.../impl/CourseScheduleServiceImpl.java` | 删除 `import static ...getClassBoardData`；改为构造器注入 `OaApiClient`；`doSync` 首行改为 `oaApiClient.getClassBoardData("", "")`（**其余校验/替换逻辑完全不动**） |
| `soft_main/.../controller/ScheduleController.java` | 注入 `OaApiClient`；`checkResultAddOrEdit` 中的 `OaRequestAPIUtils.queryHotelDataByToday(...)` 改为客户端调用 |
| `soft_main/src/main/resources/application-{dev,test,prd}.yml` | 新增 `oa.*` 节点（占位符 + 超时 + 缓存时长）；**不加任何真实密钥** |

### 3.3 数据库

无 DDL，无数据订正。

---

## 四、配置说明（实施时写入）

```yaml
# application.yml（公共默认；dev/test/prd 按需覆盖 base-url）
oa:
  base-url: https://dmp.hnkjxy.net.cn     # dev/test/prd 可各自覆盖
  app-key: ${OA_APP_KEY:}                 # 只能由环境变量/配置中心注入，源码禁止出现真实值
  app-secret: ${OA_APP_SECRET:}
  connect-timeout: 10000
  read-timeout: 30000
  token-cache-seconds: 3300               # 55 分钟，提前 5 分钟过期
  page-size: 1000                         # 与 T-01 分页截断闸门共用同一语义
```

| 配置项 | dev | test | prd | 说明 |
|---|---|---|---|---|
| `oa.base-url` | OA 测试域名 | OA 测试域名 | OA 生产域名 | 各环境独立 |
| `oa.app-key` / `oa.app-secret` | 环境变量 | 环境变量 | 环境变量 | **统一走环境变量，yml 内永远为空占位符** |

**部署环境注入示例**：

```bash
# Linux 环境变量
export OA_APP_KEY="<OA 侧重新签发的 key>"
export OA_APP_SECRET="<OA 侧重新签发的 secret>"

# Docker / K8s：写入 Secret 后以 env 注入
```

**`.gitignore` 需补充（本次一并处理）**：

```gitignore
# 本地凭据覆盖文件，禁止提交
application-local.yml
application-local-*.yml
*.local.yml
```

---

## 五、验收用例（实施后执行）

### 5.1 凭据安全（核心）

| 用例 | 构造方式 | 断言 |
|---|---|---|
| A-1 源码无真实密钥 | `git grep -nE 'e6c226b86065|202506223989193662'` | **无命中**（当前代码与本次改动后均为空） |
| A-2 全仓无残留 | `grep -rnE '(secret|appKey|app_secret)\s*[:=]\s*"[0-9a-f]{20,}"' --include='*.java' --include='*.yml' .` | 无真实值命中 |
| A-3 历史提交确认 | `git log -S "e6c226b86065" --oneline` | 列出所有含该密钥的历史提交，作为「必须吊销」的证据留存 |
| A-4 密钥空值快速失败 | 不设 `OA_APP_KEY` 启动 | **启动即失败**，日志含「OA 应用凭据未配置」；不进入运行期 |
| A-5 密钥正常注入 | 设置环境变量启动 | 启动成功，日志打印**脱敏** key（`2025****2779`），无完整密钥 |
| A-6 无明文日志 | 触发一次同步后检索日志 | `grep -c 'access_token\|e6c226b86065' app.log` → **0**（token 不再完整打印） |

### 5.2 功能回归（不能改坏现有链路）

| 用例 | 构造方式 | 断言 |
|---|---|---|
| B-1 课表同步仍可用 | 触发 `/api/courseSchedule/refresh` | 返回 `success=true`，行数与 `savedRows` 一致（T-01 行为不变） |
| B-2 巡查录入仍可用 | 调用 `POST /schedule`（`checkResultAddOrEdit`） | `peopleLeave` 正常写入，不抛异常 |
| B-3 token 只取一次 | 连续调用 2 次接口，观察 OA 侧日志 | 第二次**未**再次请求鉴权接口（命中缓存） |
| B-4 缓存过期后重取 | 手工将 `expireAt` 置过期后再调用 | 重新请求鉴权接口并刷新缓存 |
| B-5 并发取 token | 多线程同时首次调用 | 只发出**一次**鉴权请求（`synchronized` 双检生效） |

### 5.3 异常语义

| 用例 | 构造方式 | 断言 |
|---|---|---|
| C-1 OA 超时 | 断网 / base-url 指向不可达 | 抛 `OaApiException`，message 含「超时」；课表同步侧被 T-01 捕获、**数据不变** |
| C-2 HTTP 500 | Mock 返回 500 | 抛 `OaApiException`，含 HTTP 码 |
| C-3 业务码非 10000 | Mock 返回 `{"code":1,...}` | 抛 `OaApiException`，含 `code` 与 `message` |
| C-4 合法空数组 | Mock 返回 `{"code":10000,"result":{"data":[]}}` | **不抛异常**，返回空数组；由 T-01 数据量闸门决定放弃（语义与失败分离 ✅） |
| C-5 连接不泄漏 | 制造异常后观察连接数 | 连接被 `finally` 释放 |

### 5.4 编译与打包

```bash
JAVA_HOME=D:/CodeEnvironment/JDK1.8 mvn -B compile -DskipTests   # 期望 BUILD SUCCESS
mvn -B clean package -DskipTests
jar tf soft_main/target/soft_main-1.0.0.jar | grep -i oa         # 仅见 OaApiClient / OaProperties 等类
```

> 构建提示（沿用 T-01 结论）：`pom.xml` 中 `java.version=1.8`，**必须用 JDK 1.8 编译**。用 JDK 21 会因 Lombok 版本过旧报 `NoSuchFieldError: JCTree$JCImport.qualid`（与本次改动无关）。

---

## 六、上线与回滚（含密钥吊销，本次的重点）

### 6.1 上线顺序（有一处硬依赖，必须按序）

> **硬依赖**：**新凭据必须在代码发布前就绪**，否则发布即启动失败（防线 6 的「空值快速失败」是**有意设计**）。
> 因此顺序必须是：**先拿新密钥 → 再配环境变量 → 最后发布代码**。

1. **【必须先做，需 OA 侧配合】** 联系 OA 平台，说明原 `key`/`secret` 已随源码泄露，**吊销当前密钥并重新签发**；
2. 将**新** `OA_APP_KEY` / `OA_APP_SECRET` 配置到各环境的环境变量 / 配置中心（**dev、test、prd 三套独立**）；
3. 在**预发环境**验证：启动成功 + 课表同步一次成功 + 巡查录入一次成功；
4. 发布本次代码（无 DDL，无需停服，可滚动发布）；
5. 发布后确认日志中**无**明文 token / 密钥；
6. 用 `git log -S "e6c226b86065"` 的记录留档，作为安全事件处理凭证。

### 6.2 回滚

- **代码回滚**：`git revert` 本次提交即可。数据库结构无变更。
- **注意**：回滚本次代码会**同时回滚掉「密钥走环境变量」的能力**，退回硬编码旧密钥——而旧密钥应已被吊销，**回滚后会因鉴权失败而不可用**。
  **因此：本次改造不建议代码回滚**；如必须回滚，需先与 OA 侧确认旧密钥是否仍有效，或把新密钥临时写回配置（仅限紧急窗口，事后必须再改回外置）。
- **降级开关**：若希望「配置未就绪时先不影响其他功能」，可考虑给 T-01 的同步入口加一个 `oa.enabled` 开关（**列为待确认项**，默认不加，避免又一处「静默降级」）。

### 6.3 值班关注点

| 日志关键字 | 含义 | 处理 |
|---|---|---|
| `OA 应用凭据未配置` | 环境变量漏配，**启动已失败** | 补环境变量后重启 |
| `调用 OA 鉴权接口超时` | OA 侧不可达或网络问题 | 检查 OA 服务与网络 |
| `OA 返回业务错误 code=` | 密钥无效 / 额度用尽 | 核对密钥与 OA 侧状态 |
| `已获取 OA token，前 6 位：` | 正常（已脱敏） | 无需处理 |

---

## 七、预期实际效果

> 以下为方案落地后的**目标状态**；本文档编写阶段**未改动任何代码**，效果待实施验证。

| 维度 | 改造前 | 改造后（预期） |
|---|---|---|
| **密钥位置** | 明文写在 `OaRequestAPIUtils.java:53`，进了 Git 历史 | 源码 / yml **零密钥**，仅由环境变量注入 |
| **密钥轮换** | 要改代码、重新编译发布 | **改环境变量重启即可**，无需改代码 |
| **多环境** | 三环境共用一份密钥 | dev / test / prd 各自独立配置 |
| **JSON 构造** | 手拼字符串，含 `"`/`\` 即非法 | `ObjectMapper` 序列化，自动转义 |
| **token 获取** | 每次调用都取一次 | 内存缓存，提前 5 分钟过期，**鉴权请求量下降** |
| **失败语义** | 吞异常 → 返回 `""` / `null`，四类原因不可区分 | 抛 `OaApiException`，携带 HTTP 码 / 业务码 / message；**空数组 ≠ 失败** |
| **与 T-01 的关系** | T-01 只能用「`null` 即失败」兜底，仍无法区分原因 | T-01 可去掉猜测，交由 `OaApiException` 精确定位；**T-01 的校验与替换逻辑零改动** |
| **日志** | `System.out` 打印完整 token 与全部报文 | `@Slf4j` + 脱敏，正文不落日志 |
| **连接** | 异常路径泄漏 | `finally` + try-with-resources，必定释放 |
| **配置错误暴露时机** | 运行期（凌晨 3 点定时同步时） | **启动期**（快速失败） |
| **可测试性** | 只能打真实 OA | 可注入 mock 单测 |

**一句话总结**：把「密钥能不能换」从**改代码的事**降级成**改配置的事**，同时把 OA 调用的失败原因从「四合一黑洞」变成**可区分、可告警的异常**。

---

## 八、与其他问题的边界

| 编号 | 问题 | 与 T-02 的关系 |
|---|---|---|
| **T-01** | 定时任务启动即清空课表 | **已完成（未提交）**。T-01 保留了 `OaRequestAPIUtils` 的静态调用，并在第七条明确「改造为 `OaApiClient` 时只需替换首行，校验与替换逻辑不受影响」。**本次 T-02 正是兑现这句预留** |
| **R-04** | `OaRequestAPIUtils` 用 `System.out` 打印全部报文与 token | **本次一并解决**（防线 7）。T-01 当时明确「建议随 T-02 一并改为 `@Slf4j` 并脱敏」 |
| — | `OaRequestAPIUtils` 吞异常返回 `null` | **本次一并解决**（防线 5）。空数组与异常彻底分离 |
| — | `per_page=1000` 写死导致分页截断 | **本次把 `page-size` 上移为配置**，但**分页拉取本身仍未实现**（属 T-01 遗留边界 3，需单独排期） |
| **R-05** | `TruncateTable` 使用 DDL 无法回滚 | **已由 T-01 解决**，与 T-02 无关 |

### 遗留的已知边界（需评估）

1. **`base-url` 仍写在 yml 里（非密钥）**。URL 本身不敏感，且可被 git 追踪以便审计，**故不强制外置**；如需完全环境隔离，可同样走环境变量。
2. **密钥轮换仍需重启应用**（内存缓存的代价）。若要求「不重启热更新密钥」，需引入配置中心推送 + 缓存失效机制，属**增强项**，非本次范围。
3. **token 缓存为单实例内存**。多实例部署时每个实例各缓存一份（各自取一次 token），这是**可接受**的；如需全局共享可改存 Redis，但会引入网络依赖，**不建议**。
4. **`queryHotelDataByToday` 的匹配逻辑**（`className.contains(BJMC)`）为模糊匹配，存在误匹配可能——**与本问题无关**，仅记录，建议单独评估。
5. **改造涉及 `ScheduleController`**，而该 Controller 里还有一个 `/debug/userInfo` 调试接口（记忆中的已知问题），**本次不顺带删除**，避免改动面失控。

---

## 九、待确认项

1. **旧密钥的吊销时间表**是谁负责推进？（**这是本方案能否闭环的前提**——只删代码不吊销，风险仍在）
2. OA 平台重新签发密钥是否需要走申请流程、预计多久？（影响上线排期）
3. 生产环境的**配置中心**是哪一个？（决定用环境变量还是配置中心注入）
4. OA 侧的 `access_token` **实际有效期**是多少？当前计划按 55 分钟缓存，需与真实值核对后调整 `token-cache-seconds`
5. 是否需要 `oa.enabled` 降级开关？（默认**不加**，避免又一处静默降级；若运维有需求再评估）
6. `application-local.yml` 的本地覆盖方案是否被团队接受？（还是统一只用环境变量）

---

## 附录：改动清单速览（计划）

```
新增（4）
  soft_common/src/main/java/com/hnkjzyxy/ab/config/OaProperties.java
  soft_common/src/main/java/com/hnkjzyxy/ab/client/OaApiClient.java
  soft_common/src/main/java/com/hnkjzyxy/ab/exception/OaApiException.java
  soft_main/src/main/java/com/hnkjzyxy/ab/init/validation/OaPropertiesValidator.java

修改（4）
  soft_common/src/main/java/com/hnkjzyxy/ab/utils/OaRequestAPIUtils.java   ← 删除
  soft_service/src/main/java/com/hnkjzyxy/ab/service/impl/CourseScheduleServiceImpl.java
  soft_main/src/main/java/com/hnkjzyxy/ab/controller/ScheduleController.java
  soft_main/src/main/resources/application-dev.yml
  soft_main/src/main/resources/application-test.yml
  soft_main/src/main/resources/application-prd.yml
  .gitignore                                                              ← 补充本地覆盖文件规则

数据库：无
外部依赖：OA 侧吊销并重新签发密钥（硬依赖，必须先完成）
```

> **与 T-01 的衔接**：`CourseScheduleServiceImpl#doSync` 第 196 行的 `getClassBoardData("", "")` 改为 `oaApiClient.getClassBoardData("", "")`，
> **T-01 的四道数据量闸门、事务替换、分布式锁逻辑一行不改**。两者的职责边界因此非常清晰：
> **T-01 保证「拿到数据后不丢」，T-02 保证「密钥安全 + 拿不到时说得清原因」。**
