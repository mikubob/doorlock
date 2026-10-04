# T-07 / T-08 / T-09 PDF 转 Word 工具实施与验证记录

实施日期：2026-10-03。依据 [完整解决方案](T-07-T-08-T-09-PDF转Word工具-完整解决方案.md) 实施，范围限定为佐证材料 Word 导出。

## 实施结果

- **T-07 路径与输入集合**：使用 Jackson 树模型校验字符串数组，通过 `Path.resolve` 映射上传目录。拒绝错误前缀、空路径段、`.` / `..`、冒号、NUL、完整 URL、非普通 PDF、符号链接及 Windows 重解析跳转；真实路径必须仍位于上传根目录内。兼容历史正反斜杠，不执行 URL 解码。
- **生成服务联动**：仅导出 evidence 指定的材料，一次汇总并生成。按原始列表顺序，通过 `Files.isSameFile` 去重，覆盖硬链接场景，保留首次出现位置；原始数组项数先于文件 I/O 检查。
- **T-08 全页转换**：遍历每份 PDF 的所有页面，包括空白页。每页一个独立图片段落，从第二页开始设置段前分页，不追加尾部分页符。任何阶段失败均不开始下载残缺材料。
- **T-09 等比排版**：默认 A4 纵向，36pt 四边距，18pt 行框预留；图片依据渲染后宽高及实际写入的 twip 页面尺寸等比缩小，不放大或裁切。横向材料保留比例并允许留白。
- **资源保护**：PNG 逐页内存编码，不创建、复用或删除上传目录的 PNG。PDFBox 使用有界临时解析存储；图片编码缓存、底层图片流和 DOCX 写出流均执行预算检查。文档、流与页面位图在成功及异常路径释放。
- **容量与错误协议**：配置文件项数、输入大小、页数、像素、累计图片、输出、时间及并发限制；每实例默认一个许可，无等待队列，下载不持有许可。专用异常返回同值 HTTP 状态和 `ApiResult.code`，并发繁忙附带 `Retry-After: 5`。
- **临时文件**：生成失败由服务删除，成功由既有 Exporter 下载后删除。使用请求独占文件并保留 `tempFile*.docx` 下载文件名；不扫描清理共享临时目录。

转换仍然是 PDF 页面图片嵌入 DOCX，不提供 OCR、可编辑文字或表格。没有修改数据库、依赖版本、上传与 ZIP 接口、导出控制器职责或其他 TODO。

## 改动位置

| 文件 | 内容 |
|---|---|
| `soft_common/.../config/PdfToWordProperties.java` | Lombok 参数类、Spring 绑定及 Bean Validation |
| `soft_common/.../exception/PdfConversionException.java` | 状态、阶段、材料名、页码与内部 cause |
| `soft_common/.../utils/PdfToWordConverter.java` | 目录兼容入口、显式列表转换、全页等比嵌图及资源预算 |
| `soft_service/.../support/EvidenceWordService.java` | 选中材料汇总、路径边界、许可、运行环境检查及失败回收 |
| `soft_common/.../handler/GlobalExceptionHandler.java` | 仅新增 PDF 转 Word 专用异常映射 |
| `soft_main/src/main/resources/application.yml` | 公共 `evidence.word.*` 默认参数，不修改原 profile |
| `soft_common/.../PdfToWordConverterTest.java` | 真实 PDF 页序、比例、分页 XML、格式与预算测试 |
| `soft_service/.../EvidenceWordServiceTest.java` | 材料范围、路径、硬链接、重解析链接、并发及回收测试 |
| `soft_main/.../EvidenceWordHttpTest.java` | 真实 MVC 下载、错误 JSON、繁忙提示与配置绑定 |
| `soft_main/.../ResponsibilityWebTest.java` | 补充生成失败时未开始下载的断言，保留既有回归 |

新增及改动代码使用 Javadoc 注释；参数类使用 Lombok。没有新增数据库实体或持久化操作。

## 实现细节与兼容性

