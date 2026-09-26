package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.dto.TaskDto;
import com.hnkjzyxy.ab.model.Task;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//@CacheNamespace(implementation = RedisCacheConfig.class)
public interface TaskMapper extends BaseMapper<Task> {
    List<TaskDto> selectGroupMetrics(@Param("pId") Integer projectId);
    List<TaskDto> selectTeachGroupMetrics(@Param("pId") Integer projectId,@Param("taskId") List<String> taskId);
    @Select("SELECT distinct t.task_name FROM sys_task t WHERE t.category = #{category} and t.p_id = #{pId}")
    List<String> getTaskByCategoryName(@Param("pId") Integer projectId, @Param("category") String category);
}
