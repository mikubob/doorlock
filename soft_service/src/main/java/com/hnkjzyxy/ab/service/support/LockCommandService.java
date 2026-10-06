package com.hnkjzyxy.ab.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnkjzyxy.ab.mapper.LockCommandMapper;
import com.hnkjzyxy.ab.model.*;
import com.hnkjzyxy.ab.service.CoursePeriodResolver;
import com.hnkjzyxy.ab.service.gateway.SmartLockGateway;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 先持久化命令，再提交设备，并独立处理回执；提交不回写物理状态。
 */
@Service
public class LockCommandService {
    /**
     * 命令访问。
     */
    private final LockCommandMapper mapper;
    /**
     * 通讯接口。
     */
    private final SmartLockGateway gateway;
    /**
     * 幂等回执服务。
     */
    private final LockCommandReceiptService receipts;
    /**
     * 持久化提交的独立事务。
     */
    private final TransactionTemplate transaction;
    /**
     * 创建命令服务。
     * @param mapper 命令访问
     * @param gateway 厂家通讯
     * @param receipts 回执服务
     * @param manager 数据库事务管理
     */
    public LockCommandService(LockCommandMapper mapper, SmartLockGateway gateway, LockCommandReceiptService receipts,
            PlatformTransactionManager manager) {
        this.mapper = mapper; this.gateway = gateway; this.receipts = receipts;
        this.transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    /**
     * 提交真实绑定设备命令。
     * @param lock 真实设备
     * @param channel 独立通道
     * @param open 动作
     * @param actor 认证操作者
     * @param taskId 可选任务主键
     * @param requestId 幂等键
     * @param expectedTaskVersion 触发复核的配置版本，手工操作为空
     * @return 提交凭据，物理状态保持未知
     */
    public LockCommand submit(LockInfo lock, String channel, boolean open, String actor, Integer taskId, String requestId, Long expectedTaskVersion) {
        if (lock == null || lock.getSnCode() == null || lock.getIpAddress() == null || lock.getPortNumber() == null
                || lock.getClassroomId() == null || channel == null || !channel.matches("[1-4]")
                || !channel.equals(lock.getDoorChannel())) throw new IllegalArgumentException("真实设备或通道未绑定");
        LockCommand previous = mapper.selectOne(new LambdaQueryWrapper<LockCommand>().eq(LockCommand::getRequestId, requestId));
        if (previous != null) return previous;
        LockCommand command = new LockCommand();
        command.setId(UUID.randomUUID().toString()); command.setRequestId(requestId); command.setLockId(lock.getLockId());
        command.setClassroomId(lock.getClassroomId());
        command.setTaskId(taskId); command.setDeviceSn(lock.getSnCode()); command.setDoorChannel(channel);
        command.setOperation(open ? 1 : 0); command.setStatus("submitted"); command.setActor(actor);
        command.setCreatedTime(LocalDateTime.now(CoursePeriodResolver.ZONE));
        try {
            transaction.execute(status -> {
                if (taskId != null) {
                    ScheduleTask task = mapper.lockTask(taskId);
                    if (task == null || task.getTaskStatus() != 1 || task.getLoopCount() == 0) {
                        throw new IllegalStateException("任务已停用或次数耗尽");
                    }
                    if (expectedTaskVersion == null || !expectedTaskVersion.equals(task.getRowVersion())
                            || task.getLockId() != lock.getLockId() || !channel.equals(task.getDoorChannel())
                            || task.getTimedOperation() != (open ? 1 : 0)) {
                        throw new IllegalStateException("触发后的任务配置已改变，旧动作不再提交");
                    }
                    if (mapper.selectCount(new LambdaQueryWrapper<LockCommand>().eq(LockCommand::getTaskId, taskId)
                            .in(LockCommand::getStatus, "submitted", "unknown")) > 0) {
                        throw new IllegalStateException("该任务上次指令仍待确认，请先核对设备状态");
                    }
                }
                if (mapper.insert(command) != 1) throw new IllegalStateException("命令登记失败");
                if (mapper.invalidateObservation(lock) != 1) throw new IllegalStateException("锁绑定已改变或不存在");
                return null;
            });
        } catch (DuplicateKeyException duplicate) {
            return mapper.selectOne(new LambdaQueryWrapper<LockCommand>().eq(LockCommand::getRequestId, requestId));
        }
        try { gateway.sendDoorCommand(lock.getIpAddress(), lock.getPortNumber(), lock.getSnCode(), channel, open)
                .whenComplete((ignored, error) -> receipts.complete(command, error)); }
        catch (RuntimeException error) { receipts.complete(command, error); }
        return mapper.selectById(command.getId());
    }
}
