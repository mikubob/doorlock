package com.hnkjzyxy.ab.exception;

import lombok.Getter;

/**
 * 成绩入口的身份校验及权限拒绝异常
 * <p>
 * 401 表示操作人身份无效，403 表示有效身份不具有操作权限或学院范围不符。
 * 由全局异常处理器同时设置 HTTP 状态和业务码；继承运行时异常以保持评分事务的回滚语义。
 * 数据库、参数和其他技术异常不应转换为本异常。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-03
 */
@Getter
public class AuthPermissionException extends RuntimeException {
    /**
     * HTTP 错误状态码，仅允许401或403
     *
     * -- GETTER --
     * 获取 HTTP 错误状态码
     *
     * @return 与响应体业务码一致的401或403
     */
    private final int status;

    /**
     * 构造身份或权限拒绝异常
     *
     * @param status HTTP 状态码，身份无效使用401，权限不足使用403
     * @param message 提供给调用方的拒绝说明，不应包含令牌或完整用户信息
     * @throws IllegalArgumentException 状态码不是401或403时抛出
     */
    public AuthPermissionException(int status, String message) {
        super(message);
        // 限定异常用途，避免参数错误或数据库故障被误报为权限拒绝。
        if (status != 401 && status != 403) {
            throw new IllegalArgumentException("身份权限异常仅允许401或403");
        }
        this.status = status;
    }

}
