package com.hnkjzyxy.ab.mapper;

import com.hnkjzyxy.ab.model.ScheduleTask;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ScheduleMapper {
    @Select("select * from sys_schedule_task")
    public List<ScheduleTask> getAll();

    @Select("select * from sys_schedule_task where task_id=#{id}")
    public ScheduleTask getById(int id);

    @Select("select * from sys_schedule_task where user_id=#{userId}")
    public List<ScheduleTask> getByUserId(int userId);

    /**
     * 按更新时间区间查询定时任务
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 定时任务列表
     */
    @Select("select * from sys_schedule_task where updated_time >= #{startTime} and updated_time <= #{endTime}")
    public List<ScheduleTask> getByTime(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 新增定时任务
     *
     * @param scheduleTask 定时任务
     * @return 影响行数
     */
    @Insert("insert into sys_schedule_task(lock_id,user_id,timed_operation,task_details,task_status,is_loop,remarks,created_time,hour,minute,updated_time,count_day,loop_count) " +
            "values(#{lockId},#{userId},#{timedOperation},#{taskDetails},#{taskStatus},#{isLoop},#{remarks},#{createdTime},#{hour},#{minute},#{updatedTime},#{countDay},#{loopCount})")
    public int insert(ScheduleTask scheduleTask);

    /**
     * 更新定时任务
     *
     * @param scheduleTask 定时任务
     * @return 影响行数
     */
    @Update("update sys_schedule_task set lock_id=#{lockId},user_id=#{userId},timed_operation=#{timedOperation},task_details=#{taskDetails},task_status=#{taskStatus},is_loop=#{isLoop},remarks=#{remarks},created_time=#{createdTime},hour=#{hour},minute=#{minute},updated_time=#{updatedTime},count_day=#{countDay},loop_count=#{loopCount} where task_id=#{taskId}")
    public int update(ScheduleTask scheduleTask);

    /**
     * 更新任务状态与已执行次数
     *
     * @param taskId     任务ID
     * @param taskStatus 任务状态
     * @param loopCount  已循环次数
     * @return 影响行数
     */
    @Update("update sys_schedule_task set task_status=#{taskStatus},loop_count=#{loopCount} where task_id=#{taskId}")
    public int updateStatus(@Param("taskId") int taskId, @Param("taskStatus") int taskStatus, @Param("loopCount") int loopCount);

    @Delete("delete from sys_schedule_task where task_id=#{id}")
    public int delete(Integer id);

    @Update("update sys_schedule_task set loop_count=#{loopCount} where task_id=#{taskId}")
    public int updateLoopCount(@Param("loopCount") int loopCount, @Param("taskId") int taskId);
}