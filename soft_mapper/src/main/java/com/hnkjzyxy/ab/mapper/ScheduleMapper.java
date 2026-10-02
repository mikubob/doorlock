package com.hnkjzyxy.ab.mapper;

import com.hnkjzyxy.ab.model.ScheduleTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门禁定时任务数据访问接口
 */
@Mapper
public interface ScheduleMapper {
    /**
     * 查询所有门禁定时任务
     *
     * @return 门禁定时任务列表
     */
    @Select("select * from sys_schedule_task")
    public List<ScheduleTask> getAll();

    /**
     * 按ID查询门禁定时任务
     *
     * @param id 门禁定时任务ID
     * @return 门禁定时任务信息
     */
    @Select("select * from sys_schedule_task where task_id=#{id}")
    public ScheduleTask getById(int id);

    /**
     * 查询用户创建的门禁定时任务
     *
     * @param userId 用户ID
     * @return 门禁定时任务列表
     */
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

    /**
     * 删除指定门禁定时任务
     *
     * @param id 门禁定时任务ID
     * @return 受影响的记录数量
     */
    @Delete("delete from sys_schedule_task where task_id=#{id}")
    public int delete(Integer id);

    /**
     * 更新门禁定时任务的剩余执行次数
     *
     * @param loopCount 剩余执行次数
     * @param taskId 任务ID
     * @return 受影响的记录数量
     */
    @Update("update sys_schedule_task set loop_count=#{loopCount} where task_id=#{taskId}")
    public int updateLoopCount(@Param("loopCount") int loopCount, @Param("taskId") int taskId);
}