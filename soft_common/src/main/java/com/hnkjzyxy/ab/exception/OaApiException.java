package com.hnkjzyxy.ab.exception;

/**
 * OA 开放平台调用异常
 * <p>
 * 取代原工具类「吞掉异常、返回 null / 空串」的实现：调用失败一律抛出本异常，
 * 并携带失败原因，调用方因此可以区分
 * 「网络故障 / 鉴权失败 / HTTP 非 200 / 业务码错误」四类情形。
 * </p>
 * <p>
 * 语义边界：<b>业务上没有数据（空数组）不是异常</b>，只有技术故障才抛本异常。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-01
 */
public class OaApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * HTTP 响应码，非 HTTP 阶段失败时为 null
     */
    private final Integer httpCode;

    /**
     * OA 业务码（响应体 code 字段），未取到响应体时为 null
     */
    private final Integer bizCode;

    /**
     * 构造异常（无 HTTP 码与业务码）
     *
     * @param message 异常描述
     */
    public OaApiException(String message) {
        this(message, null, null, null);
    }

    /**
     * 构造异常（携带根因）
     *
     * @param message 异常描述
     * @param cause   根因
     */
    public OaApiException(String message, Throwable cause) {
        this(message, null, null, cause);
    }

    /**
     * 构造异常（携带 HTTP 码）
     *
     * @param message  异常描述
     * @param httpCode HTTP 响应码
     */
    public OaApiException(String message, Integer httpCode) {
        this(message, httpCode, null, null);
    }

    /**
     * 构造异常（完整）
     *
     * @param message  异常描述
     * @param httpCode HTTP 响应码
     * @param bizCode  OA 业务码
     * @param cause    根因
     */
    public OaApiException(String message, Integer httpCode, Integer bizCode, Throwable cause) {
        super(message, cause);
        this.httpCode = httpCode;
        this.bizCode = bizCode;
    }

    /**
     * 获取 HTTP 响应码
     *
     * @return HTTP 响应码，可能为 null
     */
    public Integer getHttpCode() {
        return httpCode;
    }

    /**
     * 获取 OA 业务码
     *
     * @return OA 业务码，可能为 null
     */
    public Integer getBizCode() {
        return bizCode;
    }
}
