package com.hnkjzyxy.ab.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.model.ScheduleTask;
import com.hnkjzyxy.ab.service.ScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 数据库期望状态与 Quartz 定期对账，注册失败保留数据库配置并重试。
 */
@Slf4j
@Service
public class LockScheduleReconcileService {
    /**
     * 任务持久化服务。
     */
    private final ScheduleService service;
    /**
     * Quartz 配置。
     */
    private final QuartzConfig config;
    /**
     * 调度器。
     */
    private final SchedulerFactoryBean factory;
    /**
     * 持久化对账结果，不修改业务配置版本。
     */
    private final com.hnkjzyxy.ab.mapper.ScheduleMapper mapper;
    /**
     * 创建调度对账服务。
     * @param service 任务服务
     * @param config Quartz 配置
     * @param factory 调度器
     * @param mapper 任务访问
     */
    public LockScheduleReconcileService(ScheduleService service, QuartzConfig config, SchedulerFactoryBean factory,
            com.hnkjzyxy.ab.mapper.ScheduleMapper mapper) {
        this.service = service; this.config = config; this.factory = factory; this.mapper = mapper;
    }
    /**
     * 每分钟重试注册和撤销；单个坏任务不阻止其他任务恢复。
     */
    @Scheduled(fixedDelay = 60000)
    public synchronized void reconcile() {
        try {
            Scheduler scheduler = factory.getScheduler();
            List<ScheduleTask> tasks = service.getAll();
            Set<JobKey> desired = new HashSet<>();
            for (ScheduleTask task : tasks) {
                JobKey key = new JobKey("smartLockJob_" + task.getTaskId(), "smartLockGroup");
                try {
                    if (task.getTaskStatus() != 1 || task.getLoopCount() == 0) {
                        if (scheduler.checkExists(key)) scheduler.deleteJob(key);
                        mark(task, "synced", "未启用、取消或耗尽任务已撤销");
                        continue;
                    }
                    int[] days = new ObjectMapper().readValue(task.getCountDay(), int[].class);
                    if (task.getDoorChannel() == null || !task.getDoorChannel().matches("[1-4]")
                            || task.getLoopCount() < -1 || (task.getTimedOperation() != 0 && task.getTimedOperation() != 1)) throw new IllegalArgumentException("通道、次数或动作尚未确认");
                    String cron = config.generateWeeklyCronExpression(task.getHour(), task.getMinute(), days);
                    desired.add(key);
                    String version = version(task);
                    if (scheduler.checkExists(key) && version.equals(scheduler.getJobDetail(key).getJobDataMap().getString("sourceVersion"))) {
                        if (!"synced".equals(task.getQuartzSyncStatus())) mark(task, "synced", "期望配置与 Quartz 一致");
                        continue;
                    }
                    if (scheduler.checkExists(key)) scheduler.deleteJob(key);
                    JobDetail job = config.createJobDetail(task.getLockId(), task.getUserId(), task.getTaskId(), task.getDoorChannel());
                    job.getJobDataMap().put("sourceVersion", version);
                    scheduler.scheduleJob(job, config.createCronTrigger(job, cron));
                    mark(task, "synced", "期望配置已注册");
                } catch (Exception error) {
                    mark(task, "failed", "调度对账失败，将自动重试；请核实通道、星期、动作及调度器");
                    log.warn("门禁任务 {} 尚未完成对账，将重试：{}", task.getTaskId(), error.getMessage());
                }
            }
            for (JobKey key : scheduler.getJobKeys(GroupMatcher.jobGroupEquals("smartLockGroup"))) if (!desired.contains(key)) scheduler.deleteJob(key);
        } catch (Exception error) { log.warn("门禁调度对账失败，将重试", error); }
    }
    /**
     * 生成触发时复核用的配置版本，不包含逐次减少的次数。
     * @param task 任务
     * @return 配置版本
     */
    public static String version(ScheduleTask task) {
        return task.getLockId() + "|" + task.getDoorChannel() + "|" + task.getTimedOperation() + "|"
                + task.getHour() + "|" + task.getMinute() + "|" + task.getCountDay() + "|" + task.getUpdatedTime() + "|" + task.getRowVersion();
    }

    /**
     * 登记同一配置版本的对账结果；失败不影响后续任务恢复。
     *
     * @param task 对账来源配置
     * @param status 同步状态
     * @param message 可公开的对账原因
     */
    private void mark(ScheduleTask task, String status, String message) {
        try {
            mapper.markSyncState(task.getTaskId(), task.getRowVersion(), task.getTaskStatus(), status, message,
                    "synced".equals(status) ? java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")) : null);
        } catch (RuntimeException error) { log.warn("任务 {} 的对账结果暂未保存，将重试", task.getTaskId()); }
    }
}
