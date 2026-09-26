package com.hnkjzyxy.ab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnkjzyxy.ab.model.Classroom;
import com.hnkjzyxy.ab.model.Exam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ClassroomService;
import com.hnkjzyxy.ab.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试信息管理
 * 提供考试的查询、新增、修改、删除及状态维护接口
 */
@RestController
@RequestMapping("/exam")
public class ExamController {

    @Autowired
    private ExamService examService;
    @Autowired
    private ClassroomService classroomService;

    /**
     * 动态查询考试列表
     *
     * @param exam 考试查询条件（可选参数：examCode、boardSn、status等）
     * @return 考试列表
     */
    @PostMapping("/list")
    public ApiResult list(@RequestBody(required = false) Exam exam) {
        List<Exam> list = examService.getExamList(exam);
        // 判断考试列表是否为空
        if (list.isEmpty()) {
            return ApiResult.error("考场不存在");
        }
        return ApiResult.ok("data", list);
    }

    /**
     * 根据ID查询考试详情
     *
     * @param id 考试ID
     * @return 考试详情
     */
    @GetMapping("/{id}")
    public ApiResult getById(@PathVariable Long id) {
        Exam exam = examService.getById(id);
        if (exam == null) {
            return ApiResult.error("考试不存在");
        }
        return ApiResult.ok("data", exam);
    }

    /**
     * 新增考试
     *
     * @param exam 考试信息
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResult add(@RequestBody Exam exam) {
        if (exam.getBoardSn() == null){
            return ApiResult.error("请选择教室");
        }
        Classroom classroom = new Classroom();
        classroom.setBoardSn(exam.getBoardSn());
        List<Classroom> classroomList = classroomService.getClassroomList(classroom);
        if (classroomList.isEmpty()) {
            return ApiResult.error("教室不存在");
        }
        // 参数校验
        if (exam.getExamCode() == null || exam.getExamCode().isEmpty()) {
            return ApiResult.error("考试号不能为空");
        }
        if (exam.getStartTime() == null) {
            return ApiResult.error("考试开始时间不能为空");
        }
        if (exam.getEndTime() == null) {
            return ApiResult.error("考试结束时间不能为空");
        }
        if (exam.getStartTime().isAfter(exam.getEndTime())) {
            return ApiResult.error("开始时间不能晚于结束时间");
        }

        LocalDateTime startTime1 = exam.getStartTime();
        LocalDateTime endTime1 = exam.getEndTime();
        
        // 查询同一个教室的所有考试，检查时间是否冲突
        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Exam::getBoardSn, exam.getBoardSn());
        List<Exam> list = examService.list(queryWrapper);

        for (Exam exam1 : list) {
            LocalDateTime startTime = exam1.getStartTime();
            LocalDateTime endTime = exam1.getEndTime();

            // 判断两个时间段是否有交集
            boolean hasConflict = !(endTime1.isBefore(startTime) || startTime1.isAfter(endTime));

            if (hasConflict) {
                return ApiResult.error("该教室在此时间段已有考试安排，时间冲突");
            }
        }



        // 设置默认值
        if (exam.getStatus() == null) {
            exam.setStatus(0); // 默认未开始
        }

        boolean result = examService.save(exam);
        if (result) {
            return ApiResult.ok("新增成功");
        } else {
            return ApiResult.error("新增失败");
        }
    }

    /**
     * 更新考试
     *
     * @param exam 考试信息
     * @return 操作结果
     */
    @PutMapping("/update")
    public ApiResult update(@RequestBody Exam exam) {
        if (exam.getBoardSn() == null){
            return ApiResult.error("请选择教室");
        }
        Classroom classroom = new Classroom();
        classroom.setBoardSn(exam.getBoardSn());
        List<Classroom> classroomList = classroomService.getClassroomList(classroom);
        if (classroomList.isEmpty()) {
            return ApiResult.error("教室不存在");
        }
        // 参数校验
        if (exam.getId() == null) {
            return ApiResult.error("考试ID不能为空");
        }

        Exam existExam = examService.getById(exam.getId());
        if (existExam == null) {
            return ApiResult.error("考试不存在");
        }

        // 如果修改了考试时间或教室，需要检查时间是否冲突
        LocalDateTime startTime1 = exam.getStartTime() != null ? exam.getStartTime() : existExam.getStartTime();
        LocalDateTime endTime1 = exam.getEndTime() != null ? exam.getEndTime() : existExam.getEndTime();
        Integer boardSn = exam.getBoardSn() != null ? exam.getBoardSn() : existExam.getBoardSn();

        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Exam::getBoardSn, boardSn)
                .ne(Exam::getId, exam.getId()); // 排除当前考试本身
        List<Exam> list = examService.list(queryWrapper);

