package com.hnkjzyxy.ab.exception;

import lombok.Getter;

/**
 * PDF 转 Word 专用异常，公开提示与底层故障分开保存。
 *
 * @version 1.0
 * @date 2026-10-03
 */
@Getter
public class PdfConversionException extends RuntimeException {
    /** HTTP 状态码，与 ApiResult.code 一致 */
    private final int status;
    /** 失败阶段，供日志定位 */
    private final String stage;
    /** 材料文件名，不包含部署路径 */
    private final String material;
    /** 面向用户的页码，从 1 开始；非页面故障为 null */
    private final Integer pageNumber;

    /**
     * 构造输入或预算拒绝异常。
     *
     * @param status HTTP 状态码
     * @param stage 失败阶段
     * @param message 安全的公开提示
     */
    public PdfConversionException(int status, String stage, String message) {
        this(status, stage, null, null, message, null);
    }

    /**
     * 构造携带内部故障上下文的异常。
     *
     * @param status HTTP 状态码
     * @param stage 失败阶段
     * @param material 材料文件名
     * @param pageNumber 从 1 开始的页码，可为空
     * @param message 安全的公开提示
     * @param cause 底层故障，仅用于内部日志
     */
    public PdfConversionException(int status, String stage, String material, Integer pageNumber,
                                  String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.stage = stage;
        this.material = material;
        this.pageNumber = pageNumber;
    }
}
