package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Flow;
import com.hnkjzyxy.ab.model.FlowTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
    default Flow findFlowByPId(@Param("pId") String pId) {
        return selectOne(new LambdaQueryWrapper<Flow>().eq(Flow::getPId, pId));
    }

    /**
     * 按项目和节点类型查询流程节点
     *
     * @param cc 流程节点类型
     * @param pId 考核项目ID
     * @return 流程节点列表
     */
    List<FlowTask> getFlowList(@Param("cc") String cc, @Param("pId") Integer pId);

    /**
     * 按角色ID查询用户ID
     *
     * @param item 角色ID
     * @return 指定角色关联的用户ID列表
     */
    List<Integer> findUserIdByRoleId(@Param("id") Integer item);

    /**
     * 查询用户相关审批流程的年度
     *
     * @param userId 用户ID，为 null 时查询所有用户的流程年度
     * @return 去重后的流程年度列表，按年度降序排列
     */
    List<String> getFlowYears(@Param("userId") Integer userId);

    /**
     * 更新指定项目的流程状态
     *
     * @param pId 考核项目ID
     * @param status 状态值
     */
    void updateFLowStatusByPId(@Param("pId") Integer pId, @Param("status") Integer status);

    /**
     * 查询项目对应的流程ID
     *
     * @param id 考核项目ID
     * @return 项目对应的流程ID，不存在时返回 null
     */
    Integer getFLowByProjectId(@Param("id") Integer id);
}