1. 保留 `pdfFilesToWordFile(String, String)` 目录工具入口，仅枚举本层普通可读 PDF，按不区分大小写文件名字典序与原名次级排序；HTTP 导出不使用该入口。
2. 项目 POI 4.0.1 精简 schemas 包缺少 `CTPageSz` / `CTPageMar` Java 类型，页面属性通过现有 XMLBeans 游标写入，无需新增或升级依赖。测试检查了 `pgSz`、`pgMar` 和图片 EMU 尺寸。
3. ImageIO 使用有界内存 `ImageOutputStream`，在内部编码缓存增长前检查位置和剩余预算，随后再由有界底层输出流复核；不依赖全局 ImageIO 缓存设置。
4. Java 8 Parallel GC 下，`-Xmx1024m` 的 `Runtime.maxMemory()` 可能报告不足 1GiB。HotSpot 使用诊断接口的 `MaxHeapSize` 判断方案规定的最大堆门槛；其他 JVM 或接口不可用时保守使用运行时报告值。默认门槛在本地 `-Xmx1024m` 下有成功转换测试。
5. 文件大小与页数在预检及正式读取时复核；页数变化报错。时间预算覆盖路径检查、预检、渲染、编码和写出，阶段边界检查中断；不承诺能强制中断第三方渲染调用。
6. 确认的伪 PDF、零页与加密文档为 422；不能可靠分类的 PDFBox I/O 故障为 500，不使用底层异常字符串猜测格式错误。公开消息不包含服务器绝对路径。

## 本地验证

环境：Windows、JDK 1.8、现有 Maven 缓存；PDFBox / FontBox 2.0.24、POI / OOXML / schemas 4.0.1，未变更依赖。

执行命令（PowerShell）：

```powershell
$env:JAVA_HOME='D:/CodeEnvironment/JDK1.8'
mvn -o -pl soft_main -am test -DskipTests=false '-DargLine=-Xmx1024m -Djava.awt.headless=true' '-Dtest=*Test' -Dsurefire.failIfNoSpecifiedTests=false
```

`*Test` 覆盖仓库规范命名的自动化回归，显式避开旧 `TestMain` 本地集成调试入口；没有修改 POM 或禁用既有测试代码。

验证结果：Java 8 全模块编译通过；规范命名回归共 101 项，100 项通过、1 项按既有条件跳过，0 失败、0 错误。本次相关的转换测试 11 项、生成服务测试 13 项、HTTP 测试 10 项和职责拆分 Web 回归 5 项全部通过，涵盖多文件五页合并、空白页、横向/方形/小页面、旋转 CropBox、原 PDF/PNG 保护、原始项数、硬链接身份去重、Windows junction 拒绝、超限与并发隔离。

全量不筛选测试另行执行过，旧 `TestMain` 有三项环境错误：

- `readUsers`、`readExcel` 依赖固定 `E:\桌面\...` 模板文件，当前环境读取失败；
- `test1` 所用数据库缺少 `source_project_item_id` 列。

这些错误与本次转换链路无关，未修改模板、数据库或相应测试。完整日志位于 `soft_main/target/pdf-word-regression.log`，规范命名回归日志位于 `soft_main/target/pdf-word-scoped-regression.log`；`target` 为本地构建产物。

## 尚需部署环境验收

本地测试检查 DOCX 的页面绘图引用与分页属性，不能代替 Word / WPS 的实际分页、字体与打印预览验收。尚未执行 Linux 中文字体样本验证、Word 2016+ / WPS 2021+ 客户端检查，以及原方案规定的 5 / 10 / 50 页实际材料重复压测。

默认配置与原方案一致：150 DPI、最多 20 项 / 总 50 页、单实例 1 个转换、60 秒生成预算、累计图片与 DOCX 各 64MiB、PDF 解析临时存储 256MiB、最大堆门槛 1024MiB。部署时按原方案检查堆、临时目录、中文字体与网关等待预算；未将尚未完成的客户端或生产容量验收写为已通过。
