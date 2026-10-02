package com.hnkjzyxy.ab.vo;

import com.hnkjzyxy.ab.model.Result;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 项目结果及暂存数据的返回对象
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultVo implements Serializable {

    /**
     * 用户唯一编号
     */
    private Integer uId;

    /**
     * 项目ID
     */
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
     * 暂存或提交结果的完成标记（0=未完成，1=完成）
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
