package com.hnkjzyxy.ab.params;

import com.hnkjzyxy.ab.model.Result;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * 项目结果提交及暂存参数
 * <p>
 * 与 ResultVo 保留相同的字段格式以兼容旧请求及暂存 JSON；输入校验仅定义在本类型。
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResultSubmitParam implements Serializable {

    /**
     * 兼容旧请求的用户编号，实际操作者由认证身份确定
     */
    private Integer uId;

    /**
     * 项目ID
     */
    @NotNull(message = "项目id不能为空！")
    private Integer projectId;

    /**
     * 任务结果（佐证材料地址）
     */
    private String evidence;

    /**
     * 审批步骤
     */
    private Integer step;

    /**
     * 审批分数
     */
    private String score;

    /**
     * 审批意见
     */
    private String opinion;

    /**
     * 结果完成标记，提交时写入结果明细的 isFinish 字段（0=未完成，1=完成）
     */
    private Integer isFlag;

    /**
     * 用户名（工号）
     */
    private String userName;

    /**
     * 用户昵称（姓名）
     */
    private String nickName;

    /**
     * 结果明细列表
     */
    private List<Result> results;
}
