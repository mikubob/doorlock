package com.hnkjzyxy.ab.dto;

import com.hnkjzyxy.ab.params.PageQueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 子任务得分查询参数（按ID）
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/3/18 20:25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTaskIdDto extends PageQueryParam implements Serializable {

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 子任务ID
     */
    private String taskId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 子任务名称
     */
    private String taskName;

    /**
     * 用户子任务得分
     */
    private Integer score;

    /**
     * 前端限制的满分
     */
    private Integer limitScore;

    /**
     * 任务所属分类
     */
    private String taskCategory;

    /**
     * 附件记录（以数组形式存储的字符串，仅代表该用户某一项子任务的附件）
     */
    private String evidence;

}
