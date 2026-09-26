package com.hnkjzyxy.ab.vo;

import com.hnkjzyxy.ab.model.Result;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 项目结果提交参数
 */
public class ResultVo implements Serializable {

    /**
     * 用户唯一编号
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
     * 是否打回（0=否，1=是）
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

    public ResultVo() {
    }

    public ResultVo(Integer uId, Integer projectId, String evidence, Integer step, String score, String opinion, Integer isFlag, String userName, String nickName, List<Result> results) {
        this.uId = uId;
        this.projectId = projectId;
        this.evidence = evidence;
        this.step = step;
        this.score = score;
        this.opinion = opinion;
        this.isFlag = isFlag;
        this.userName = userName;
        this.nickName = nickName;
        this.results = results;
    }

    public Integer getUId() {
        return uId;
    }

    public void setUId(Integer uId) {
        this.uId = uId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public Integer getIsFlag() {
        return isFlag;
    }

    public void setIsFlag(Integer isFlag) {
        this.isFlag = isFlag;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

}
