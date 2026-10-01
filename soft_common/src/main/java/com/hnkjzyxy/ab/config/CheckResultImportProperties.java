package com.hnkjzyxy.ab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 巡查结果导入参数配置
 * <p>
 * 对应 application-*.yml 中的 {@code check-result.import.*} 节点。
 * 由 SoftApplication 上的 {@code @ConfigurationPropertiesScan} 自动注册，无需再加 {@code @Component}。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-01
 */
@Data
@ConfigurationProperties(prefix = "check-result.import")
public class CheckResultImportProperties {

    /**
     * 导入总开关，false 时上传接口直接拒绝，不解析也不写库
     */
    private boolean enabled = true;

    /**
     * 单次导入的最大数据行数，超过则拒绝导入（防止单事务全量内存被超大文件打爆）
     */
    private int maxRows = 50000;

    /**
     * 单事务内的分批写入条数，只影响 SQL 往返次数，不影响事务边界
     */
    private int batchSize = 500;

    /**
     * 存在解析失败行时是否放弃整次导入。
     * false=跳过坏行、其余入库（默认，体验优先）；true=只要有一行坏数据就整体放弃
     */
    private boolean failFastOnError = false;

    /**
     * 学院为空时是否拒绝整次导入。true 时不再静默写入 null，避免数据在统计报表中「隐形」
     */
    private boolean requireCollege = true;

    /**
     * 学院取值策略：FROM_UPLOADER=取上传人所属学院（默认）；FROM_EXCEL=取 Excel 内学院列；MANUAL=先入库后补录
     */
    private CollegeStrategy collegeStrategy = CollegeStrategy.FROM_UPLOADER;

    /**
     * 表头列序校验是否严格拦截。
     * false（默认）=只对「日期」锚点列做校验，错位时告警并写入回执提示，不阻断导入；
     * true=锚点列错位直接拒绝整次导入。默认宽松，因为巡查模板由业务方线下维护，列名/列序可能被调整
     */
    private boolean strictHeadCheck = false;

    /**
     * 学院取值策略枚举
     */
    public enum CollegeStrategy {
        /**
         * 取上传人所属学院（默认）
         */
        FROM_UPLOADER,
        /**
         * 取 Excel 内的学院列（需要 Excel 模板包含学院列）
         */
        FROM_EXCEL,
        /**
         * 允许学院为空，导入后由前端补录
         */
        MANUAL
    }
}
