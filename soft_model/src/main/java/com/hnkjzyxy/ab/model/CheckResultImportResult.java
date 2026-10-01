package com.hnkjzyxy.ab.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 巡查结果导入回执
 * 描述一次 Excel 导入的解析与落库结果，含逐行错误清单
 *
 * @version 1.0
 * @date 2026-10-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckResultImportResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否整体成功（写入过程中无未捕获异常）
     */
    private boolean success;

    /**
     * 解析到的数据总行数（不含表头）
     */
    private int totalRows;

    /**
     * 成功解析并写入数据库的行数
     */
    private int savedRows;

    /**
     * 解析失败被跳过的行数
     */
    private int failedRows;

    /**
     * 错误清单，每项含行号与原因
     */
    private List<RowError> errors = new ArrayList<>();

    /**
     * 实际解析到的表头行数，用于核对模板是否符合预期
     */
    private int headRowCount;

    /**
     * 结果说明，供前端直接展示
     */
    private String message;

    /**
     * 本次导入耗时（毫秒）
     */
    private long costMillis;

    /**
     * 新增一条错误明细
     *
     * @param rowIndex Excel 行号（1-based，含表头行）
     * @param reason   失败原因
     */
    public void addError(int rowIndex, String reason) {
        this.errors.add(new RowError(rowIndex, reason));
    }

    /**
     * 逐行错误明细
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Excel 行号（1-based，含表头行）
         */
        private int rowIndex;

        /**
         * 失败原因
         */
        private String message;
    }
}
