package com.hnkjzyxy.ab.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.util.StringUtils;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.model.*;
import com.hnkjzyxy.ab.params.*;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.FlowService;
import com.hnkjzyxy.ab.service.MessageService;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.vo.FlowQueryVo;
import com.hnkjzyxy.ab.vo.FlowStatusVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 审批流程管理
 * 提供流程维护、项目审批、结果查看与提交等接口
 *
 * @author 16702
 */
@RestController
public class FlowController {

    @Autowired
    private FlowService flowService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private MessageService messageService;

    /**
     * 获取流程年份列表
     *
     * @return 当前用户相关流程的年份列表
     */
    @GetMapping("/flow/years")
    public ApiResult getFlowYears(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        List<String> list = flowService.getFlowYears(user.getUserId());
        return ApiResult.ok("data", list);
    }

    /**
     * 分页查询流程列表
     *
     * @param param 流程查询条件
     * @return 流程列表及分页数据
     */
    @GetMapping("/flow/list")
    public ApiResult getFlowList(FlowParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        param.setUserId(user.getUserId());
        Map<String, Object> flowList = flowService.getFlowList(param);
        return ApiResult.ok("data", flowList);
    }

    /**
     * 创建或修改流程
     * 传入 id 时为修改，否则为新增
     *
     * @param flow 流程信息
     * @return 操作结果
     */
    @PostMapping("/create/flow")
    public ApiResult createFlow(@Valid @RequestBody Flow flow, Authentication authentication) {
        Integer id = flow.getId();
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        flow.setUserId(user.getUserId());
        flowService.createFlow(flow, user);
        return ApiResult.ok(id != null ? "修改成功！" : "创建流程成功！");
    }

    /**
     * 获取项目流程信息
     *
     * @param pId 项目ID
     * @return 项目流程详情
     */
    @GetMapping("/findFlow/{pId}")
    public ApiResult findFlowByPId(@PathVariable String pId) {
        if (StrUtil.isEmpty(pId)) {
            throw new RuntimeException("项目id不能为空！");
        }
        FlowQueryVo flow = flowService.findFlowByPId(pId);
        return ApiResult.ok("data", flow);
    }

    /**
     * 获取待审批项目年份列表
     *
     * @return 待审批项目的年份列表
     */
    @GetMapping("/project/approve/years")
    public ApiResult getProjectApproveYears(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        List<String> years = flowService.getProjectApproveYears(user);
        return ApiResult.ok("data", years);
    }

    /**
     * 获取项目审批列表
     *
     * @param param 项目审批查询条件
     * @return 项目审批列表及分页数据
     */
    @GetMapping("/project/approve")
    public ApiResult getProjectApproveList(ProjectParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        return flowService.getProjectApproveList(param, user);
    }

    /**
     * 获取项目提交结果详情
     *
     * @param id 项目ID
     * @return 项目提交结果详情
     */
    @GetMapping("/result/detail/{pId}")
    public ApiResult getAppRoveDetail(@PathVariable("pId") String id, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (StringUtils.isEmpty(id)) {
            throw new RuntimeException("项目id不能为空！");
        }
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        return flowService.getProjectResultDetail(Integer.valueOf(id), user.getUserId());
    }

    /**
     * 获取项目提交结果列表
     *
     * @param param 结果查询条件
     * @return 项目提交结果列表及分页数据
     */
    @GetMapping("/project/result")
    public ApiResult getAppRoveDetail(@Valid ApproveParam param, Authentication authentication) throws Exception {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        Map<String, Object> map = flowService.getResultOutcomeList(param, user);
        return ApiResult.ok("data", map);
    }

    /**
     * 获取当前项目未提交人员名单
     *
     * @param param 查询条件（项目ID）
     * @return 未提交人员名单
     */
    @GetMapping("/NotSubmitted/list")
    public ApiResult getNotSubmittedList(@Valid UserParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        Map<String, Object> userVos = flowService.getNotSubmittedList(param, user);
        return ApiResult.ok("data", userVos);
    }

    /**
     * 提交审批结果
     *
     * @param result 审批结果信息
     * @return 操作结果
     */
    @PostMapping("/submit/approve")
    public ApiResult submitApprove(@Valid @RequestBody ResultItem result, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        flowService.submitApprove(result, user);
        return ApiResult.ok("审批成功！");
    }

    /**
     * 审批结果列表查询
     *
     * @param param 审批查询条件（项目ID、用户ID）
     * @return 审批结果详情
     */
    @GetMapping("/approval/result")
    public ApiResult approvalResult(@Valid ApproveParam param) {
        if (ObjectUtil.isNull(param.getPId())) {
            throw new RuntimeException("项目id不能为空！");
        }
        if (ObjectUtil.isNull(param.getUId())) {
            throw new RuntimeException("用户id不能为空！");
        }
        return flowService.getAppRoveResultDetail(param);
    }

    /**
     * 查看项目流程状态
     *
     * @param pId 项目ID
     * @return 项目流程状态信息
     */
    @GetMapping("/project/flow/{pId}")
    public ApiResult projectFlow(@PathVariable("pId") String pId, Authentication authentication) {
        if (StringUtils.isEmpty(pId)) {
            throw new RuntimeException("项目id不能为空！");
        }
        User user = userService.getUserByName(authentication.getName());
        FlowStatusVo projectFLow = flowService.getProjectFLow(pId, user.getUserId());
        return ApiResult.ok("data", projectFLow);
    }

    /**
     * 提醒指定用户查看项目
     *
     * @param param 提醒参数（项目ID、接收用户ID）
     * @return 操作结果
     */
    @PostMapping("/project/notice")
    @CacheEvict(value = {HnkjxyConstants.NOTICES}, allEntries = true)
    public ApiResult noticeProjectByUId(@Valid @RequestBody NoticeQueryParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        Project project = projectService.getById(param.getPId());
        Message build = Message.builder()
                .title("提醒通知")
                .userId(param.getUId())
                .content("项目名称：" + project.getTitle())
                .createName(user.getNickName())
                .build();
        messageService.save(build);
        return ApiResult.ok("提醒成功！");
    }


}