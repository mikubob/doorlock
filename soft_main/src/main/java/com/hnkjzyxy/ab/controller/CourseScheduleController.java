package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.model.CourseSchedule;
import com.hnkjzyxy.ab.model.CourseScheduleSyncResult;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.CourseScheduleService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;


import java.util.List;



/**
 * 课程安排管理
 */
@RestController
@RequestMapping("/courseSchedule")
//@PreAuthorize("permitAll()") // 放行整个 Controller，无需认证
public class CourseScheduleController {

    @Autowired
    private CourseScheduleService courseScheduleService;

    /**
     * 刷新课程安排数据（从 OA 拉取并整体替换课表）
     * <p>
     * 同步失败或未执行时不会改动现有课表数据，返回体中的 data 字段包含本次同步明细。
     * </p>
     *
     * @return 同步结果，data 为本次同步明细
     */
    @GetMapping("/refresh")
    public ApiResult refresh() {
        CourseScheduleSyncResult result = courseScheduleService.sync("manual");
        if (result.isSuccess()) {
            return ApiResult.ok("刷新成功").put("data", result);
        }
        String message = result.getMessage() == null ? "刷新失败" : result.getMessage();
        return ApiResult.error(message);
    }

    /**
     * 查询课程列表（支持条件查询）- GET方式
     */
/*
    @GetMapping("/list")
    public ApiResult listGet(CourseSchedule courseSchedule) {
        List<CourseSchedule> list = courseScheduleService.getList(courseSchedule);
        return ApiResult.ok("data", list);
    }
*/

    /**
     * 查询课程列表（支持条件查询）- POST方式
     */
    @PostMapping("/list")
    public ApiResult list(@RequestBody(required = false) CourseSchedule courseSchedule) {
        List<CourseSchedule> list = courseScheduleService.getList(courseSchedule);
        return ApiResult.ok("data", list);
    }

    /**
     * 根据ID查询课程详情
     * 注意：此通配符路由必须放在具体路由之后，避免匹配到 "list" 等字符串
     */
    @GetMapping("/{id}")
    public ApiResult getById(@PathVariable Integer id) {
        CourseSchedule courseSchedule = courseScheduleService.getById(id);
        if (courseSchedule == null) {
            return ApiResult.error("课程不存在");
        }
        return ApiResult.ok("data", courseSchedule);
    }

    /**
     * 新增课程
     */
    @PostMapping("/add")
    public ApiResult add(@RequestBody CourseSchedule courseSchedule) {
        // 参数校验
        if (courseSchedule.getCourseName() == null || courseSchedule.getCourseName().isEmpty()) {
            return ApiResult.error("课程名称不能为空");
        }
        if (courseSchedule.getAcademicYear() == null || courseSchedule.getAcademicYear().isEmpty()) {
            return ApiResult.error("学年不能为空");
        }
        if (courseSchedule.getSemester() == null || courseSchedule.getSemester().isEmpty()) {
            return ApiResult.error("学期不能为空");
        }
        if (courseSchedule.getClassName() == null || courseSchedule.getClassName().isEmpty()) {
            return ApiResult.error("班级名称不能为空");
        }
        if (courseSchedule.getClassDate() == null || courseSchedule.getClassDate().isEmpty()) {
            return ApiResult.error("上课日期不能为空");
        }

        boolean result = courseScheduleService.saveCourseSchedule(courseSchedule);
        if (result) {
            return ApiResult.ok("新增成功");
        } else {
            return ApiResult.error("新增失败");
        }
    }

    /**
     * 更新课程
     */
    @PutMapping("/update")
    public ApiResult update(@RequestBody CourseSchedule courseSchedule) {
        if (courseSchedule.getId() == null) {
            return ApiResult.error("ID不能为空");
        }

        CourseSchedule existCourse = courseScheduleService.getById(courseSchedule.getId());
        if (existCourse == null) {
            return ApiResult.error("课程不存在");
        }

        boolean result = courseScheduleService.updateCourseSchedule(courseSchedule);
        if (result) {
            return ApiResult.ok("更新成功");
        } else {
            return ApiResult.error("更新失败");
        }
    }

    /**
     * 删除课程
     */
    @DeleteMapping("/delete/{id}")
    public ApiResult delete(@PathVariable Integer id) {
        CourseSchedule existCourse = courseScheduleService.getById(id);
        if (existCourse == null) {
            return ApiResult.error("课程不存在");
        }

        boolean result = courseScheduleService.deleteById(id);
        if (result) {
            return ApiResult.ok("删除成功");
        } else {
            return ApiResult.error("删除失败");
        }
    }

    /**
     * 批量删除课程
     */
    @DeleteMapping("/deleteBatch")
    public ApiResult deleteBatch(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return ApiResult.error("请选择要删除的课程");
        }

        boolean result = courseScheduleService.deleteBatch(ids);
        if (result) {
            return ApiResult.ok("删除成功");
        } else {
            return ApiResult.error("删除失败");
        }
    }

}
