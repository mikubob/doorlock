package com.hnkjzyxy.ab.service.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.hnkjzyxy.ab.config.CheckResultImportProperties;
import com.hnkjzyxy.ab.model.CheckResult;
import com.hnkjzyxy.ab.model.CheckResultImportResult;
import com.hnkjzyxy.ab.service.CheckResultService;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 巡查结果 Excel 导入监听器
 * <p>
 * 职责：单行解析容错、显式字段映射、错误行隔离、整次导入单事务落库。
 * </p>
 * <p>
 * 铁律：不使用 {@code BeanUtils.copyProperties} 做实体搬运（类型不兼容时会静默丢字段），
 * 所有字段一律显式 set；任何解析失败都只丢该行并登记行号与原因，不牵连整批。
 * </p>
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-04 21:16
 */
@Slf4j
public class CheckResultDataListener extends AnalysisEventListener<CheckResultModel> {

    /**
     * 兼容的日期格式。
     * 注意：{@code M} / {@code d} 是宽松模式，可同时吃下「2024-5-5」与「2024-05-05」；
     * 若写成 {@code MM} / {@code dd} 则只能解析补零格式。
     */
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy年M月d日"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    /**
     * 锚点列索引：用于低成本探测模板列序是否错位（对应 {@code CheckResultModel#date} 的 @ExcelProperty）
     */
    private static final int ANCHOR_COLUMN_INDEX = 1;

    /**
     * 锚点列应有的表头名
     */
    private static final String ANCHOR_COLUMN_NAME = "日期";

    private final CheckResultService checkResultService;
    private final SnowFlowUtils snowFlowUtils;
    private final TransactionTemplate transactionTemplate;
    private final CheckResultImportProperties importProperties;

    /**
     * 本次导入的来源学院（策略为 FROM_UPLOADER 时由上传人上下文推导）
     */
    private final String sourceCollege;

    /**
     * 已成功映射、等待统一落库的实体
     */
    private final List<CheckResult> data = new ArrayList<>();

    /**
     * 错误明细，逐行登记行号与原因
     */
    private final List<CheckResultImportResult.RowError> errorRows = new ArrayList<>();

    /**
     * 成功解析行数
     */
    private int successCount = 0;

    /**
     * 解析失败行数
     */
    private int failCount = 0;

    /**
     * 已写入数据库行数
     */
    private int savedCount = 0;

    /**
     * 解析开始时间戳，用于统计耗时
     */
    private final long startMillis = System.currentTimeMillis();

    /**
     * 表头列序告警信息（非严格模式下记录，随回执返回）
     */
    private String headWarning;

    /**
     * 实际解析到的表头行数
     */
    private int headRowCount;

    /**
     * 构造导入监听器
     *
     * @param checkResultService  巡查结果服务
     * @param snowFlowUtils       雪花 ID 生成器
     * @param transactionTemplate 事务模板，用于整次导入单事务落库
     * @param importProperties    导入参数配置
     * @param sourceCollege       来源学院（策略为 FROM_UPLOADER 时传入上传人学院）
     */
    public CheckResultDataListener(CheckResultService checkResultService,
                                   SnowFlowUtils snowFlowUtils,
                                   TransactionTemplate transactionTemplate,
                                   CheckResultImportProperties importProperties,
                                   String sourceCollege) {
        this.checkResultService = checkResultService;
        this.snowFlowUtils = snowFlowUtils;
        this.transactionTemplate = transactionTemplate;
        this.importProperties = importProperties;
        this.sourceCollege = sourceCollege;
    }

    /**
     * 每解析一行调用一次：单行失败只丢该行，不中断整次导入
     *
     * @param row 当前行解析结果
     * @param ctx 解析上下文
     */
    @Override
    public void invoke(CheckResultModel row, AnalysisContext ctx) {
        // getRowIndex() 从 0 开始且已消费表头，+1 得到 Excel 中肉眼可见的行号（表头为第 1 行）
        int rowIndex = ctx.readRowHolder().getRowIndex() + 1;
        try {
            data.add(convert(row, rowIndex));
            successCount++;
        } catch (RowParseException e) {
            failCount++;
            errorRows.add(new CheckResultImportResult.RowError(rowIndex, e.getReason()));
            log.warn("[巡查导入] 第 {} 行解析失败，已跳过：{}", rowIndex, e.getReason());
        } catch (Exception e) {
            failCount++;
            errorRows.add(new CheckResultImportResult.RowError(rowIndex, "未知错误 - " + e.getMessage()));
            log.error("[巡查导入] 第 {} 行未预期异常", rowIndex, e);
        }
    }

    /**
     * 表头解析完成后执行：校验锚点列是否错位
     * <p>
     * {@code @ExcelProperty} 同时配置了 {@code value} 与 {@code index}，EasyExcel 2.x **以 index 为准**，
     * 因此上传的 Excel 一旦插入或删除列，全部字段会整体错位且不报任何错。
     * 这里只校验「日期」这一锚点列（index=1）作低成本探测，发现错位时告警并写入回执；
     * {@code strict-head-check=true} 时直接拒绝整次导入。
     * </p>
     *
     * @param headMap 表头映射（key=列索引，value=表头名）
     * @param ctx     解析上下文
     */
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext ctx) {
        if (headMap == null || headMap.isEmpty()) {
            return;
        }
        String anchor = headMap.get(ANCHOR_COLUMN_INDEX);
        if (ANCHOR_COLUMN_NAME.equals(trimToNull(anchor))) {
            return;
        }
        String actual = anchor == null ? "（空列）" : anchor;
        String message = "模板列序可能已变更：第 " + (ANCHOR_COLUMN_INDEX + 1)
                + " 列应为「" + ANCHOR_COLUMN_NAME + "」，实际为「" + actual + "」；"
                + "字段可能整体错位，请核对模板或联系管理员";
        if (importProperties.isStrictHeadCheck()) {
            throw new ImportRejectedException(message);
        }
        headWarning = message;
        log.warn("[巡查导入] {}", message);
    }

