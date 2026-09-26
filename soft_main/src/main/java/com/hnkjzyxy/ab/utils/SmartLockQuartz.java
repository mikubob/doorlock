package com.hnkjzyxy.ab.utils;

import com.hnkjzyxy.ab.model.LockInfo;
import com.hnkjzyxy.ab.model.ScheduleTask;
import com.hnkjzyxy.ab.model.SwitchRecord;
import com.hnkjzyxy.ab.service.ScheduleService;
import com.hnkjzyxy.ab.service.SmartLockService;
import com.hnkjzyxy.ab.service.SwitchRecordService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SmartLockQuartz implements Job {

    @Autowired
    private SmartLockService smartLockService;

    @Autowired
    private SwitchRecordService switchRecordService;
    @Autowired
    ScheduleService scheduleService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("定时任务启动");
        try {
            // 获取传递的参数
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            int lockId = dataMap.getInt("lockId");
            Integer userId = dataMap.getInt("userId");
            int taskId = dataMap.getInt("taskId");
            String Channel= dataMap.getString("Channel");
            handleExecutionCount(context, lockId,userId);
            // 检查服务是否注入成功（防御性编程）
            if (smartLockService == null) {
                throw new IllegalStateException("SmartLockService未正确注入");
            }
            if (switchRecordService == null) {
                throw new IllegalStateException("SwitchRecordService未正确注入");
            }

            // 获取锁信息
            LockInfo lockInfo = smartLockService.getById(lockId);
            if (lockInfo == null) {
                throw new IllegalArgumentException("锁ID " + lockId + " 不存在");
            }

            // 执行开锁
            SmartLockSwitch.openDoor(
                    lockInfo.getIpAddress(),
                    lockInfo.getPortNumber(),
                    lockInfo.getSnCode(),
                    Channel
            );

            // 记录开锁操作
            recordSwitchOperation(lockId, userId, 1);
            // 更新锁状态为开启
            smartLockService.updateSwitchStatus(lockId, 1);
            ScheduleTask scheduleTask=scheduleService.getById(taskId);
            int currentCount=scheduleTask.getLoopCount();
            scheduleService.updateLoopCount(currentCount-1,taskId);
            System.out.println("定时任务结束");
        } catch (Exception e) {
            throw new JobExecutionException("定时任务执行失败", e);
        }
    }

    /**
     * 记录开关锁操作
     *
     * @param lockId          锁ID
     * @param userId          操作用户ID
     * @param operationMethod 操作方式（0=关，1=开）
     */
    private void recordSwitchOperation(int lockId, Integer userId, int operationMethod) {
        SwitchRecord switchRecord = new SwitchRecord();
        switchRecord.setOperationMethod(operationMethod);
        switchRecord.setLockId(lockId);
        switchRecord.setUserId(userId);
        switchRecord.setOperationTime(LocalDateTime.now());
        switchRecordService.insert(switchRecord);
    }
    /**
     * 处理执行次数控制
     */
    private void handleExecutionCount(JobExecutionContext context, int lockId,Integer userId ) throws SchedulerException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        // 修复：使用getInt而非getInteger，并添加默认值处理
        Integer maxCount = dataMap.containsKey("maxExecutionCount") ? dataMap.getInt("maxExecutionCount") : null;

        if (maxCount != null) {
            // 累加执行次数
            int currentCount = dataMap.getInt("currentExecutionCount") + 1;
            dataMap.put("currentExecutionCount", currentCount);

            // 达到最大执行次数，停止任务
            if (currentCount >= maxCount) {
                Scheduler scheduler = context.getScheduler();
                TriggerKey triggerKey = context.getTrigger().getKey();
                scheduler.unscheduleJob(triggerKey);
                ScheduleTask scheduleTask = new ScheduleTask();
                scheduleTask.setLockId(lockId);
                scheduleTask.setUserId(userId);
                scheduleTask.setTaskStatus(2);
                scheduleService.updateStatus(scheduleTask);
            }
        }
    }

}