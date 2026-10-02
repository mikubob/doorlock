package com.hnkjzyxy.ab.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Flow;
import com.hnkjzyxy.ab.model.FlowTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

//@CacheNamespace(implementation = MybatisRedisCache.class)
/**
 * 审批流程数据访问接口
 */
public interface FlowMapper extends BaseMapper<Flow> {

    /**
     * 按项目ID查询审批流程
     *
     * @param pId 考核项目ID
     * @return 审批流程信息
     */
    @Select("select * from sys_flow where p_id = #{pId} and status = 1")
    Flow findFlowByPId(@Param("pId") String pId);

    /**
     * 按项目和节点类型查询流程节点
     *
     * @param cc 流程节点类型
     * @param pId 考核项目ID
     * @return 流程节点列表
     */
    @Select("SELECT B.u_id as uId,B.role_id as roleId FROM `sys_flow` A INNER JOIN sys_flow_task B on A.id = B.parent_id WHERE A.p_id = #{pId} and A.status = 1 and B.type = #{cc} and B.status = 1")
    List<FlowTask> getFlowList(@Param("cc") String cc, @Param("pId") Integer pId);

    /**
     * 按角色ID查询用户ID
     *
     * @param item 角色ID
     * @return 查询结果列表
     */
    @Select("select user_id from sys_user_role where role_id = #{id}")
    List<Integer> findUserIdByRoleId(@Param("id") Integer item);


    /**
     * 查询用户相关审批流程的年度
     *
     * @param userId 用户ID
     * @return 查询结果列表
     */
    @Select("<script>select distinct year(create_time) as createTime from sys_flow where <if test='userId!=null'>user_id = #{userId}</if> order by createTime desc</script>")
    List<String> getFlowYears(@Param("userId") Integer userId);

    /**
     * 更新指定项目的流程状态
     *
     * @param pId 考核项目ID
     * @param status 状态值
     */
    @Update("update sys_flow set status = #{status} where p_id = #{pId}")
    void updateFLowStatusByPId(@Param("pId") Integer pId, @Param("status") Integer status);

    /**
     * 查询项目对应的流程ID
     *
     * @param id 审批流程ID
     * @return 查询得到的数值
     */
    @Select("select id from sys_flow where  p_id = #{id}")
    Integer getFLowByProjectId(Integer id);
}
