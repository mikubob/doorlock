package com.hnkjzyxy.ab.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Flow;
import com.hnkjzyxy.ab.model.FlowTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

//@CacheNamespace(implementation = RedisCacheConfig.class)
public interface FlowMapper extends BaseMapper<Flow> {

    @Select("select * from sys_flow where p_id = #{pId} and status = 1")
    Flow findFlowByPId(@Param("pId") String pId);

    @Select("SELECT B.u_id as uId,B.role_id as roleId FROM `sys_flow` A INNER JOIN sys_flow_task B on A.id = B.parent_id WHERE A.p_id = #{pId} and A.status = 1 and B.type = #{cc} and B.status = 1")
    List<FlowTask> getFlowList(@Param("cc") String cc, @Param("pId") Integer pId);

    @Select("select user_id from sys_user_role where role_id = #{id}")
    List<Integer> findUserIdByRoleId(@Param("id") Integer item);


    @Select("<script>select distinct year(create_time) as createTime from sys_flow where <if test='userId!=null'>user_id = #{userId}</if> order by createTime desc</script>")
    List<String> getFlowYears(@Param("userId") Integer userId);

    @Update("update sys_flow set status = #{status} where p_id = #{pId}")
    void updateFLowStatusByPId(@Param("pId") Integer pId, @Param("status") Integer status);

    @Select("select id from sys_flow where  p_id = #{id}")
    Integer getFLowByProjectId(Integer id);
}
