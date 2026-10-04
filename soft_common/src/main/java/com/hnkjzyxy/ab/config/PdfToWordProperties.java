package com.hnkjzyxy.ab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

/**
 * 佐证材料 Word 生成参数，由 ConfigurationPropertiesScan 注册。
 *
 * @version 1.0
 * @date 2026-10-03
 */
@Data
@Validated
@ConfigurationProperties(prefix = "evidence.word")
public class PdfToWordProperties {
    /** PDF 页面渲染分辨率，单位 DPI */
    @Min(72)
    @Max(200)
    private int renderDpi = 150;
    /** Word 纸张宽度，单位 pt */
    @Positive
    private double pageWidthPt = 595.3;
    /** Word 纸张高度，单位 pt */
    @Positive
    private double pageHeightPt = 841.9;
    /** 四边页边距，单位 pt */
    @Positive
    private double marginPt = 36;
    /** 图片段落行框预留高度，单位 pt */
    @Positive
    private double paragraphReservePt = 18;
    /** 原始材料数组的最大项数，重复材料仍计数 */
    @Positive
    private int maxFiles = 20;
    /** 单份 PDF 最大页数 */
    @Positive
    private int maxPagesPerPdf = 50;
    /** 本次生成的最大总页数 */
    @Positive
    private int maxTotalPages = 50;
    /** 单份 PDF 最大大小，单位 MiB */
    @Positive
    private long maxFileSizeMb = 50;
    /** 去重后 PDF 最大总大小，单位 MiB */
    @Positive
    private long maxTotalInputSizeMb = 100;
    /** 单页渲染的最大像素数 */
    @Positive
    private long maxPagePixels = 12000000;
    /** PNG 编码数据最大累计大小，单位 MiB */
    @Positive
    private long maxTotalImageSizeMb = 64;
    /** DOCX 最大输出大小，单位 MiB */
    @Positive
    private long maxOutputSizeMb = 64;
    /** 每份 PDF 的解析临时存储上限，单位 MiB */
    @Positive
    private long maxPdfScratchSizeMb = 256;
    /** 每实例同时生成许可数，不排队 */
    @Positive
    private int maxConcurrentExports = 1;
    /** 生成预算，单位秒，不包含 HTTP 下载 */
    @Positive
    private int maxGenerationSeconds = 60;
    /** Word 请求要求的 JVM 最大堆下限，单位 MiB */
    @Positive
    private long minJvmMaxHeapMb = 1024;

    /**
     * 校验版式、单位转换边界及单项和总量限制的关系。
     *
     * @return 参数可用于生成时为 true
     */
    @AssertTrue(message = "Word 生成版式或资源限制配置不合法")
    public boolean isConsistent() {
        return Double.isFinite(pageWidthPt) && Double.isFinite(pageHeightPt)
                && Double.isFinite(marginPt) && Double.isFinite(paragraphReservePt)
                && pageWidthPt > 2 * marginPt
                && pageHeightPt > 2 * marginPt + paragraphReservePt
                && Math.round(pageWidthPt * 20) > 2 * Math.round(marginPt * 20)
                && Math.round(pageHeightPt * 20) / 20.0
                > 2 * Math.round(marginPt * 20) / 20.0 + paragraphReservePt
                && pageWidthPt <= Integer.MAX_VALUE / 12700.0
                && pageHeightPt <= Integer.MAX_VALUE / 12700.0
                && maxPagesPerPdf <= maxTotalPages && maxFileSizeMb <= maxTotalInputSizeMb
                && validMegabytes(maxFileSizeMb) && validMegabytes(maxTotalInputSizeMb)
                && validMegabytes(maxTotalImageSizeMb) && validMegabytes(maxOutputSizeMb)
                && validMegabytes(maxPdfScratchSizeMb) && validMegabytes(minJvmMaxHeapMb);
    }

    /**
     * 校验 MiB 配置可安全转换为 long 字节数。
     *
     * @param value MiB 配置值
     * @return 正数且转换不溢出时为 true
     */
    private boolean validMegabytes(long value) {
        return value > 0 && value <= Long.MAX_VALUE / (1024L * 1024L);
    }

    /**
     * 校验非 Spring 调用方传入的参数，避免目录兼容入口绕过配置校验。
     *
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    public void validate() {
        if (!isConsistent() || renderDpi < 72 || renderDpi > 200
                || marginPt <= 0 || paragraphReservePt <= 0 || maxFiles <= 0
                || maxPagesPerPdf <= 0 || maxTotalPages <= 0 || maxPagePixels <= 0
                || maxConcurrentExports <= 0 || maxGenerationSeconds <= 0) {
            throw new IllegalArgumentException("Word 生成参数不合法");
        }
    }
}