    /**
     * 全部解析完成后执行：整次导入闸门校验 + 单事务落库
     * <p>
     * 两道闸门均为**整次导入级别**，触发即放弃整次导入、不写入任何数据，
     * 且以 {@link ImportRejectedException} 向上传播到接口层，避免返回「成功 0 条」的假成功。
     * </p>
     *
     * @param ctx 解析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext ctx) {
        headRowCount = ctx.readSheetHolder().getHeadRowNumber();
        int totalRows = successCount + failCount;

        // 闸门 1：数据量超限。必须在此处判定——只有解析结束才知道总行数，且超限属于「整份文件不可接受」，
        // 不该与「某一行填错」混为一谈（放在 invoke 里会造成「记一条错误但超出部分照常入库」的假象）
        if (importProperties.getMaxRows() > 0 && totalRows > importProperties.getMaxRows()) {
            throw new ImportRejectedException("单次导入数据量 " + totalRows + " 行，超过上限 "
                    + importProperties.getMaxRows() + " 行，已放弃整次导入（未写入任何数据），请拆分文件后重试");
        }

        // 闸门 2：配置为「有错即放弃」时，不写入任何数据
        if (failCount > 0 && importProperties.isFailFastOnError()) {
            throw new ImportRejectedException("存在 " + failCount + " 行无法解析，已按配置放弃整次导入（未写入任何数据）");
        }

        persistAtomically();
        log.info("[巡查导入] 解析完成：总计 {} 行，成功 {}，失败 {}，表头 {} 行",
                totalRows, successCount, failCount, headRowCount);
    }

    /**
     * 在单个事务内分批写入，任一批失败即整体回滚，杜绝「半张表」脏数据
     */
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
        data.clear();
    }

    /**
     * 组装导入回执
     *
     * @return 导入结果明细
     */
    public CheckResultImportResult getResult() {
        CheckResultImportResult result = new CheckResultImportResult();
        result.setSuccess(true);
        result.setTotalRows(successCount + failCount);
        result.setSavedRows(savedCount);
        result.setFailedRows(failCount);
        result.setErrors(errorRows);
        result.setHeadRowCount(headRowCount);
        result.setCostMillis(System.currentTimeMillis() - startMillis);
        result.setMessage(buildMessage());
        return result;
    }

    /**
     * 拼装回执说明文案
     *
     * @return 面向上传方的结果说明
     */
    private String buildMessage() {
        StringBuilder sb = new StringBuilder("导入完成：");
        if (failCount > 0) {
            sb.append("成功 ").append(savedCount).append(" 条，失败 ").append(failCount)
                    .append(" 条，请按错误清单修正后重传");
        } else {
            sb.append("共写入 ").append(savedCount).append(" 条数据");
        }
        if (headWarning != null) {
            sb.append("；").append(headWarning);
        }
        return sb.toString();
    }

    /**
     * 单行 Model → 实体，显式映射；字段缺失或格式错误抛 {@link RowParseException}
     *
     * @param item     Excel 行模型
     * @param rowIndex Excel 行号（1-based，含表头行）
     * @return 巡查结果实体
     */
    private CheckResult convert(CheckResultModel item, int rowIndex) {
        CheckResult r = new CheckResult();
        r.setId(snowFlowUtils.nextId());

        // —— 字符串字段：显式赋值，不用反射 ——
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

        // —— 数值字段：容错解析（空 / “-” / 带单位 / 小数 均可）——
        r.setShouldArrival(parseIntSafe(item.getShouldArrival(), "应到人数", rowIndex));
        r.setArrival(parseIntSafe(item.getArrival(), "实到人数", rowIndex));
        r.setFoodBringPerson(parseIntSafeOrZero(item.getFoodBringPerson(), "带食物人数", rowIndex));

        // —— 日期：多格式兼容 + 判空 ——
        r.setDate(parseDate(item.getDate(), rowIndex));

        // —— 是否类字段：统一“是/1/Y”判定，空值按“否”，不再抛 NPE ——
        r.setIsLate(isYes(item.getIsLate()) ? 1 : 0);
        r.setIsNormal(isYes(item.getIsNormal()) ? 1 : 0);
        r.setIsViolate(isYes(item.getIsViolate()) ? 1 : 0);

        // —— 备注：保留原始文字描述（长度 > 2 说明不是单纯的“是/否”）——
        StringJoiner joiner = new StringJoiner(",");
        addIfDetail(joiner, item.getIsLate());
        addIfDetail(joiner, item.getIsNormal());
        addIfDetail(joiner, item.getIsViolate());
        r.setRemark(joiner.toString());

        // —— 无 Excel 来源的字段：显式赋值，不再依赖数据库默认值与 ORM 策略的巧合 ——
        r.setCollege(resolveCollege(rowIndex));
        r.setPeopleLeave(0);
        r.setIsStand(1);
        r.setIsConsist(1);
        return r;
    }

    /**
     * 解析本次导入的学院归属。
     * 策略为 FROM_UPLOADER 时取上传人所属学院；为空且要求学院必填时拒绝该行，不静默写 null
     *
     * @param rowIndex Excel 行号
     * @return 学院名称
     */
    private String resolveCollege(int rowIndex) {
        if (CheckResultImportProperties.CollegeStrategy.MANUAL == importProperties.getCollegeStrategy()) {
            return sourceCollege;
        }
        if (sourceCollege == null || sourceCollege.trim().isEmpty()) {
            if (importProperties.isRequireCollege()) {
                throw new RowParseException(rowIndex, "学院", "无法确定所属学院，请联系管理员为当前账号配置学院后再导入");
            }
            return null;
        }
        return sourceCollege.trim();
    }

    /**
     * 数值容错解析
     *
     * @param raw       原始单元格文本
     * @param fieldName 字段名，用于错误提示
     * @param rowIndex  Excel 行号
     * @return 解析结果
     */
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
            return (int) Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            throw new RowParseException(rowIndex, fieldName + "格式不正确：" + raw);
        }
    }

    /**
     * 允许缺省为 0 的数值解析（带食物人数）
     *
     * @param raw       原始单元格文本
     * @param fieldName 字段名
     * @param rowIndex  Excel 行号
     * @return 解析结果，空值返回 0
     */
    private static int parseIntSafeOrZero(String raw, String fieldName, int rowIndex) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        return parseIntSafe(raw, fieldName, rowIndex);
    }

    /**
     * 日期多格式解析
     *
     * @param raw      原始单元格文本
     * @param rowIndex Excel 行号
     * @return 日期
     */
    private static Date parseDate(String raw, int rowIndex) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new RowParseException(rowIndex, "日期", "日期为空");
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
        throw new RowParseException(rowIndex, "日期",
                "日期格式不正确：" + raw + "（支持 2024-05-05 / 2024/5/5 / 2024年5月5日 / 20240505）");
    }

    /**
     * 文本必填校验
     *
     * @param raw       原始单元格文本
     * @param fieldName 字段名
     * @param rowIndex  Excel 行号
     * @return 去除首尾空白后的文本
     */
    private static String requireText(String raw, String fieldName, int rowIndex) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new RowParseException(rowIndex, fieldName, fieldName + "为空");
        }
        return raw.trim();
    }

    /**
     * 是否类取值判定
     * <p>
     * 除标准取值「是 / 1 / Y」外，还要兼容**带补充描述的写法**——例如「是否迟到」列填
     * 「是，第 1 节迟到 10 分钟」。这类单元格本意是「是」，若只做全等比较会被判定为「否」，
     * 出现「remark 里写着迟到、is_late 却是 0」的数据自相矛盾（原实现同样有此问题）。
     * 因此这里按「以『是』/『1』/『Y』开头」判定；明确以「否 / 无 / 不」开头的按「否」处理。
     * </p>
     *
     * @param v 原始单元格文本
     * @return 是否判定为「是」
     */
    private static boolean isYes(String v) {
        if (v == null) {
            return false;
        }
        String t = v.trim();
        if (t.isEmpty()) {
            return false;
        }
        // 明确否定的写法优先排除，避免「不是」「无迟到」被前缀误判为「是」
        if (t.startsWith("否") || t.startsWith("无") || t.startsWith("不")) {
            return false;
        }
        return t.startsWith("是") || t.startsWith("1") || t.startsWith("Y") || t.startsWith("y");
    }

    /**
     * 备注只收录「有具体描述」的单元格（长度 > 2 说明不是单纯的「是/否」）
     *
     * @param joiner 备注拼接器
     * @param v      原始单元格文本
     */
    private static void addIfDetail(StringJoiner joiner, String v) {
        if (v != null && v.trim().length() > 2) {
            joiner.add(v.trim());
        }
    }

    /**
     * 去除首尾空白，空串归一为 null
     *
     * @param v 原始文本
     * @return 去空白后的文本，空串返回 null
     */
    private static String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