        for (Exam exam1 : list) {
            LocalDateTime startTime = exam1.getStartTime();
            LocalDateTime endTime = exam1.getEndTime();

            // 判断两个时间段是否有交集
            boolean hasConflict = !(endTime1.isBefore(startTime) || startTime1.isAfter(endTime));

            if (hasConflict) {
                return ApiResult.error("该教室在此时间段已有考试安排，时间冲突");
            }
        }

        boolean result = examService.updateById(exam);
        if (result) {
            return ApiResult.ok("更新成功");
        } else {
            return ApiResult.error("更新失败");
        }
    }

    /**
     * 删除考试
     *
     * @param id sn
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult delete(@PathVariable Long id) {
        boolean result = examService.removeById(id);
        if (result) {
            return ApiResult.ok("删除成功");
        } else {
            return ApiResult.error("删除失败");
        }
    }

    /**
     * 批量删除考试
     *
     * @param ids 考试ID列表
     * @return 操作结果
     */
    @DeleteMapping("/batchDelete")
    public ApiResult deleteBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ApiResult.error("请选择要删除的考试");
        }

        boolean result = examService.removeByIds(ids);
        if (result) {
            return ApiResult.ok("批量删除成功");
        } else {
            return ApiResult.error("批量删除失败");
        }
    }

    /**
     * 更新考试状态
     *
     * @param id     考试ID
     * @param status 状态 0=未开始 1=进行中 2=已结束
     * @return 操作结果
     */
    @PutMapping("/status/{id}")
    public ApiResult updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null || status < 0 || status > 2) {
            return ApiResult.error("状态参数错误");
        }

        Exam exam = examService.getById(id);
        if (exam == null) {
            return ApiResult.error("考试不存在");
        }

        exam.setStatus(status);

        boolean result = examService.updateById(exam);
        if (result) {
            return ApiResult.ok("状态更新成功");
        } else {
            return ApiResult.error("状态更新失败");
        }
    }
    /**
     * 批量添加考试
     *
     * @param exams 考试信息列表
     * @return 操作结果
     */
    @PostMapping("/batchAdd")
    public ApiResult batchAdd(@RequestBody List<Exam> exams) {
        if (exams == null || exams.isEmpty()) {
            return ApiResult.error("考试列表不能为空");
        }

        String errorMsg = examService.batchAdd(exams);
        if (errorMsg != null) {
            return ApiResult.error(errorMsg);
        }
        return ApiResult.ok("批量添加考试成功");
    }

    /**
     * 批量更新考试
     *
     * @param exams 考试信息列表（必须包含id）
     * @return 操作结果
     */
    @PutMapping("/batchUpdate")
    public ApiResult batchUpdate(@RequestBody List<Exam> exams) {
        if (exams == null || exams.isEmpty()) {
            return ApiResult.error("考试列表不能为空");
        }

        String errorMsg = examService.batchUpdate(exams);
        if (errorMsg != null) {
            return ApiResult.error(errorMsg);
        }
        return ApiResult.ok("批量更新考试成功");
    }
}
