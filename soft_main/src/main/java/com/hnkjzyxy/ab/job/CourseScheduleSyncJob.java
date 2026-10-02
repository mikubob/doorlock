package com.hnkjzyxy.ab.job;

import com.hnkjzyxy.ab.init.ScheduleSyncOnStartupRunner;

import com.hnkjzyxy.ab.service.CourseScheduleService;
import com.hnkjzyxy.ab.vo.CourseScheduleSyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 课表同步定时任务入口
 * <p>
 * 只负责「何时执行」，具体同步逻辑（拉取、校验、事务替换、分布式锁、数据量保护）
 * 全部在 {@link CourseScheduleService#sync(String)} 中实现。
 * </p>
 * <p>
 * 注意：启动期同步已从 @PostConstruct 中移除（原实现会在 Bean 初始化阶段
 * 先清空课表再拉取，属于不可恢复的数据丢失风险）。如需启动同步，请开启
 * {@code schedule.sync.sync-on-startup}，由 {@link ScheduleSyncOnStartupRunner} 在
 * 容器完全启动后执行。
 * </p>
 */
@Slf4j
@Component
public class CourseScheduleSyncJob {

    /**
     * 课表业务服务
     */
    private final CourseScheduleService courseScheduleService;

    /**
     * 初始化CourseScheduleSyncJob
     *
     * @param courseScheduleService 课表业务服务
     */
    public CourseScheduleSyncJob(CourseScheduleService courseScheduleService) {
        this.courseScheduleService = courseScheduleService;
    }

    /**
     * 定时任务 - 每天凌晨3点执行同步课表
     * <p>
     * Cron 表达式: 秒 分 时 日 月 周，默认 {@code 0 0 3 * * ?} 表示每天 03:00:00 执行，
     * 可通过 {@code schedule.sync.cron} 覆盖。
     * </p>
     */
    @Scheduled(cron = "${schedule.sync.cron:0 0 3 * * ?}")
    public void executeDaily() {
        CourseScheduleSyncResult result = courseScheduleService.sync("cron");
        if (result.isSuccess()) {
            log.info("定时任务执行成功：{}", result);
        } else if (result.isExecuted()) {
            // 本轮确实跑了但失败，正式表数据保持上一轮结果，需要关注
            log.error("定时任务执行失败，课表数据保持上一轮结果：{}", result);
        } else {
            log.warn("定时任务本次未执行：{}", result.getMessage());
        }
    }
}
