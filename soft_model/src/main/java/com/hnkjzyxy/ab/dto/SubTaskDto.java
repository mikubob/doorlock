package com.hnkjzyxy.ab.dto;

import com.hnkjzyxy.ab.params.PageQueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 子任务得分查询参数（按名称）
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/3/18 20:25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTaskDto extends PageQueryParam implements Serializable {

    /**
     * 项目名称
     */
    private String title;

    /**
     * 子任务名称
     */
    private String taskName;

    /**
     * 任务所属分类
     */
    private String taskCategory;

    /**
     * 用户子任务得分
     */
    private Integer score;

}
