package com.hnkjzyxy.ab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 课表同步参数配置
 * <p>
 * 对应 application-*.yml 中的 {@code schedule.sync.*} 节点。
 * 由 SoftApplication 上的 {@code @ConfigurationPropertiesScan} 自动注册，无需再加 {@code @Component}。
 * </p>
 *
 * @version 1.0
 * @date 2026-09-29
 */
@Data
@ConfigurationProperties(prefix = "schedule.sync")
public class ScheduleSyncProperties {

    /**
     * 同步总开关，false 时定时任务与手动刷新均直接跳过
     */
    private boolean enabled = true;

    /**
     * 应用启动完成后是否同步一次，生产环境默认为 false，避免「重启即整表替换」
     */
    private boolean syncOnStartup = false;

    /**
     * 每日同步的 cron 表达式（秒 分 时 日 月 周）
     */
    private String cron = "0 0 3 * * ?";

    /**
     * 批量写入条数
     */
    private int batchSize = 500;

    /**
     * 本次同步允许的最小有效记录数，低于该值视为接口异常，放弃同步并保留旧数据
     */
    private int minRows = 50;

    /**
     * 收缩保护比例：本次有效记录数 / 替换前记录数 低于该比例时放弃同步
     */
    private double shrinkGuardRatio = 0.5D;

    /**
     * OA 接口单次请求返回条数上限。本次返回条数达到该值说明数据被截断，放弃同步
     */
    private int pageSize = 1000;

    /**
     * 同步分布式锁的持有时长（秒），防止持有者宕机后死锁
     */
    private long lockTtlSeconds = 1800L;
}
