package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.model.Exam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 考试信息管理
 * 提供考试的查询、新增、修改、删除及状态维护接口
 */
@RestController
@RequestMapping("/exam")
@PreAuthorize("hasRole('admin')")
public class ExamController {

    /**
     * 考试安排业务服务
     */
    @Autowired
    private ExamService examService;

    /**
     * 排考预览，实际提交仍在新事务中重新验证。
     * @param exams 考试批次
     * @param update 是否更新
     * @return 最新冲突提示
     */
    @PostMapping("/preview")
    @PreAuthorize("hasRole('admin')")
    public ApiResult preview(@RequestBody List<Exam> exams, @RequestParam(defaultValue = "false") boolean update) {
        String error = examService.preview(exams, update);
        return error == null ? ApiResult.ok("目标教室及整个时段校验通过") : ApiResult.error(error);
    }

    /**
     * 动态查询考试列表
     *
     * @param exam 考试查询条件（可选参数：examCode、boardSn、status等）
     * @return 考试列表
     */
    @PostMapping("/list")
    public ApiResult list(@RequestBody(required = false) Exam exam) {
        List<Exam> list = examService.getExamList(exam);
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
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/add")
    public ApiResult add(@RequestBody Exam exam) {
        String error = examService.batchAdd(Collections.singletonList(exam));
        return error == null ? ApiResult.ok("新增成功") : ApiResult.error(error);
    }

    /**
     * 更新考试
     *
     * @param exam 考试信息
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
    @PutMapping("/update")
    public ApiResult update(@RequestBody Exam exam) {
        String error = examService.batchUpdate(Collections.singletonList(exam));
        return error == null ? ApiResult.ok("更新成功") : ApiResult.error(error);
    }

    /**
     * 删除考试
     *
     * @param id sn
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public ApiResult delete(@PathVariable Long id) {
        boolean result = examService.deleteExams(Collections.singletonList(id));
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
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/batchDelete")
    public ApiResult deleteBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ApiResult.error("请选择要删除的考试");
        }

        boolean result = examService.deleteExams(ids);
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
     * @param status 生命周期，0=未开始，1=进行中，2=已结束，3=提前结束，4=取消安排
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
    @PutMapping("/status/{id}")
    public ApiResult updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return ApiResult.error("请通过更新接口提交状态及行版本，重新启用必须重新验证课表");
    }

    /**
     * 批量添加考试
     *
     * @param exams 考试信息列表
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
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
    @PreAuthorize("hasRole('admin')")
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
