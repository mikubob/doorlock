package com.hnkjzyxy.ab.model;

import lombok.Data;

/**
 * 项目子项
 * 用于维护项目下的分类与子项（评分项）
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/4/21 17:16
 */
@Data
public class ProjectItem {

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 所属项目ID
     */
    private Integer projectId;

    /**
     * 子项名称（分类名 / 子任务名）
     */
    private String pname;

    /**
     * 评分标准
     */
    private String standard;

    /**
     * 层级
     */
    private Integer grade;

    /**
     * 分值
     */
    private Integer score;

    /**
     * 父级ID（0 表示分类）
     */
    private Integer parentId;

    /**
     * 是否需要上传文件（0=否，1=是）
     */
    private Integer isFile;

    /**
     * 是否需要佐证材料（0=否，1=是）
     */
    private Integer isExtend;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序号
     */
    private Integer orderBy;

}
