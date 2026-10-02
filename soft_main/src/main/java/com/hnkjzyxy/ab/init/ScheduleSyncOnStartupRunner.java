package com.hnkjzyxy.ab.init;

import com.hnkjzyxy.ab.service.CourseScheduleService;
import com.hnkjzyxy.ab.vo.CourseScheduleSyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 应用启动后同步一次课表
 * <p>
 * 仅在 {@code schedule.sync.sync-on-startup=true} 时注册；默认关闭，避免「重启即整表替换」。
 * 相比原先的 {@code @PostConstruct + Thread.sleep(5000)}：容器已完全启动、数据源与 OA 配置
 * 均已就绪，且不阻塞 Bean 初始化线程。
 * </p>
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE)
@ConditionalOnProperty(name = "schedule.sync.sync-on-startup", havingValue = "true")
public class ScheduleSyncOnStartupRunner implements ApplicationRunner {

    private final CourseScheduleService courseScheduleService;

    public ScheduleSyncOnStartupRunner(CourseScheduleService courseScheduleService) {
        this.courseScheduleService = courseScheduleService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("应用启动完成，执行一次课表同步（schedule.sync.sync-on-startup=true）");
        CourseScheduleSyncResult result = courseScheduleService.sync("startup");
        if (result.isSuccess()) {
            log.info("启动期课表同步成功：{}", result);
        } else {
            log.warn("启动期课表同步未成功，课表保持原有数据：{}", result);
        }
    }
}
