package com.hnkjzyxy.ab.exception;

import lombok.Getter;

/**
 * 整次导入被拒绝异常
 * <p>
 * 与 {@link RowParseException} 的区别：行级解析失败只丢该行、其余照常入库；
 * 而本异常表示**整次导入集体放弃**（数据量超限、配置为「有错即放弃」等），
 * 必须向上传播到接口层，让调用方拿到明确的失败结果，而不是「成功 0 条」的假成功。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-01
 */
@Getter
public class ImportRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 拒绝原因
     */
    private final String reason;

    /**
     * 构造整次导入拒绝异常
     *
     * @param reason 拒绝原因
     */
    public ImportRejectedException(String reason) {
        super(reason);
        this.reason = reason;
    }

    /**
     * 构造整次导入拒绝异常
     *
     * @param reason 拒绝原因
     * @param cause  原始异常
     */
    public ImportRejectedException(String reason, Throwable cause) {
        super(reason, cause);
        this.reason = reason;
    }
}
