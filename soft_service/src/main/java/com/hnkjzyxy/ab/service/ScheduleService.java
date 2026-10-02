package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.model.ScheduleTask;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门禁定时任务Service接口
 */
public interface ScheduleService {
    /**
     * 查询所有门禁定时任务
     *
     * @return 门禁定时任务列表
     */
    public List<ScheduleTask> getAll();

    /**
     * 查询指定时间范围内的门禁定时任务
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 门禁定时任务列表
     */
    public List<ScheduleTask> getByTime(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 新增门禁定时任务
     *
     * @param scheduleTask 门禁定时任务信息
     * @return 受影响的记录数量
     */
    public int insert(ScheduleTask scheduleTask);

    /**
     * 更新门禁定时任务
     *
     * @param scheduleTask 门禁定时任务信息
     * @return 受影响的记录数量
     */
    public int update(ScheduleTask scheduleTask);

    /**
     * 删除指定门禁定时任务
     *
     * @param id 门禁定时任务ID
     * @return 受影响的记录数量
     */
    public int delete(Integer id);

    /**
     * 更新门禁定时任务状态
     *
     * @param scheduleTask 门禁定时任务信息
     * @return 受影响的记录数量
     */
    public int updateStatus(ScheduleTask scheduleTask);

    /**
     * 按ID查询门禁定时任务
     *
     * @param id 门禁定时任务ID
     * @return 门禁定时任务信息
     */
    public ScheduleTask getById(int id);

    /**
     * 查询用户创建的门禁定时任务
     *
     * @param userId 用户ID
     * @return 门禁定时任务列表
     */
    public List<ScheduleTask> getByUserId(int userId);

    /**
     * 更新门禁定时任务的剩余执行次数
     *
     * @param loopCount 剩余执行次数
     * @param taskId 任务ID
     * @return 受影响的记录数量
     */
    public int updateLoopCount(int loopCount, int taskId);
}
