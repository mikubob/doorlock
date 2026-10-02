package com.hnkjzyxy.ab.job;

import com.hnkjzyxy.ab.service.support.EvidenceFileCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 文件清理任务入口，保留原有默认不执行清理的行为。 */
@Slf4j
@Component
public class EvidenceFileCleanupJob {
    private final EvidenceFileCleanupService cleanupService;
    private final boolean enabled;

    public EvidenceFileCleanupJob(EvidenceFileCleanupService cleanupService,
            @Value("${file.cleanup.enabled:false}") boolean enabled) {
        this.cleanupService = cleanupService;
        this.enabled = enabled;
    }

    public void clearFileJob() {
        if (!enabled) {
            log.info("文件清理任务未启用，跳过执行");
            return;
        }
        cleanupService.clearFiles();
    }
}
