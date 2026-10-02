package com.hnkjzyxy.ab.exception;

import lombok.Getter;

/**
 * 行级解析异常
 * <p>
 * 携带 Excel 行号（1-based，含表头）与失败原因，供导入回执直接展示给上传方，
 * 避免「一行坏数据导致整批失败且看不到原因」。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-01
 */
@Getter
public class RowParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Excel 行号（1-based，含表头行）
     */
    private final int rowIndex;

    /**
     * 失败字段名，整次导入级别的失败可为空
     */
    private final String fieldName;

    /**
     * 失败原因
     */
    private final String reason;

    /**
     * 构造行级解析异常
     *
     * @param rowIndex  Excel 行号（1-based，含表头行）
     * @param fieldName 失败字段名，可为 null
     * @param reason    失败原因
     */
    public RowParseException(int rowIndex, String fieldName, String reason) {
        super(buildMessage(rowIndex, fieldName, reason));
        this.rowIndex = rowIndex;
        this.fieldName = fieldName;
        this.reason = reason;
    }

    /**
     * 构造行级解析异常（整次导入级别，无具体字段与行号）
     *
     * @param rowIndex Excel 行号，整次导入级别传 0
     * @param reason   失败原因
     */
    public RowParseException(int rowIndex, String reason) {
        this(rowIndex, null, reason);
    }

    /**
     * 拼接可读的异常消息
     *
     * @param rowIndex  Excel 行号
     * @param fieldName 字段名
     * @param reason    原因
     * @return 形如「第 12 行：应到人数格式不正确」的消息
     */
    private static String buildMessage(int rowIndex, String fieldName, String reason) {
        StringBuilder sb = new StringBuilder();
        if (rowIndex > 0) {
            sb.append("第 ").append(rowIndex).append(" 行：");
        }
        sb.append(reason);
        return sb.toString();
    }
}
