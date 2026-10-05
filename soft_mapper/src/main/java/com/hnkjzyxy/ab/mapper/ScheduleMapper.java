package com.hnkjzyxy.ab.mapper;

import com.hnkjzyxy.ab.model.ScheduleTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
    List<ScheduleTask> getAll();

    /**
     * 按ID查询门禁定时任务
     *
     * @param id 门禁定时任务ID
     * @return 门禁定时任务信息
     */
    ScheduleTask getById(@Param("id") int id);

    /**
     * 查询用户创建的门禁定时任务
     *
     * @param userId 用户ID
     * @return 门禁定时任务列表
     */
    List<ScheduleTask> getByUserId(@Param("userId") int userId);

    /**
     * 按更新时间区间查询定时任务
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 定时任务列表
     */
    List<ScheduleTask> getByTime(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 新增定时任务
     *
     * @param scheduleTask 定时任务
     * @return 影响行数
     */
    int insert(ScheduleTask scheduleTask);

    /**
     * 更新定时任务
     *
     * @param scheduleTask 定时任务
     * @return 影响行数
     */
    int update(ScheduleTask scheduleTask);

    /**
     * 更新任务状态与已执行次数
     *
     * @param taskId     任务ID
     * @param taskStatus 任务状态
     * @param loopCount  已循环次数
     * @return 影响行数
     */
    int updateStatus(@Param("taskId") int taskId, @Param("taskStatus") int taskStatus, @Param("loopCount") int loopCount);

    /**
     * 删除指定门禁定时任务
     *
     * @param id 门禁定时任务ID
     * @return 受影响的记录数量
     */
    int delete(@Param("id") Integer id);

    /**
     * 更新门禁定时任务的剩余执行次数
     *
     * @param loopCount 剩余执行次数
     * @param taskId 任务ID
     * @return 受影响的记录数量
     */
    int updateLoopCount(@Param("loopCount") int loopCount, @Param("taskId") int taskId);

    /**
     * 仅为仍然相同的期望配置登记对账结果，不覆盖并发新版本。
     *
     * @param taskId 真实任务主键
     * @param rowVersion 对账读取的配置版本
     * @param taskStatus 对账读取的业务状态
     * @param syncStatus 对账结果
     * @param message 结果说明
     * @param syncedAt 成功时间，失败时为空
     * @return 影响行数
     */
    int markSyncState(@Param("taskId") int taskId, @Param("rowVersion") Long rowVersion,
            @Param("taskStatus") int taskStatus, @Param("syncStatus") String syncStatus,
            @Param("message") String message, @Param("syncedAt") LocalDateTime syncedAt);
}
