# 项目开发文档

与 TODO 有关的分析、专项方案和验收记录统一存放在 `doc/todo/`，按原任务编号命名。

- [项目 TODO 清单分析与解决方案](todo/项目TODO清单分析与解决方案.md)
- [T-01 课表同步清空风险](todo/T-01-课表同步清空风险-完整解决方案.md)
- [T-02 OA 接口密钥明文硬编码](todo/T-02-OA接口密钥明文硬编码-完整解决方案.md)
- [T-03 Excel 巡查导入映射与解析](todo/T-03-Excel巡查导入映射失效与解析中断-完整解决方案.md)
- [T-04 项目子项导入任务与原有数据清理](todo/T-04-项目子项导入任务与原有数据清理-完整解决方案.md)
- [T-01–T-03 修复与验收记录](todo/T-01-T-03修复与验收记录.md)

## 源码分层约定

沿用现有 Maven 模块和包结构，新增文件按职责归属存放：

| 职责 | 存放位置 |
|---|---|
| 数据库实体、数据库查询原始模型 | `soft_model/.../model/` |
| Excel 导入行数据 | `soft_model/.../dto/excel/` |
| 接口响应、导入回执、同步执行结果 | `soft_model/.../vo/` |
| 通用异常 | `soft_common/.../exception/` |
| 外部系统客户端、配置属性 | `soft_common/.../client/`、`soft_common/.../config/` |
| 服务接口 | `soft_service/.../service/` |
| 服务实现 | `soft_service/.../service/impl/` |
| Excel 解析监听器 | `soft_service/.../service/listener/` |
| 依赖领域模型的业务工具 | `soft_service/.../service/utils/` |
| HTTP 控制器、启动执行器、调度入口 | `soft_main/.../controller/`、`soft_main/.../config/` |

`ProjectTaskGuard` 为服务接口，事务实现位于 `ProjectTaskGuardImpl`，保留原 Bean 名称 `projectTaskGuard`。
`ProjectItemSource` 和 `ProjectImportState` 为数据库查询模型，继续存放在 `model` 包。
迁移脚本继续沿用现有 `soft_main/src/sql/migrations/` 目录和人工执行方式。

## 注解与注释约定

实体及数据对象用 Lombok 生成常规访问方法和签名一致的构造方法。纯访问方法采用
`@Getter`、`@Setter`；已有 `@Data` 的类沿用原注解。包含特殊相等规则、重载转换方法
或兼容构造签名的类保留对应方法，避免因格式整理改变行为。

数据库映射、校验、Excel 列映射及 Spring 配置绑定注解按原语义保留。数据对象不注册为
Spring Bean；服务实现使用 `@Service`，配置属性沿用 `@ConfigurationProperties`。

类和字段用 Javadoc 说明职责、字段含义及取值范围；保留的公开方法、构造方法说明
参数、返回值和异常条件，沿用现有 `@param`、`@return`、`@throws` 写法。

本次只整理目录、Lombok 和注释，不新增业务能力，不推进 T-05 及后续 TODO。

## 本次验证

使用项目声明的 Java 8 环境执行：

```text
mvn -o clean test -DskipTests
mvn -o -pl soft_service -am test -Dtest=ModelConventionCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false
```

六个 Maven 模块全量编译通过；5 项兼容性测试通过，覆盖构造签名与 JSON 属性、
主键相等与状态转换、Excel 写入读取、异常明细和服务接口代理事务边界。
这些验证不依赖外部数据库、Redis 或 OA，不替代各专项的集成验收。

本机默认 Java 21 与项目现有 Lombok 版本存在编译兼容问题，本次使用已有的 Java 8，
未调整依赖版本或全局环境配置。
