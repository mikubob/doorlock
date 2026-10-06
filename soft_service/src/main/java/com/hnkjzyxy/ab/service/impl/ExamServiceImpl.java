package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.mapper.ExamMapper;
import com.hnkjzyxy.ab.model.*;
import com.hnkjzyxy.ab.service.*;
import com.hnkjzyxy.ab.vo.ScheduleInterval;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考试统一写入服务，单条、批量及重新启用均验证最终排程。
 */
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {
    /**
     * 排程事务协调器。
     */
    @Autowired
    private ScheduleWriteCoordinator coordinator;

    /**
     * 学校业务时钟，与教室状态采用同一时区。
     */
    @Autowired
    @Qualifier("schoolBusinessClock")
    private Clock clock;

    /**
     * 审计序列化器，保存结构化的变更前后快照。
     */
    private final ObjectMapper auditJson = new ObjectMapper().findAndRegisterModules();

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Exam> getExamList(Exam condition) {
        coordinator.lock();
        refreshLocked();
        List<Exam> rows = baseMapper.getExamList(condition == null ? new Exam() : condition);
        for (Exam row : rows) row.setTimeStatus(row.getStatus() == null ? "unknown"
                : row.getStatus() == 0 ? "upcoming" : row.getStatus() == 1 ? "ongoing"
                : row.getStatus() == 3 ? "earlyEnded" : row.getStatus() == 4 ? "cancelled" : "ended");
        return rows;
    }

    /**
     * 查询单场考试前补齐时间状态，与列表查询保持一致。
     * @param id 考试主键
     * @return 最新考试或空
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Exam getById(Serializable id) {
        coordinator.lock();
        refreshLocked();
        return baseMapper.selectById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByBoardSn(String sn) {
        coordinator.lock();
        Exam condition = new Exam(); condition.setBoardSn(sn);
        List<Exam> before = baseMapper.getExamList(condition);
        boolean result = baseMapper.removeByBoardSn(sn);
        if (result) {
            coordinator.changed("EXAM_DELETE_SN", detail(before, Collections.emptyList()));
            coordinator.reconcileConflicts();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String batchAdd(List<Exam> exams) { return write(exams, false, true); }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String batchUpdate(List<Exam> exams) { return write(exams, true, true); }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String preview(List<Exam> exams, boolean update) { return write(exams, update, false); }

    /**
     * 验证整批最终结果后逐行保存，影响行数异常抛出异常回滚。
     * @param input 请求批次
     * @param update 是否更新
     * @param persist 是否保存
     * @return 输入或冲突错误，成功返回空
     */
    private String write(List<Exam> input, boolean update, boolean persist) {
        coordinator.lock();
        if (input == null || input.isEmpty() || input.size() > 200) return "考试批次必须包含 1 至 200 条记录";
        Set<Long> ids = new HashSet<>();
        List<Exam> rows = new ArrayList<>();
        List<Exam> existing = baseMapper.selectList(null);
        for (int i = 0; i < input.size(); i++) {
            Exam patch = input.get(i);
            String prefix = "第" + (i + 1) + "条考试：";
            if (patch == null) return prefix + "考试不能为空";
            Exam row = new Exam();
            Exam old = null;
            if (update) {
                if (patch.getId() == null || !ids.add(patch.getId())) return prefix + "考试ID缺失或重复";
                old = existing.stream().filter(e -> patch.getId().equals(e.getId())).findFirst().orElse(null);
                if (old == null) return prefix + "考试不存在";
                if (patch.getRowVersion() == null || !patch.getRowVersion().equals(old.getRowVersion())) return prefix + "考试已被修改，请刷新后重试";
                BeanUtils.copyProperties(old, row);
                if (patch.getClassroomId() != null) row.setClassroomId(patch.getClassroomId());
                else if (patch.getBoardSn() != null && !patch.getBoardSn().equals(old.getBoardSn())) row.setClassroomId(null);
                if (patch.getBoardSn() != null) row.setBoardSn(patch.getBoardSn());
                if (patch.getExamCode() != null) row.setExamCode(patch.getExamCode());
                if (patch.getExamContent() != null) row.setExamContent(patch.getExamContent());
                if (patch.getImageUrl() != null) row.setImageUrl(patch.getImageUrl());
                if (patch.getVideoUrl() != null) row.setVideoUrl(patch.getVideoUrl());
                if (patch.getStartTime() != null) row.setStartTime(patch.getStartTime());
                if (patch.getEndTime() != null) row.setEndTime(patch.getEndTime());
                if (patch.getStatus() != null) row.setStatus(patch.getStatus());
                if (ExamLifecycle.terminal(old.getStatus()) && !ExamLifecycle.terminal(row.getStatus())) row.setActualEndTime(null);
                row.setRowVersion(old.getRowVersion() + 1);
            } else {
                if (patch.getId() != null) return prefix + "新增不能指定考试ID";
                BeanUtils.copyProperties(patch, row);
                row.setRowVersion(0L); row.setActualEndTime(null);
            }
            if (row.getStatus() == null) row.setStatus(0);
            if (row.getStatus() < 0 || row.getStatus() > 4) return prefix + "状态不合法";
            if (row.getExamCode() == null || row.getExamCode().trim().isEmpty() || row.getExamCode().length() > 100) return prefix + "考试号必填且不超过100字符";
            if (row.getExamContent() == null || row.getExamContent().trim().isEmpty() || row.getExamContent().length() > 1000) return prefix + "考试内容必填且不超过1000字符";
            row.setExamCode(row.getExamCode().trim()); row.setExamContent(row.getExamContent().trim());
            if (row.getStartTime() == null || row.getEndTime() == null || !row.getEndTime().isAfter(row.getStartTime())) return prefix + "结束时间必须晚于开始时间";
            LocalDateTime now = LocalDateTime.now(clock);
            if (row.getStatus() == 3 && (old == null || !Integer.valueOf(3).equals(old.getStatus()))) {
                if (old == null || ExamLifecycle.terminal(old.getStatus()) || old.getStartTime() == null || old.getEndTime() == null
                        || now.isBefore(old.getStartTime()) || !now.isBefore(old.getEndTime())
                        || now.isBefore(row.getStartTime()) || !now.isBefore(row.getEndTime())) {
                    return prefix + "只有进行中的考试可以提前结束";
                }
                row.setActualEndTime(now);
            }
            if (row.getStatus() == 2 && (old == null || !Integer.valueOf(2).equals(old.getStatus())) && now.isBefore(row.getEndTime())) {
                return prefix + "考试尚未到结束时间，请选择提前结束或取消安排";
            }
            if (row.getStatus() == 4) row.setActualEndTime(null);
            try {
                Classroom room = coordinator.room(row.getClassroomId(), row.getBoardSn());
                row.setClassroomId(room.getId()); row.setBoardSn(room.getBoardSn());
                coordinator.validateCourseConflict(row);
            } catch (IllegalArgumentException error) { return prefix + error.getMessage(); }
            if (Integer.valueOf(2).equals(row.getStatus()) && row.getActualEndTime() == null) {
                row.setActualEndTime(row.getEndTime());
            }
            rows.add(row);
        }
        for (int i = 0; i < rows.size(); i++) {
            Exam row = rows.get(i);
            if (ExamLifecycle.terminal(row.getStatus())) continue;
            for (Exam old : existing) {
                if (ids.contains(old.getId()) || ExamLifecycle.terminal(old.getStatus())) continue;
                Long roomId = old.getClassroomId();
                if (roomId == null) {
                    try { roomId = coordinator.room(null, old.getBoardSn()).getId(); }
                    catch (IllegalArgumentException error) { return "已有考试教室绑定无法确认，请处理后重试"; }
                }
                if (!roomId.equals(row.getClassroomId())) continue;
                if (old.getStartTime() == null || old.getEndTime() == null) return "已有考试时间无法确认";
                if (conflict(row, old)) return "第" + (i + 1) + "条考试：该教室此时段已有考试 " + old.getExamCode();
            }
            for (int j = 0; j < i; j++) if (!ExamLifecycle.terminal(rows.get(j).getStatus())
                    && row.getClassroomId().equals(rows.get(j).getClassroomId()) && conflict(row, rows.get(j))) {
                return "第" + (j + 1) + "条与第" + (i + 1) + "条考试时间冲突";
            }
        }
        if (!persist) return null;
        List<Exam> before = existing.stream().filter(e -> ids.contains(e.getId())).collect(Collectors.toList());
        for (Exam row : rows) {
            if (!ExamLifecycle.terminal(row.getStatus())) {
                row.setStatus(ExamLifecycle.statusAt(row, LocalDateTime.now(clock)));
                row.setActualEndTime(row.getStatus() == 2 ? row.getEndTime() : null);
            }
        }
        for (Exam row : rows) if ((update ? baseMapper.updateById(row) : baseMapper.insert(row)) != 1) throw new IllegalStateException("考试整批保存失败，已回滚");
        coordinator.changed(update ? "EXAM_UPDATE" : "EXAM_ADD", detail(before, rows));
        coordinator.reconcileConflicts();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshStatuses() {
        coordinator.lock();
        return refreshLocked();
    }

    /**
     * 已持有共同锁时刷新状态；停机跨越多个边界时直接补齐最终状态。
     * @return 实际更新行数
     */
    private int refreshLocked() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Exam> before = new ArrayList<>(), after = new ArrayList<>();
        for (Exam old : baseMapper.selectList(null)) {
            if (old.getStatus() == null || old.getStatus() < 0 || old.getStatus() > 1
                    || old.getStartTime() == null || old.getEndTime() == null || !old.getEndTime().isAfter(old.getStartTime())) continue;
            int target = ExamLifecycle.statusAt(old, now);
            if (target == old.getStatus()) continue;
            Exam row = new Exam(); BeanUtils.copyProperties(old, row);
            row.setStatus(target); row.setRowVersion(old.getRowVersion() + 1);
            row.setActualEndTime(target == 2 ? row.getEndTime() : null);
            if (baseMapper.updateById(row) != 1) throw new IllegalStateException("自动更新考试状态失败，已回滚");
            before.add(old); after.add(row);
        }
        if (!after.isEmpty()) {
            coordinator.changed("EXAM_STATUS_AUTO", detail(before, after), "system");
            coordinator.reconcileConflicts();
        }
        return after.size();
    }

    /**
     * 序列化操作快照，不依赖后续修改或删除后的考试数据。
     * @param before 操作前考试
     * @param after 操作后考试
     * @return 审计 JSON
     */
    private String detail(List<Exam> before, List<Exam> after) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("before", before); snapshot.put("after", after);
        try { return auditJson.writeValueAsString(snapshot); }
        catch (JsonProcessingException error) { throw new IllegalStateException("考试操作快照保存失败", error); }
    }

    /**
     * 比较考试区间。
     * @param a 第一场
     * @param b 第二场
     * @return 是否冲突
     */
    private boolean conflict(Exam a, Exam b) {
        return CoursePeriodResolver.overlaps(new ScheduleInterval(a.getStartTime(), a.getEndTime()),
                new ScheduleInterval(b.getStartTime(), b.getEndTime()));
    }

    /**
     * 按真实考试主键整批删除并更新版本。
     * @param ids 待删除主键
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteExams(List<Long> ids) {
        coordinator.lock();
        if (ids == null || ids.isEmpty() || ids.size() > 200 || ids.contains(null) || new HashSet<>(ids).size() != ids.size()) throw new IllegalArgumentException("删除ID缺失、重复或超限");
        List<Exam> before = baseMapper.selectBatchIds(ids);
        if (before.size() != ids.size()) throw new IllegalArgumentException("考试已被删除，请刷新");
        if (baseMapper.deleteBatchIds(ids) != ids.size()) throw new IllegalStateException("整批删除失败，已回滚");
        coordinator.changed("EXAM_DELETE", detail(before, Collections.emptyList()));
        coordinator.reconcileConflicts();
        return true;
    }
}
