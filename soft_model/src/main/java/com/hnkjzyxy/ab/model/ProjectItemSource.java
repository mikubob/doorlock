package com.hnkjzyxy.ab.model;

import lombok.Data;

/**
 * 项目子项原始来源数据
 * <p>
 * 保留数据库 varchar 字段的原始内容，避免隐式类型转换掩盖非法项目 ID 或分值。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
@Data
public class ProjectItemSource {
    /**
     * 来源子项主键ID
     */
    private Integer id;

    /**
     * 所属项目ID原始字符串，导入前严格解析
     */
    private String projectId;

    /**
     * 分类名称或子任务名称
     */
    private String pname;

    /**
     * 评分标准原始内容
     */
    private String standard;

    /**
     * 层级（1=分类，2=子项）
     */
    private Integer grade;

    /**
     * 最高分值原始字符串，导入前严格解析
     */
    private String score;

    /**
     * 父分类ID（顶层分类为0）
     */
    private Integer parentId;

    /**
     * 是否需要佐证文件（0=否，1=是）
     */
    private Integer isFile;

    /**
     * 是否需要扩展项（0=否，1=是）
     */
    private Integer isExtend;

    /**
     * 备注，可为空
     */
    private String remark;
}
