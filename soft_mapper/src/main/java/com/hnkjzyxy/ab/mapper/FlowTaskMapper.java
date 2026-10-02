package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.vo.ApproveVo;
import com.hnkjzyxy.ab.vo.FlowTaskVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//@CacheNamespace(implementation = MybatisRedisCache.class)
public interface FlowTaskMapper extends BaseMapper<FlowTask> {
    @Select("select A.p_id pId, B.u_id uId,B.role_id roleId from sys_flow A " +
            "join sys_flow_task B on A.id = B.parent_id WHERE B.type = #{type} and B.status = 1")
    List<FlowTaskVo> getFlowTaskList(@Param("type") String type);

    @Select("select A.id,A.flow_name flowName,A.`describe` as `describe`,A.p_id pId, B.u_id uId,B.role_id roleId,B.sort from sys_flow A " +
            "join sys_flow_task B on A.id = B.parent_id WHERE B.type = #{type} and B.status = 1 order by sort asc")
    List<ApproveVo> getApproveVoList(@Param("type") String type);

    @Select("select A.id,A.flow_name flowName,A.`describe` as `describe`,A.p_id pId, B.u_id uId,B.role_id roleId,B.sort from sys_flow A " +
            "join sys_flow_task B on A.id = B.parent_id WHERE B.type = #{type} and A.p_id = #{pId} and B.status = 1 order by sort asc")
    List<ApproveVo> getFlowStepList(@Param("type") String type, Integer pId);

    @Select("select id,sort,type,u_id as uId,role_id as roleId,start_time as startTime,end_time as endTime,parent_id as parentId " +
            "from sys_flow_task where parent_id = #{id} and status = 1 order by sort asc")
    List<FlowTask> selectByParentId(@Param("id") Integer id);

    @Select("select max(B.sort) from sys_flow A join sys_flow_task B on A.id = B.parent_id WHERE B.type = #{type} and A.p_id = #{pId} and B.status = 1 order by sort asc")
    Integer getFlowMaxSort(@Param("type") String type, Integer pId);

}
