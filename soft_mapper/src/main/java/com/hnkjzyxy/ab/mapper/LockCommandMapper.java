package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.LockCommand;
import org.apache.ibatis.annotations.*;

/**
 * 命令持久化及回执幂等更新。
 */
@Mapper
public interface LockCommandMapper extends BaseMapper<LockCommand> {
    /**
     * 取得任务行锁，防止多实例重复保留有限任务执行。
     *
     * @param taskId 真实任务主键
     * @return 最新任务
     */
    com.hnkjzyxy.ab.model.ScheduleTask lockTask(@Param("taskId") Integer taskId);

    /**
     * 命令登记时使旧设备观测失效，提交不能代表物理状态。
     *
     * @param lock 提交时核实的真实锁绑定
     * @return 影响行数
     */
    int invalidateObservation(com.hnkjzyxy.ab.model.LockInfo lock);
    /**
     * 只完成尚未完成的命令。
     * @param command 回执内容
     * @return 首次完成时为一
     */
    int complete(LockCommand command);

    /**
     * 独立人工复核只能收敛尚未复核的未知命令。
     *
     * @param command 明确确认的结果
     * @return 首次复核为一
     */
    int reviewUnknown(LockCommand command);
    /**
     * 原子扣减成功回执对应任务的剩余次数，零为耗尽，负一为无限。
     * @param taskId 真实任务主键
     * @return 影响行数
     */
    int consume(@Param("taskId") Integer taskId);
    /**
     * 将长期没有回执的提交标记为未知，不补发历史命令。
     * @return 标记行数
     */
    int expirePending();
}
