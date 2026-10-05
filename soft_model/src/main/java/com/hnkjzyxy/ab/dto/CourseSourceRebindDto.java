package com.hnkjzyxy.ab.dto;

import lombok.Data;

/**
 * 来源改变后的独立课程调整复核请求。
 */
@Data
public class CourseSourceRebindDto {
    /**
     * 保留独立调整的稳定课程键。
     */
    private String courseKey;
    /**
     * 人工确认的新 OA 来源键；为空表示明确转为本地课程。
     */
    private String sourceCourseKey;
    /**
     * 调整记录的最新行版本。
     */
    private Long rowVersion;
    /**
     * 新 OA 来源的最新行版本。
     */
    private Long sourceRowVersion;
    /**
     * 人工确认依据。
     */
    private String reason;
}
