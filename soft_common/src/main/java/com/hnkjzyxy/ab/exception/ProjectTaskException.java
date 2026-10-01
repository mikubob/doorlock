package com.hnkjzyxy.ab.exception;

/**
 * 项目任务导入及生命周期校验异常
 * <p>
 * 携带实际 HTTP 状态码，供 T-04 专用异常处理器返回统一错误响应。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
public class ProjectTaskException extends RuntimeException {
    /**
     * HTTP 错误状态码
     */
    private final int status;

    /**
     * 构造项目任务业务异常
     *
     * @param status HTTP 错误状态码
     * @param message 业务错误说明
     */
    public ProjectTaskException(int status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * 获取 HTTP 错误状态码
     *
     * @return HTTP 错误状态码
     */
    public int getStatus() {
        return status;
    }
}
