package com.hnkjzyxy.ab.init;
import com.hnkjzyxy.ab.config.LockScheduleReconcileService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
/**
 * 启动时按持久化期望状态恢复任务，不补发已错过动作。
 */
@Component
public class ScheduleLoad implements CommandLineRunner {
    /**
     * 调度对账服务。
     */
    private final LockScheduleReconcileService service;
    /**
     * 创建任务恢复器。
     * @param service 对账服务
     */
    public ScheduleLoad(LockScheduleReconcileService service) { this.service = service; }
    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args) { service.reconcile(); }
}
