package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.vo.ApproveVo;
import com.hnkjzyxy.ab.vo.FlowTaskVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//@CacheNamespace(implementation = MybatisRedisCache.class)
/**
 * 流程节点数据访问接口
 */
public interface FlowTaskMapper extends BaseMapper<FlowTask> {
    /**
     * 按节点类型查询流程节点展示信息
     *
     * @param type 查询或节点类型
     * @return 查询结果列表
     */
    @Select("select A.p_id pId, B.u_id uId,B.role_id roleId from sys_flow A " +
            "join sys_flow_task B on A.id = B.parent_id WHERE B.type = #{type} and B.status = 1")
    List<FlowTaskVo> getFlowTaskList(@Param("type") String type);

    /**
     * 按节点类型查询审批节点信息
     *
     * @param type 查询或节点类型
     * @return 审批节点列表
     */
    @Select("select A.id,A.flow_name flowName,A.`describe` as `describe`,A.p_id pId, B.u_id uId,B.role_id roleId,B.sort from sys_flow A " +
            "join sys_flow_task B on A.id = B.parent_id WHERE B.type = #{type} and B.status = 1 order by sort asc")
    List<ApproveVo> getApproveVoList(@Param("type") String type);

    /**
     * 按项目和节点类型查询审批步骤
     *
     * @param type 查询或节点类型
     * @param pId 考核项目ID
     * @return 审批节点列表
     */
    @Select("select A.id,A.flow_name flowName,A.`describe` as `describe`,A.p_id pId, B.u_id uId,B.role_id roleId,B.sort from sys_flow A " +
            "join sys_flow_task B on A.id = B.parent_id WHERE B.type = #{type} and A.p_id = #{pId} and B.status = 1 order by sort asc")
    List<ApproveVo> getFlowStepList(@Param("type") String type, Integer pId);

    /**
     * 查询父节点下的流程节点
     *
     * @param id 流程节点ID
     * @return 流程节点列表
     */
    @Select("select id,sort,type,u_id as uId,role_id as roleId,start_time as startTime,end_time as endTime,parent_id as parentId " +
            "from sys_flow_task where parent_id = #{id} and status = 1 order by sort asc")
    List<FlowTask> selectByParentId(@Param("id") Integer id);

    /**
     * 查询项目指定节点类型的最大排序值
     *
     * @param type 查询或节点类型
     * @param pId 考核项目ID
     * @return 查询得到的数值
     */
    @Select("select max(B.sort) from sys_flow A join sys_flow_task B on A.id = B.parent_id WHERE B.type = #{type} and A.p_id = #{pId} and B.status = 1 order by sort asc")
    Integer getFlowMaxSort(@Param("type") String type, Integer pId);

}
