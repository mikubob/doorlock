package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.dto.CourseSourceRebindDto;
import com.hnkjzyxy.ab.exception.AuthPermissionException;
import com.hnkjzyxy.ab.model.CourseSchedule;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.CourseScheduleService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.vo.CourseScheduleSyncResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程安排管理
 */
@RestController
@RequestMapping("/courseSchedule")
@PreAuthorize("hasRole('admin')")
public class CourseScheduleController {

    /**
     * 独立确认调整记录的新来源或明确转为本地课程。
     *
     * @param request 稳定身份、行版本及确认依据
     * @return 复核结果
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/rebindSource")
    public ApiResult rebindSource(@RequestBody CourseSourceRebindDto request) {
        return courseScheduleService.rebindSource(request) ? ApiResult.ok("来源已独立复核") : ApiResult.error("复核失败");
    }

    /**
     * 课表业务服务
     */
    @Autowired
    private CourseScheduleService courseScheduleService;
    /**
     * 认证用户所属学院。
     */
    @Autowired
    private UserService userService;

    /**
     * 刷新课程安排数据（从 OA 拉取并整体替换课表）
     * <p>
     * 同步失败或未执行时不会改动现有课表数据，返回体中的 data 字段包含本次同步明细。
     * </p>
     *
     * @return 同步结果，data 为本次同步明细
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/refresh")
    public ApiResult refresh() {
        CourseScheduleSyncResult result = courseScheduleService.sync("manual");
        if (result.isSuccess()) {
            return ApiResult.ok("刷新成功").put("data", result);
        }
        String message = result.getMessage() == null ? "刷新失败" : result.getMessage();
        return ApiResult.error(message).put("data", result);
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
     *
     * @param courseSchedule 课表信息
     * @return 统一接口响应
     */
    @PostMapping("/list")
    @PreAuthorize("isAuthenticated() and !hasRole('LOCK_ONLY')")
    public ApiResult list(@RequestBody(required = false) CourseSchedule courseSchedule, Authentication authentication) {
        if (courseSchedule == null) courseSchedule = new CourseSchedule();
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_admin".equals(a.getAuthority()));
        if (!admin) {
            User user = userService.getUserByName(authentication.getName());
            if (user == null || user.getCollege() == null) throw new AuthPermissionException(403, "用户学院范围未确认");
            courseSchedule.setDepartmentName(user.getCollege());
        }
        List<CourseSchedule> list = courseScheduleService.getList(courseSchedule);
        return ApiResult.ok("data", list);
    }

    /**
     * 根据ID查询课程详情
     * 注意：此通配符路由必须放在具体路由之后，避免匹配到 "list" 等字符串
     *
     * @param id 课表ID
     * @return 统一接口响应
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
     *
     * @param courseSchedule 课表信息
     * @return 统一接口响应
     */
    @PreAuthorize("hasRole('admin')")
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
     *
     * @param courseSchedule 课表信息
     * @return 统一接口响应
     */
    @PreAuthorize("hasRole('admin')")
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
     *
     * @param id 课表ID
     * @param rowVersion 最新行版本
     * @param reason 独立停课理由
     * @return 统一接口响应
     */
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/delete/{id}")
    public ApiResult delete(@PathVariable Integer id,
            @RequestParam Long rowVersion,
            @RequestParam String reason) {
        CourseSchedule patch = new CourseSchedule(); patch.setId(id); patch.setRowVersion(rowVersion);
        patch.setEffective(0); patch.setChangeReason(reason);
        return courseScheduleService.updateCourseSchedule(patch) ? ApiResult.ok("独立停课成功") : ApiResult.error("停课失败");
    }

    /**
     * 批量删除课程
     *
     * @param ids 课表ID集合
     * @return 统一接口响应
     */
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/deleteBatch")
    public ApiResult deleteBatch(@RequestBody List<Integer> ids) {
        return ApiResult.error("请通过独立调整接口逐条提交课程身份、行版本及办理理由，不支持无版本批量停课");
    }

}
