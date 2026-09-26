package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.ExamMapper;
import com.hnkjzyxy.ab.model.Classroom;
import com.hnkjzyxy.ab.model.Exam;
import com.hnkjzyxy.ab.service.ClassroomService;
import com.hnkjzyxy.ab.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 考试信息Service实现类
 */
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ClassroomService classroomService;

    @Override
    public List<Exam> getExamList(Exam exam) {
        return examMapper.getExamList(exam);
    }

    @Override
    public boolean removeByBoardSn(Long boardSn) {
        return examMapper.removeByBoardSn(boardSn);
    }

    /**
     * 批量添加考试
     *
     * @param exams 考试列表
     * @return 操作结果，成功返回null，失败返回错误信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String batchAdd(List<Exam> exams) {
        // 逐条校验教室、考试号、时间逻辑
        for (int i = 0; i < exams.size(); i++) {
            Exam exam = exams.get(i);
            int index = i + 1;

            // 校验教室SN码
            if (exam.getBoardSn() == null) {
                return "第" + index + "条考试：请选择教室";
            }
            // 校验教室是否存在
            Classroom classroom = new Classroom();
            classroom.setBoardSn(exam.getBoardSn());
            List<Classroom> classroomList = classroomService.getClassroomList(classroom);
            if (classroomList == null || classroomList.isEmpty()) {
                return "第" + index + "条考试：教室不存在";
            }
            // 校验考试号
            if (exam.getExamCode() == null || exam.getExamCode().isEmpty()) {
                return "第" + index + "条考试：考试号不能为空";
            }
            // 校验考试时间
            if (exam.getStartTime() == null) {
                return "第" + index + "条考试：考试开始时间不能为空";
            }
            if (exam.getEndTime() == null) {
                return "第" + index + "条考试：考试结束时间不能为空";
            }
            if (exam.getStartTime().isAfter(exam.getEndTime())) {
                return "第" + index + "条考试：开始时间不能晚于结束时间";
            }

            // 设置默认值
            if (exam.getStatus() == null) {
                exam.setStatus(0);
            }
        }

        // 检查与数据库已有考试的时间冲突
        for (int i = 0; i < exams.size(); i++) {
            Exam exam = exams.get(i);
            int index = i + 1;

            LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Exam::getBoardSn, exam.getBoardSn());
            List<Exam> existExams = this.list(queryWrapper);

            for (Exam existExam : existExams) {
                boolean hasConflict = !(exam.getEndTime().isBefore(existExam.getStartTime())
                        || exam.getStartTime().isAfter(existExam.getEndTime()));
                if (hasConflict) {
                    return "第" + index + "条考试：该教室在此时间段已有考试安排，时间冲突";
                }
            }
        }

        // 检查当前批次内考试之间的时间冲突
        for (int i = 0; i < exams.size(); i++) {
            Exam exam1 = exams.get(i);
            for (int j = i + 1; j < exams.size(); j++) {
                Exam exam2 = exams.get(j);
                // 不同教室无需比较
                if (!exam1.getBoardSn().equals(exam2.getBoardSn())) {
                    continue;
                }
                boolean hasConflict = !(exam1.getEndTime().isBefore(exam2.getStartTime())
                        || exam1.getStartTime().isAfter(exam2.getEndTime()));
                if (hasConflict) {
                    return "第" + (i + 1) + "条考试与第" + (j + 1) + "条考试时间冲突";
                }
            }
        }

        // 所有验证通过后批量插入
        boolean result = this.saveBatch(exams);
        return result ? null : "批量添加失败";
    }

    /**
     * 批量更新考试
     *
     * @param exams 考试列表（必须包含id）
     * @return 操作结果，成功返回null，失败返回错误信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String batchUpdate(List<Exam> exams) {
        // 合并原始数据，支持部分字段更新
        for (int i = 0; i < exams.size(); i++) {
            Exam exam = exams.get(i);
            int index = i + 1;

            // 校验考试ID
            if (exam.getId() == null) {
                return "第" + index + "条考试：考试ID不能为空";
            }
            // 校验考试是否存在
            Exam existExam = this.getById(exam.getId());
            if (existExam == null) {
                return "第" + index + "条考试：考试不存在";
            }

            // 合并原始数据和更新数据
            if (exam.getBoardSn() != null) {
                existExam.setBoardSn(exam.getBoardSn());
            }
            if (exam.getExamCode() != null) {
                existExam.setExamCode(exam.getExamCode());
            }
            if (exam.getExamContent() != null) {
                existExam.setExamContent(exam.getExamContent());
            }
            if (exam.getImageUrl() != null) {
                existExam.setImageUrl(exam.getImageUrl());
            }
            if (exam.getVideoUrl() != null) {
                existExam.setVideoUrl(exam.getVideoUrl());
            }
            if (exam.getStartTime() != null) {
                existExam.setStartTime(exam.getStartTime());
            }
            if (exam.getEndTime() != null) {
                existExam.setEndTime(exam.getEndTime());
            }
            if (exam.getStatus() != null) {
                existExam.setStatus(exam.getStatus());
            }

            // 校验合并后的教室SN码
            if (existExam.getBoardSn() == null) {
                return "第" + index + "条考试：请选择教室";
            }
            // 校验教室是否存在
            Classroom classroom = new Classroom();
            classroom.setBoardSn(existExam.getBoardSn());
            List<Classroom> classroomList = classroomService.getClassroomList(classroom);
            if (classroomList == null || classroomList.isEmpty()) {
                return "第" + index + "条考试：教室不存在";
            }
            // 校验考试号
            if (existExam.getExamCode() == null || existExam.getExamCode().isEmpty()) {
                return "第" + index + "条考试：考试号不能为空";
            }
            // 校验考试时间
            if (existExam.getStartTime() == null) {
                return "第" + index + "条考试：考试开始时间不能为空";
            }
            if (existExam.getEndTime() == null) {
                return "第" + index + "条考试：考试结束时间不能为空";
            }
            if (existExam.getStartTime().isAfter(existExam.getEndTime())) {
                return "第" + index + "条考试：开始时间不能晚于结束时间";
            }

            exams.set(i, existExam);
        }

        // 检查与数据库中已有考试的时间冲突（排除自身）
        for (int i = 0; i < exams.size(); i++) {
            Exam exam = exams.get(i);
            int index = i + 1;

            LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Exam::getBoardSn, exam.getBoardSn())
                    .ne(Exam::getId, exam.getId());
            List<Exam> existExams = this.list(queryWrapper);

            for (Exam existExam : existExams) {
                boolean hasConflict = !(exam.getEndTime().isBefore(existExam.getStartTime())
                        || exam.getStartTime().isAfter(existExam.getEndTime()));
                if (hasConflict) {
                    return "第" + index + "条考试：该教室在此时间段已有考试安排，时间冲突";
                }
            }
        }

        // 检查当前批次内考试之间的时间冲突
        for (int i = 0; i < exams.size(); i++) {
            Exam exam1 = exams.get(i);
            for (int j = i + 1; j < exams.size(); j++) {
                Exam exam2 = exams.get(j);
                // 不同教室无需比较
                if (!exam1.getBoardSn().equals(exam2.getBoardSn())) {
                    continue;
                }
                // 同一条记录无需比较
                if (exam1.getId().equals(exam2.getId())) {
                    continue;
                }
                boolean hasConflict = !(exam1.getEndTime().isBefore(exam2.getStartTime())
                        || exam1.getStartTime().isAfter(exam2.getEndTime()));
                if (hasConflict) {
                    return "第" + (i + 1) + "条考试与第" + (j + 1) + "条考试时间冲突";
                }
            }
        }

        // 所有验证通过后批量更新
        boolean result = this.updateBatchById(exams);
        return result ? null : "批量更新失败";
    }
}
