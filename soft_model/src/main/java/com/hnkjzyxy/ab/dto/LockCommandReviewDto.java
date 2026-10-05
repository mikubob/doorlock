package com.hnkjzyxy.ab.dto;

import lombok.Data;

/**
 * 管理员依据现场核查独立复核未知回执，不发出新的物理指令。
 */
@Data
public class LockCommandReviewDto {
    /**
     * 是否已确认原动作执行；未填写不能默认成功或失败。
     */
    private Boolean executed;
    /**
     * 核查来源及确认依据。
     */
    private String reason;
}
