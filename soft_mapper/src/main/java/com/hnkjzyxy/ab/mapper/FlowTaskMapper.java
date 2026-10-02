package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.vo.ApproveVo;
import com.hnkjzyxy.ab.vo.FlowTaskVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程节点数据访问接口
 */
public interface FlowTaskMapper extends BaseMapper<FlowTask> {
    /**
     * 按节点类型查询流程节点展示信息
     *
     * @param type 查询或节点类型
     * @return 包含项目ID、节点用户及角色的节点列表
     */
    List<FlowTaskVo> getFlowTaskList(@Param("type") String type);

    /**
     * 按节点类型查询审批节点信息
     *
     * @param type 查询或节点类型
     * @return 审批节点列表
     */
    List<ApproveVo> getApproveVoList(@Param("type") String type);

    /**
     * 按项目和节点类型查询审批步骤
     *
     * @param type 查询或节点类型
     * @param pId 考核项目ID
     * @return 审批节点列表
     */
    List<ApproveVo> getFlowStepList(@Param("type") String type, @Param("pId") Integer pId);

    /**
     * 查询父节点下的流程节点
     *
     * @param id 流程节点ID
     * @return 流程节点列表
     */
    List<FlowTask> selectByParentId(@Param("id") Integer id);

    /**
     * 查询项目指定节点类型的最大排序值
     *
     * @param type 查询或节点类型
     * @param pId 考核项目ID
     * @return 有效节点的最大排序值，无匹配节点时返回 null
     */
    Integer getFlowMaxSort(@Param("type") String type, @Param("pId") Integer pId);

}
