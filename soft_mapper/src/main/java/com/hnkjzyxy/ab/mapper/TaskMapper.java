package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.dto.TaskDto;
import com.hnkjzyxy.ab.model.Task;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考核任务数据访问接口
 */
public interface TaskMapper extends BaseMapper<Task> {
    /**
     * 按项目汇总任务分类指标
     *
     * @param projectId 考核项目ID
     * @return 按任务分类汇总的项目指标列表
     */
    List<TaskDto> selectGroupMetrics(@Param("pId") Integer projectId);

    /**
     * 按项目和任务集合汇总教师任务指标
     *
     * @param projectId 考核项目ID
     * @param taskId 任务ID
     * @return 指定任务集合按分类汇总的教师指标列表
     */
    List<TaskDto> selectTeachGroupMetrics(@Param("pId") Integer projectId,@Param("taskId") List<String> taskId);

    /**
     * 查询项目指定分类下的任务名称
     *
     * @param projectId 考核项目ID
     * @param category 任务分类
     * @return 指定项目及分类下去重后的任务名称列表
     */
    List<String> getTaskByCategoryName(@Param("pId") Integer projectId, @Param("category") String category);
}
