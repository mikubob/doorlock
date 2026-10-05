package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.ExamMapper;
import com.hnkjzyxy.ab.model.*;
import com.hnkjzyxy.ab.service.*;
import com.hnkjzyxy.ab.vo.ScheduleInterval;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

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
     * {@inheritDoc}
     */
    @Override
    public List<Exam> getExamList(Exam condition) {
        List<Exam> rows = baseMapper.getExamList(condition == null ? new Exam() : condition);
        LocalDateTime now = LocalDateTime.now(CoursePeriodResolver.ZONE);
        for (Exam row : rows) row.setTimeStatus(Integer.valueOf(2).equals(row.getStatus()) ? "ended"
                : row.getStartTime() == null || row.getEndTime() == null ? "unknown"
                : now.isBefore(row.getStartTime()) ? "upcoming" : now.isBefore(row.getEndTime()) ? "ongoing" : "ended");
        return rows;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByBoardSn(String sn) {
        coordinator.lock();
        boolean result = baseMapper.removeByBoardSn(sn);
        if (result) coordinator.changed("EXAM_DELETE_SN", sn);
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
            if (update) {
                if (patch.getId() == null || !ids.add(patch.getId())) return prefix + "考试ID缺失或重复";
                Exam old = existing.stream().filter(e -> patch.getId().equals(e.getId())).findFirst().orElse(null);
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
                if (Integer.valueOf(2).equals(old.getStatus()) && !Integer.valueOf(2).equals(row.getStatus())) row.setActualEndTime(null);
                row.setRowVersion(old.getRowVersion() + 1);
            } else {
                if (patch.getId() != null) return prefix + "新增不能指定考试ID";
                BeanUtils.copyProperties(patch, row);
                row.setRowVersion(0L); row.setActualEndTime(null);
            }
            if (row.getStatus() == null) row.setStatus(0);
            if (row.getStatus() < 0 || row.getStatus() > 2) return prefix + "状态不合法";
            if (row.getExamCode() == null || row.getExamCode().trim().isEmpty() || row.getExamCode().length() > 100) return prefix + "考试号必填且不超过100字符";
            if (row.getExamContent() == null || row.getExamContent().trim().isEmpty() || row.getExamContent().length() > 1000) return prefix + "考试内容必填且不超过1000字符";
            row.setExamCode(row.getExamCode().trim()); row.setExamContent(row.getExamContent().trim());
            if (row.getStartTime() == null || row.getEndTime() == null || !row.getEndTime().isAfter(row.getStartTime())) return prefix + "结束时间必须晚于开始时间";
            try {
                Classroom room = coordinator.room(row.getClassroomId(), row.getBoardSn());
                row.setClassroomId(room.getId()); row.setBoardSn(room.getBoardSn());
                coordinator.validateCourseConflict(row);
            } catch (IllegalArgumentException error) { return prefix + error.getMessage(); }
            if (Integer.valueOf(2).equals(row.getStatus()) && row.getActualEndTime() == null) {
                LocalDateTime now = LocalDateTime.now(CoursePeriodResolver.ZONE);
                row.setActualEndTime(now.isBefore(row.getEndTime()) ? now : row.getEndTime());
            }
            rows.add(row);
        }
        for (int i = 0; i < rows.size(); i++) {
            Exam row = rows.get(i);
            if (Integer.valueOf(2).equals(row.getStatus())) continue;
            for (Exam old : existing) {
                if (ids.contains(old.getId()) || Integer.valueOf(2).equals(old.getStatus())) continue;
                Long roomId = old.getClassroomId();
                if (roomId == null) {
                    try { roomId = coordinator.room(null, old.getBoardSn()).getId(); }
                    catch (IllegalArgumentException error) { return "已有考试教室绑定无法确认，请处理后重试"; }
                }
                if (!roomId.equals(row.getClassroomId())) continue;
                if (old.getStartTime() == null || old.getEndTime() == null) return "已有考试时间无法确认";
                if (conflict(row, old)) return "第" + (i + 1) + "条考试：该教室此时段已有考试 " + old.getExamCode();
            }
            for (int j = 0; j < i; j++) if (!Integer.valueOf(2).equals(rows.get(j).getStatus())
                    && row.getClassroomId().equals(rows.get(j).getClassroomId()) && conflict(row, rows.get(j))) {
                return "第" + (j + 1) + "条与第" + (i + 1) + "条考试时间冲突";
            }
        }
        if (!persist) return null;
        String before = existing.stream().filter(e -> ids.contains(e.getId())).collect(java.util.stream.Collectors.toList()).toString();
        for (Exam row : rows) if ((update ? baseMapper.updateById(row) : baseMapper.insert(row)) != 1) throw new IllegalStateException("考试整批保存失败，已回滚");
        coordinator.changed(update ? "EXAM_UPDATE" : "EXAM_ADD", "before=" + before + "; after=" + rows);
        coordinator.reconcileConflicts();
        return null;
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
        coordinator.changed("EXAM_DELETE", before.toString());
        coordinator.reconcileConflicts();
        return true;
    }
}
