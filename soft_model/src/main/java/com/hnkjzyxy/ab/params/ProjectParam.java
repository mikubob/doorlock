package com.hnkjzyxy.ab.params;

import com.hnkjzyxy.ab.params.PageQueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目查询参数
 *
 * @author 16702
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectParam extends PageQueryParam {

    /**
     * 项目标题
     */
    private String title;

    /**
     * 项目状态
     */
    private Integer status;

    /**
     * 年份
     */
    private String year;

    /**
     * 项目ID
     */
    private Integer projectId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 创建人工号
     */
    private String createName;

}
