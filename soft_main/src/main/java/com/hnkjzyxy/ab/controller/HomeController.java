package com.hnkjzyxy.ab.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.*;
import com.hnkjzyxy.ab.params.HomeParam;
import com.hnkjzyxy.ab.params.NoticeParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.*;
import com.hnkjzyxy.ab.vo.DataVo;
import com.hnkjzyxy.ab.vo.QueryPage;
import com.hnkjzyxy.ab.vo.RadarChartVo;
import com.hnkjzyxy.ab.vo.TeacherAndDepartmentVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 首页数据管理
 * 提供公告发布与置顶、站内通知、项目结果柱状图与雷达图等首页数据接口
 *
 * @author 16702
 */
@RestController
public class HomeController {

    @Autowired
    private NoticeService noticeService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ResultService resultService;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private MessageService messageService;
    @Resource
    private HomeService homeService;

    /**
     * 分页查询公告列表
     *
     * @param param 分页查询条件
     * @return 公告列表及分页数据
     */
    @GetMapping("/notice/list")
    //@RedisCache(key = HnkjxyConstants.NOTICE_LIST)
    //@Cacheable(value = {HnkjxyConstants.NOTICE_LIST},key = "#param.getPage() + '-' + #param.getLimit()")
    public ApiResult getNoticeList(QueryPage param) {
        Map<String, Object> map = noticeService.getNoticeListByPage(param);
        return ApiResult.ok("data", map);
    }

    /**
     * 获取置顶公告
     *
     * @return 当前置顶的公告信息
     */
    @GetMapping("/notice/top")
    public ApiResult getNoticeTop() {
        Notice isTop = noticeService.getOne(new QueryWrapper<Notice>().eq("is_top", 1));
        return ApiResult.ok("data", isTop);
    }

    /**
     * 发布公告
     *
     * @param param 公告内容（标题、正文、是否置顶等）
     * @return 操作结果
     */
    @PostMapping("/notice/publish")
    @CacheEvict(value = {HnkjxyConstants.NOTICE_LIST}, allEntries = true)
    public ApiResult publishNotice(@Valid @RequestBody NoticeParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        Role role = userRoleMapper.getRoleWeight(user.getUserId());
        if (role.getWeight() <= 5) {
            throw new RuntimeException("没有发布权限！");
        }
        param.setCreateName(user.getNickName());
        noticeService.publishNotice(param);
        return ApiResult.ok("发布公告成功！");
    }

    /**
     * 置顶 / 取消置顶公告
     * 同一时间仅允许一条公告处于置顶状态
     *
     * @param id 公告ID
     * @return 操作结果
     */
    @PostMapping("/notice/isTop/{id}")
    @CacheEvict(value = {HnkjxyConstants.NOTICE_LIST}, allEntries = true)
    public ApiResult noticeTop(@PathVariable("id") String id, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        Role role = userRoleMapper.getRoleWeight(user.getUserId());
        if (role.getWeight() <= 5) {
            throw new RuntimeException("没有置顶权限！");
        }

        if (StrUtil.isBlank(id)) {
            throw new RuntimeException("公告id不能为空！");
        }
        Notice byId = noticeService.getById(id);
        Notice notice = noticeService.getOne(new QueryWrapper<Notice>().eq("is_top", 1));
        if (ObjectUtil.isNotNull(notice)) {
            notice.setIsTop(0);
            noticeService.updateById(notice);
        }
        if (ObjectUtil.isNotNull(byId)) {
            byId.setIsTop(byId.getIsTop().intValue() == 1 ? 0 : 1);
            noticeService.updateById(byId);
        }
        return ApiResult.ok("置顶成功！");
    }

    /**
     * 获取公告详情
     *
     * @param id 公告ID
     * @return 公告详情（同时累加浏览量）
     */
    @GetMapping("/notice/detail/{id}")
    public ApiResult getNoticeDetail(@PathVariable Integer id) {
        if (Objects.isNull(id)) {
            throw new RuntimeException("公告id不能为空！");
        }
        Notice detail = noticeService.getNoticeDetailById(id);
        noticeService.addPageView(id);
        return ApiResult.ok("data", detail);
    }

