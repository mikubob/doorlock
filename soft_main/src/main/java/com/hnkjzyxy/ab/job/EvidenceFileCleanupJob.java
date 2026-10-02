package com.hnkjzyxy.ab.job;

import com.hnkjzyxy.ab.service.support.EvidenceFileCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 佐证材料文件清理任务入口，仅在明确启用后执行清理
 */
@Slf4j
@Component
public class EvidenceFileCleanupJob {
    /**
     * 佐证材料冗余文件清理服务
     */
    private final EvidenceFileCleanupService cleanupService;
    /**
     * 文件清理启用开关，默认 false
     */
    private final boolean enabled;

    /**
     * 初始化文件清理任务
     *
     * @param cleanupService 佐证材料文件清理服务
     * @param enabled 是否启用清理，对应 file.cleanup.enabled，默认 false
     */
    public EvidenceFileCleanupJob(EvidenceFileCleanupService cleanupService,
            @Value("${file.cleanup.enabled:false}") boolean enabled) {
        this.cleanupService = cleanupService;
        this.enabled = enabled;
    }

    /**
     * 根据启用配置执行佐证材料文件清理
     * <p>
     * file.cleanup.enabled 默认为 false；未启用时记录日志并直接返回，不调用清理服务。
     * </p>
     */
    public void clearFileJob() {
        if (!enabled) {
            log.info("文件清理任务未启用，跳过执行");
            return;
        }
        cleanupService.clearFiles();
    }
}
