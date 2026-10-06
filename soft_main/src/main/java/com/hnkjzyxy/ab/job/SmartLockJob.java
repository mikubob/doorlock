package com.hnkjzyxy.ab.job;

import com.hnkjzyxy.ab.config.LockScheduleReconcileService;
import com.hnkjzyxy.ab.model.*;
import com.hnkjzyxy.ab.service.*;
import com.hnkjzyxy.ab.service.support.LockCommandService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 按真实任务身份触发，复核最新状态及配置，设备回执才扣减次数。
 */
@Component
@DisallowConcurrentExecution
public class SmartLockJob implements Job {
    /**
     * 任务来源。
     */
    @Autowired
    private ScheduleService scheduleService;
    /**
     * 真实设备访问。
     */
    @Autowired
    private SmartLockService smartLockService;
    /**
     * 持久化命令及幂等回执。
     */
    @Autowired
    private LockCommandService commandService;
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDataMap data = context.getJobDetail().getJobDataMap();
            ScheduleTask task = scheduleService.getById(data.getInt("taskId"));
            if (task == null || task.getTaskStatus() != 1 || task.getLoopCount() == 0
                    || !LockScheduleReconcileService.version(task).equals(data.getString("sourceVersion"))) return;
            LockInfo device = smartLockService.getById(task.getLockId());
            if (device == null || device.getClassroomId() == null || !Objects.equals(task.getDoorChannel(), device.getDoorChannel())) {
                throw new IllegalArgumentException("设备教室或通道绑定已改变，请独立核实任务");
            }
            String requestId = "timer:" + task.getTaskId() + ":" + context.getScheduledFireTime().getTime();
            commandService.submit(device, task.getDoorChannel(), task.getTimedOperation() == 1,
                    "user:" + task.getUserId(), task.getTaskId(), requestId, task.getRowVersion());
        } catch (Exception error) { throw new JobExecutionException("门禁任务提交失败", error); }
    }
}