    /**
     * 获取最近一次项目结束时间
     *
     * @return 当前用户最近结束的项目信息
     */
    @GetMapping("/end/project")
    public ApiResult endProject(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        Project endProject = projectService.getEndProject(user.getUserId());
        return ApiResult.ok("data", endProject);
    }

    /**
     * 获取项目结果柱状图
     * 按身份维度返回项目结果平均值（个人=本人项目，教研室主任=本教研室，院长=全项目）
     *
     * @param param 查询条件（项目ID、年份）
     * @return 柱状图数据
     */
    @GetMapping("/columnar/chart")
    public ApiResult columnarChart(HomeParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        DataVo vo = resultService.columnarChart(param, user);
        return ApiResult.ok("data", vo);
    }

    /**
     * 获取项目结果雷达图
     *
     * @param param 查询条件（项目ID、年份）
     * @return 雷达图维度和数值
     */
    @GetMapping("/radar/chart")
    public ApiResult radarChart(@Valid HomeParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        RadarChartVo vo = homeService.radarChart(param, user);
        return ApiResult.ok("data", vo);
    }

    /**
     * 获取项目年份列表
     *
     * @return 当前用户相关项目的年份列表
     */
    @GetMapping("/project/years/list")
    public ApiResult finishProjectYears(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        List<String> years = projectService.getYearByProject(user);
        return ApiResult.ok("data", years);
    }

    /**
     * 根据年份查询项目列表
     *
     * @param year 年份
     * @return 该年份下的项目列表
     */
    @GetMapping("/year/project/{year}")
    public ApiResult getYearProjectList(@PathVariable("year") String year, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        List<Project> projects = projectService.getProjectByYear(year, user);
        return ApiResult.ok("data", projects);
    }

    /**
     * 获取通知列表
     *
     * @return 当前登录用户的通知列表
     */
    @GetMapping("/notices")
    //@Cacheable(value = {HnkjxyConstants.NOTICES},key = "#auth.getName()")
    public ApiResult getNotices(Authentication auth) {
        User user = userService.getUserByName(auth.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, user.getUserId());
        List<Message> list = messageService.list(wrapper);
        return ApiResult.ok("data", list);
    }

    /**
     * 将当前用户的通知全部标记为已读
     *
     * @return 操作结果
     */
    @PostMapping("/readNotice")
    //@CacheEvict(value = {HnkjxyConstants.NOTICES},allEntries = true)
    public ApiResult readNotice(Authentication auth) {
        User user = userService.getUserByName(auth.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        messageService.readNotice(user.getUserId());
        return ApiResult.ok();
    }


    /**
     * 按老师统计雷达图
     *
     * @param param 查询条件（项目ID、年份）
     * @return 雷达图维度和数值
     */
    @GetMapping("/radar/chart/teacher")
    public ApiResult getRadarByTeacher(@Valid HomeParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        RadarChartVo vo = homeService.getChartByTeacherOrDepartment(param, user);
        return ApiResult.ok("data", vo);
    }


    /**
     * 按教研室统计雷达图
     *
     * @param param 查询条件（项目ID、年份）
     * @return 雷达图维度和数值
     */
    @GetMapping("/radar/chart/department")
    public ApiResult getRadarByDepartment(@Valid HomeParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        RadarChartVo vo = homeService.getChartByTeacherOrDepartment(param, user);
        return ApiResult.ok("data", vo);
    }


    /**
     * 根据项目ID获取参与教师及教研室
     *
     * @param param 查询条件（项目ID）
     * @return 项目参与教师与教研室信息
     */
    @GetMapping("/radar/teacherOrDepartment")
    public ApiResult getTeacherAndDepartmentByProject(@Valid HomeParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        TeacherAndDepartmentVo vo = homeService.getTeacherAndDepartment(param.getProjectId());
        return ApiResult.ok("data", vo);
    }


}