package com.hnkjzyxy.ab.vo;

import lombok.Data;

/**
 * 项目子项信息
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/4/21 16:56
 */
@Data
public class ProjectItemVo {

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 所属项目ID
     */
    private Integer projectId;

    /**
     * 父级ID（即所属分类ID）
     */
    private Integer parentId;

    /**
     * 分类名称
     */
    private String category;

    /**
     * 子任务名称
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

}
