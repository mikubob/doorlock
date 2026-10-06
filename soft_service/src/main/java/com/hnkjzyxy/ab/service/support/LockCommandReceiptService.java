package com.hnkjzyxy.ab.service.support;

import com.hnkjzyxy.ab.dto.LockCommandReviewDto;
import com.hnkjzyxy.ab.mapper.LockCommandMapper;
import com.hnkjzyxy.ab.model.LockCommand;
import com.hnkjzyxy.ab.service.CoursePeriodResolver;
import com.hnkjzyxy.ab.service.ScheduleWriteCoordinator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

/**
 * 回执与成功次数在同一事务中收敛，重复回执不重复记数。
 */
@Service
public class LockCommandReceiptService {
    /**
     * 命令数据访问。
     */
    private final LockCommandMapper mapper;
    /**
     * 人工确认需要理由、认证操作者及共同事务审计。
     */
    @Autowired
    private ScheduleWriteCoordinator coordinator;
    /**
     * 创建回执服务。
     * @param mapper 命令访问
     */
    public LockCommandReceiptService(LockCommandMapper mapper) { this.mapper = mapper; }
    /**
     * 幂等接收厂家回执。
     * @param command 命令身份
     * @param error 回执故障
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(LockCommand command, Throwable error) {
        Throwable cause = error;
        while (cause != null && cause.getCause() != null) cause = cause.getCause();
        boolean timeout = cause instanceof TimeoutException;
        command.setStatus(error == null ? "acknowledged" : timeout ? "unknown" : "failed");
        command.setErrorMessage(error == null ? null : timeout ? "回执超时，设备状态待人工确认" : "设备命令失败");
        command.setCompletedTime(LocalDateTime.now(CoursePeriodResolver.ZONE));
        if (mapper.complete(command) == 1 && error == null && command.getTaskId() != null) mapper.consume(command.getTaskId());
    }

    /**
     * 复核超期/超时命令，确认执行时只计一次，不补发原命令、不伪造门状态。
     *
     * @param id 未知命令主键
     * @param request 明确核查结果和依据
     */
    @Transactional(rollbackFor = Exception.class)
    public void review(String id, LockCommandReviewDto request) {
        if (request == null || request.getExecuted() == null || request.getReason() == null
                || request.getReason().trim().isEmpty() || request.getReason().length() > 500) {
            throw new IllegalArgumentException("人工复核必须明确是否执行，并填写不超过500字的核查依据");
        }
        coordinator.lock();
        LockCommand command = mapper.selectById(id);
        if (command == null || !"unknown".equals(command.getStatus())) throw new IllegalArgumentException("命令不存在或已被复核，请刷新");
        String before = command.toString();
        command.setStatus(request.getExecuted() ? "acknowledged" : "failed");
        command.setErrorMessage(request.getExecuted() ? "管理员依据现场核查确认原指令已执行" : "管理员依据现场核查确认原指令未执行");
        command.setCompletedTime(LocalDateTime.now(CoursePeriodResolver.ZONE));
        if (mapper.reviewUnknown(command) != 1) throw new IllegalArgumentException("命令已被其他管理员复核，请刷新");
        if (request.getExecuted() && command.getTaskId() != null) mapper.consume(command.getTaskId());
        coordinator.changed("LOCK_COMMAND_REVIEW", "before=" + before + "; after=" + command + "; reason=" + request.getReason().trim());
    }
    /**
     * 过期命令保持未知，不能当作成功或自动补发。
     */
    @Scheduled(fixedDelay = 60000)
    public void expire() { mapper.expirePending(); }
}
