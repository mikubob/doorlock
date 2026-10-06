package com.hnkjzyxy.ab.vo;

import lombok.Data;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 考试操作记录分页条件。
 */
@Data
public class ExamRecordQuery {
    /**
     * 页码，从一开始。
     */
    private int page = 1;
    /**
     * 每页记录数。
     */
    private int size = 20;
    /**
     * 操作类型，为空时查询全部考试操作。
     */
    private String action;
    /**
     * 操作人，为空时不限。
     */
    private String actor;
    /**
     * 起始学校时间，包含边界。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime from;
    /**
     * 截止学校时间，包含边界。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime to;
}
