package com.hnkjzyxy.ab.job;

import com.hnkjzyxy.ab.service.ExamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每十五秒按学校时间持久化考试生命周期，启动后补齐停机期间的状态。
 */
@Slf4j
@Component
public class ExamStatusRefreshJob {
    /**
     * 考试事务服务。
     */
    private final ExamService examService;

    /**
     * 创建考试状态任务。
     * @param examService 考试服务
     */
    public ExamStatusRefreshJob(ExamService examService) { this.examService = examService; }

    /**
     * 自动状态更新不触发任何门锁设备操作，失败保留原状态供下轮重试。
     */
    @Scheduled(fixedDelay = 15000, initialDelay = 1000)
    public void refresh() {
        try { examService.refreshStatuses(); }
        catch (RuntimeException error) { log.error("自动刷新考试状态失败", error); }
    }
}
