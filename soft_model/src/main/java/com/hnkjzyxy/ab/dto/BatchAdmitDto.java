package com.hnkjzyxy.ab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量录取参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchAdmitDto {

    /**
     * 学生信息ID列表
     */
    private List<Long> studentIds;

    /**
     * 录取方向
     */
    private String admittedMajor;
}
