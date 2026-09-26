package com.hnkjzyxy.ab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务查询参数
 * 按项目、分类查询任务信息
 *
 * @version 1.0
 * @author Spell a
 * @date 2024-01-17 11:46
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDto {

    /**
     * 项目ID
     */
    private Integer projectId;

    /**
     * 分类名称
     */
    private String category;

    /**
     * 分数
     */
    private Integer score;

}
