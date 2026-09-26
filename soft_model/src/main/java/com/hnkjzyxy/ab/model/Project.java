package com.hnkjzyxy.ab.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 项目信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "sys_project")
public class Project implements Serializable {

    /**
     * 项目ID
     */
    @TableId
    private Integer id;

    /**
     * 项目标题
     */
    private String title;

    /**
     * 流程ID（非数据库字段）
     */
    @TableField(exist = false)
    private Integer flowId;

    /**
     * 项目描述
     */
    @TableField(value = "`describe`")
    @NotBlank(message = "项目描述不能为空！")
    private String describe;

    /**
     * 发送人
     */
    private String sendName;

    /**
     * 创建人工号
     */
    private String createName;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空！")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空！")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 创建（发布）时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 是否收集信息（0=否，1=是）
     */
    private Integer isCollect;

    /**
     * 项目状态（1=已发布，3=已删除）
     */
    private Integer status;

    /**
     * 项目导入文件（非数据库字段）
     */
    @TableField(exist = false)
    @JSONField(serialize = false)
    private MultipartFile file;

    /**
     * 任务列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Task> task;

    /**
     * 任务数量（非数据库字段）
     */
    @TableField(exist = false)
    private Integer taskNum;

    /**
     * 未提交人数（非数据库字段）
     */
    @TableField(exist = false)
    private Integer noSubmitCount;

    /**
     * 未审批人数（非数据库字段）
     */
    @TableField(exist = false)
    private Integer resultCount;

    /**
     * 已审批人数（非数据库字段）
     */
    @TableField(exist = false)
    private Integer approveCount;

    /**
     * 总人数（非数据库字段）
     */
    @TableField(exist = false)
    private Integer totalCount;

    /**
     * 结果信息（非数据库字段）
     */
    @TableField(exist = false)
    private List<Result> results;

}
