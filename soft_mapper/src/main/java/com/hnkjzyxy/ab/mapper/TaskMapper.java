package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.dto.TaskDto;
import com.hnkjzyxy.ab.model.Task;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//@CacheNamespace(implementation = MybatisRedisCache.class)
/**
 * TaskMapper数据访问接口
 */
public interface TaskMapper extends BaseMapper<Task> {
    /**
     * 按项目汇总任务分类指标
     *
     * @param projectId 考核项目ID
     * @return 查询结果列表
     */
    List<TaskDto> selectGroupMetrics(@Param("pId") Integer projectId);
    /**
     * 按项目和任务集合汇总教师任务指标
     *
     * @param projectId 考核项目ID
     * @param taskId 任务ID
     * @return 查询结果列表
     */
    List<TaskDto> selectTeachGroupMetrics(@Param("pId") Integer projectId,@Param("taskId") List<String> taskId);
    /**
     * 查询项目指定分类下的任务名称
     *
     * @param projectId 考核项目ID
     * @param category 任务分类
     * @return 查询结果列表
     */
    @Select("SELECT distinct t.task_name FROM sys_task t WHERE t.category = #{category} and t.p_id = #{pId}")
    List<String> getTaskByCategoryName(@Param("pId") Integer projectId, @Param("category") String category);
}
