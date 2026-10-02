package com.hnkjzyxy.ab.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.enums.HnkjzyEncode;
import com.hnkjzyxy.ab.annotation.RedisCache;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.mapper.FlowMapper;
import com.hnkjzyxy.ab.mapper.FlowTaskMapper;
import com.hnkjzyxy.ab.mapper.ProjectMapper;
import com.hnkjzyxy.ab.mapper.ResultItemMapper;
import com.hnkjzyxy.ab.mapper.ResultMapper;
import com.hnkjzyxy.ab.mapper.RoleMapper;
import com.hnkjzyxy.ab.mapper.UserMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.Flow;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.ResultExtend;
import com.hnkjzyxy.ab.model.ResultItem;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ApproveParam;
import com.hnkjzyxy.ab.params.FlowParam;
import com.hnkjzyxy.ab.params.ProjectParam;
import com.hnkjzyxy.ab.params.UserParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.FlowService;
import com.hnkjzyxy.ab.service.FlowTaskService;
import com.hnkjzyxy.ab.service.NoticeService;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.ResultExtendService;
import com.hnkjzyxy.ab.service.ResultItemService;
import com.hnkjzyxy.ab.service.ResultService;
import com.hnkjzyxy.ab.service.TaskService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.utils.PageUtils;
import com.hnkjzyxy.ab.vo.ApproveVo;
import com.hnkjzyxy.ab.vo.FlowQueryTaskVo;
import com.hnkjzyxy.ab.vo.FlowQueryVo;
import com.hnkjzyxy.ab.vo.FlowStatus;
import com.hnkjzyxy.ab.vo.FlowStatusVo;
import com.hnkjzyxy.ab.vo.FlowVo;
import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * 审批流程Service实现类
 *
 * @author 16702
 */
@Service
public class FlowServiceImpl extends ServiceImpl<FlowMapper, Flow> implements FlowService {

    /**
     * 审批写入共用的项目锁及任务归属保护服务
     */
    @Resource
    private ProjectTaskGuard projectTaskGuard;

    /**
     * 审批流程数据访问接口
     */
    @Resource
    private FlowMapper flowMapper;
    /**
     * 流程节点数据访问接口
     */
    @Resource
    private FlowTaskMapper flowTaskMapper;
    /**
     * 用户角色关联数据访问接口
     */
    @Resource
    private UserRoleMapper userRoleMapper;
    /**
     * 考核结果数据访问接口
     */
    @Resource
    private ResultMapper resultMapper;
    /**
     * 考核项目数据访问接口
     */
    @Resource
    private ProjectMapper projectMapper;
    /**
     * 用户数据访问接口
     */
    @Resource
    private UserMapper userMapper;
    /**
     * 审批明细数据访问接口
     */
    @Resource
    private ResultItemMapper resultItemMapper;
    /**
     * 流程节点业务服务
     */
    @Resource
    private FlowTaskService flowTaskService;
    /**
     * 考核项目业务服务
     */
    @Autowired
    private ProjectService projectService;
    /**
     * 角色数据访问接口
     */
    @Resource
    private RoleMapper roleMapper;
    /**
     * 考核结果业务服务
     */
    @Autowired
    private ResultService resultService;
    /**
     * 用户业务服务
     */
    @Resource
    private UserService userService;
    /**
     * TaskService业务服务
     */
    @Resource
    private TaskService taskService;
    /**
     * 审批明细业务服务
     */
    @Resource
    private ResultItemService resultItemService;
    /**
     * 结果扩展项业务服务
     */
    @Resource
    private ResultExtendService resultExtendService;
    /**
     * 通知公告业务服务
     */
    @Resource
    private NoticeService noticeService;


    //获取流程列表
    /**
     * {@inheritDoc}
     */
    @Override
    //@RedisCache(key = HnkjxyConstants.FLOW_LIST)
    //@Cacheable(value = {HnkjxyConstants.FLOW_LIST},key = "#param.getUserId()+'-'+#param.getYear()+'-'+#param.getPage()+'-'+#param.getLimit()+'-'+#param.getFlowName()",sync = true)
    public Map<String, Object> getFlowList(FlowParam param) {
        Page<Flow> page = new Page<>(param.getPage(), param.getLimit());
        LambdaQueryWrapper<Flow> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(param.getFlowName())) {
            wrapper.like(Flow::getFlowName, param.getFlowName());
        }
        if (StrUtil.isNotBlank(param.getYear())) {
            wrapper.ge(Flow::getCreateTime, param.getYear().concat("-01-01 00:00:00"));
            wrapper.le(Flow::getCreateTime, param.getYear().concat("-12-31 23:59:59"));
        }
        Role role = userRoleMapper.getRoleWeight(param.getUserId());
        wrapper.eq(Flow::getStatus, 1);
        if (role.getWeight().intValue() < 100) {
            wrapper.eq(Flow::getUserId, param.getUserId());
        }
        wrapper.orderByDesc(Flow::getId);
        this.page(page, wrapper);
        List<Flow> flowList = page.getRecords();
        List<FlowVo> flowVos = flowList.stream().map(item -> {
            Project project = projectService.getById(item.getPId());
            if (project.getStatus().equals(3)) {
                return null;
            }
            FlowVo vo = new FlowVo();
            BeanUtil.copyProperties(item, vo);
            vo.setTitle(project.getTitle());
            vo.setStatus(project.getStatus());
            return vo;
        }).filter(ObjectUtil::isNotNull).collect(Collectors.toList());
        HashMap<String, Object> map = new HashMap<>();
        map.put("total", page.getTotal());
        map.put("list", flowVos);
        return map;
    }

    //创建流程
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    //@CacheEvict(value = {HnkjxyConstants.FLOW_LIST,HnkjxyConstants.FLOW_YEARS,HnkjxyConstants.FLOW_DETAIL},allEntries = true)
    public void createFlow(Flow flow, User user) {

        //判断是否已创建该项目流程
        int count = this.count(new QueryWrapper<Flow>().eq("p_id", flow.getPId()));
        if (count > 0 && ObjectUtil.isNull(flow.getId())) {
            throw new RuntimeException("该项目已创建流程!");
        }

        //创建流程和修改流程
        this.saveOrUpdate(flow);

        //删除之前的流程任务
        flowTaskService.lambdaUpdate().eq(FlowTask::getParentId, flow.getId()).remove();

        //拿到流程任务表
        List<FlowTask> flowTasks = flow.getFlowTask();
        if (ObjectUtil.isNotEmpty(flowTasks)) {
            int i = 1;
            for (FlowTask item : flowTasks) {
                item.setParentId(flow.getId());
                if ("APPROVAL".equals(item.getType())) {
                    item.setSort(i++);
                }
                item.setUId(JSONArray.toJSONString(item.getUIds()));
                item.setRoleId(JSONArray.toJSONString(item.getRoleIds()));
                flowTaskService.saveOrUpdate(item);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override //当前用户获取项目审批列表
    @RedisCache(key = HnkjxyConstants.APPROVE_LIST)
    //@Cacheable(value = {HnkjxyConstants.APPROVE_LIST}, key = "#user.getUserId()+'-'+#param.getYear()+'-'+#param.getPage()+'-'+#param.getLimit()+'-'+#param.getTitle()")
    public ApiResult getProjectApproveList(ProjectParam param, User user) {
        //1、用户角色
        List<Integer> roles = userRoleMapper.getRoles(user.getUserId());

        //2、获取当前用户最大权重角色
        Role roleWeight = userRoleMapper.getRoleWeight(user.getUserId());
        //拿到审批人列表
        List<ApproveVo> approval = flowTaskMapper.getApproveVoList("APPROVAL");

        //拿到可以审批的项目列表
        List<Integer> pIds = getApproveProjectPIds(approval, user, roles, roleWeight.getWeight());
        HashMap<String, Object> map = checkProjectApprove(pIds, param, roles, user, roleWeight.getWeight());

        return ApiResult.ok("data", map);
    }

    /**
     * 拿到审批节点的人
     *
     * @param approval 待审批节点列表
     * @param user 当前用户
     * @param roles 当前用户角色ID集合
     * @param roleWeight 角色权重
     * @return 查询结果列表
     */
    public List<Integer> getApproveProjectPIds(List<ApproveVo> approval, User user, List<Integer> roles, Integer roleWeight) {
        return approval.stream().map(item -> {
            //审批节点的人和角色
            List<Integer> roleIds = JSONObject.parseArray(item.getRoleId(), Integer.class), uIds = JSONObject.parseArray(item.getUId(), Integer.class);

            //判断当前用户是否拥有对这个项目的审批权限
            ApproveVo vo = getApproveVo(user, item.getPId());
            if (ObjectUtil.isNull(vo)) {
                return null;
            }
            if ((uIds.size() > 0 && uIds.contains(user.getUserId()))) {
                return item.getPId();
            } else if (roleWeight.equals(HnkjzyEncode.DIRECTOR.getCode()) && roleIds.size() > 0 && isCheckApprove(roles, roleIds)) {
                //当前用户是教研室主任 并且审批角色中包含 并且接收人有该用户教研室的人
                int size = getUsers(item.getPId(), roles, true).size();
                if (size == 0) {
                    return null;
                }
                return item.getPId();
            } else if (roleIds.size() > 0) { //判断当前用户是否拥有对这个项目的审批权限

                //上面就已经判断完了
                return item.getPId();
            }
            return null;
        }).filter(ObjectUtil::isNotNull).distinct().collect(Collectors.toList());
    }

    /**
     * 是否有审批权限
     *
     * @param roles 当前用户角色ID集合
     * @param roleIds 目标角色ID集合
     * @return 操作或条件校验结果
     */
    public Boolean isCheckApprove(List<Integer> roles, List<Integer> roleIds) {
        return roles.stream().anyMatch(item -> !item.equals(1) && roleIds.contains(item));
    }

    /**
     * 判断该项目是否已有人提交
     *
     * @param vo 审批节点数据
     * @param roles 当前用户角色ID集合
     * @param flag 处理标记
     * @return 查询得到的数值
     */
    public Integer checkProjectSubmitCount(ApproveVo vo, List<Integer> roles, Boolean flag) {
        List<Result> results = resultMapper.selectResultCountByPId(vo.getPId(), vo.getSort().intValue() - 1);
        if (flag) { //主任审批、只能拿跟自己一个教研室的
            return results.stream().filter(item -> {
                List<Integer> roleIds = userRoleMapper.getRoles(item.getUId());
                return checkRoles(roles, roleIds);
            }).collect(Collectors.toList()).size();
        } else {
            return results.size();
        }
    }

    /**
     * 返回审批列表
     *
     * @param pIds 考核项目ID集合
     * @param param 审批流程操作或查询参数
     * @param roles 当前用户角色ID集合
     * @param user 当前用户
     * @param roleWeight 角色权重
     * @return 查询数据及相关统计信息
     */
    public HashMap<String, Object> checkProjectApprove(List<Integer> pIds, ProjectParam param, List<Integer> roles, User user, Integer roleWeight) {
        HashMap<String, Object> map = new HashMap<>();
        long total = 0L;
        List<Project> list = new ArrayList<>();
        //判断项目id是否为空
        if (ObjectUtil.isNotEmpty(pIds) || pIds.size() > 0) {
            Page<Project> page = new Page<>(param.getPage(), param.getLimit());
            LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
            if (StrUtil.isNotBlank(param.getYear())) {
                wrapper.ge(Project::getStartTime, param.getYear() + "-01-01 00:00:00")
                        .le(Project::getStartTime, param.getYear() + "-12-31 23:59:59");
            }
            if (StrUtil.isNotBlank(param.getTitle())) {
                wrapper.like(Project::getTitle, param.getTitle());
            }
            wrapper.eq(Project::getStatus, 1);
            wrapper.orderByDesc(Project::getCreateTime).in(Project::getId, pIds);
            //通过项目id拿到项目
            list = projectService.page(page, wrapper).getRecords();
            list.forEach(item -> {
                ApproveVo vo = getApproveVo(user, item.getId());
                //判断当前是用户审批、还是角色审批,false 为用户审批、true为角色审批
                Boolean flag = checkResult(user.getUserId(), vo, roles);
                //角色审批 又是主任审批
                HashSet<Integer> userId;
                if (flag && roleWeight.equals(HnkjzyEncode.DIRECTOR.getCode())) {
                    //拿到跟自己一个教研室的总接收人数
                    //userId = getUserId(item.getId(), roles, true);
                    userId = getUsers(item.getId(), roles, true);
                } else {
                    //总接收人数
                    userId = getUsers(item.getId(), roles, false);
                }
                setProjectStatus(item, vo, userId);
            });
            total = page.getTotal();
        }
        map.put("total", total);
        map.put("list", list);
        return map;
    }

    /**
     * 汇总项目接收人，并按需筛选当前角色可见的用户
     *
     * @param pId 考核项目ID
     * @param roles 当前用户角色ID集合
     * @param flag 处理标记
     * @return 查询结果列表
     */
    public HashSet<Integer> getUsers(Integer pId, List<Integer> roles, Boolean flag) {
        //拿到该项目的所有接收人
        List<FlowTask> list = flowMapper.getFlowList("CC", pId);
        //拿到接收人id
        HashSet<Integer> set = new HashSet<>();
        list.forEach(item -> {
            List<Integer> uIds = JSONArray.parseArray(item.getUId(), Integer.class), roleIds = JSONArray.parseArray(item.getRoleId(), Integer.class);
            //拿到接收用户id
            if (ObjectUtil.isNotEmpty(uIds) && uIds.size() > 0) {
                if (flag) {
                    uIds.forEach(val -> {
                        List<Integer> roles1 = userRoleMapper.getRoles(val);
                        if (checkRoles(roles, roles1)) {
                            set.add(val);
                        }
                    });
                } else {
                    set.addAll(uIds);
                }
            } else if (ObjectUtil.isNotEmpty(roleIds) && roleIds.size() > 0) {
                HashSet<Integer> byUserIds = new HashSet<>();
                if (flag) {
                    //教研室主任审批 //判断是否一个教研室
                    Integer role = checkRole(roles, roleIds);
                    if (!role.equals(0)) {
                        byUserIds = new HashSet<>(userRoleMapper.selectUserIdByRoleId(role));
                    }
                } else {
                    for (Integer role : roleIds) {
                        byUserIds.addAll(new HashSet<>(userRoleMapper.selectUserIdByRoleId(role)));
                    }
                }
                if (ObjectUtil.isNotEmpty(byUserIds) && byUserIds.size() > 0) {
                    set.addAll(byUserIds);
                }
            }
        });
        return set;
    }

    /**
     * 判断接收人中是否包含本教研室的人获取接收人列表
     *
     * @param pId 考核项目ID
     * @param roles 当前用户角色ID集合
     * @param flag 处理标记
     * @param step 审批步骤
     * @return 查询结果列表
     */
    public HashSet<Integer> getUserId(Integer pId, List<Integer> roles, Boolean flag, Integer step) {
        //拿到该项目的所有接收人
        List<FlowTask> list = flowMapper.getFlowList("CC", pId);
        //拿到接收人id
        HashSet<Integer> set = new HashSet<>();
        list.forEach(item -> {
            List<Integer> uIds = JSONArray.parseArray(item.getUId(), Integer.class), roleIds = JSONArray.parseArray(item.getRoleId(), Integer.class);
            //拿到接收用户id
            if (ObjectUtil.isNotEmpty(uIds) && uIds.size() > 0) {
                if (flag) {
                    uIds.forEach(val -> {
                        List<Integer> roles1 = userRoleMapper.getRoles(val);
                        if (checkRoles(roles, roles1)) {
                            //set.add(val);
                            Integer uId = resultMapper.checkResultByUId(pId, step, val);
                            if (ObjectUtil.isNotEmpty(uId) && uId > 0) {
                                set.add(val);
                            }
                        }
                    });
                } else {
                    //set.addAll(uIds);
                    set.addAll(resultMapper.selectUserIdByStep(pId, step));
                }
            } else if (ObjectUtil.isNotEmpty(roleIds) && roleIds.size() > 0) {
                HashSet<Integer> byUserIds = new HashSet<>();
                if (flag) {
                    //教研室主任审批 //判断是否一个教研室
                    Integer role = checkRole(roles, roleIds);
                    if (!role.equals(0)) {
                        byUserIds = resultMapper.getUserIdByRole(role, pId, step);
                    }
                } else {
                    for (Integer role : roleIds) {
                        byUserIds.addAll(resultMapper.getUserIdByRole(role, pId, step));
                    }
                }
                if (ObjectUtil.isNotEmpty(byUserIds) && byUserIds.size() > 0) {
                    set.addAll(byUserIds);
                }
            }
        });
        return set;
    }

    /**
     * 查找当前用户与目标角色集合共有的非普通角色
     *
     * @param roles 当前用户角色ID集合
     * @param roleIds 目标角色ID集合
     * @return 匹配的非普通角色ID；无匹配角色时返回零
     */
    public Integer checkRole(List<Integer> roles, List<Integer> roleIds) {
        for (Integer role : roles) {
            Integer weight = roleMapper.getWeightByRole(role);
            if (!(weight.equals(HnkjzyEncode.NORMAL.getCode())) && roleIds.contains(role)) {
                return role;
            }
        }
        return 0;
    }

    /**
     * 通过角色拿到跟自己一个教研室的角色
     *
     * @param roles 当前用户角色ID集合
     * @param roleIds 目标角色ID集合
     * @return 查询结果列表
     */
    public List<Integer> selectRoles(List<Integer> roles, List<Integer> roleIds) {
        return roles.stream().filter(val -> {
            Role one = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_id", val));
            return one.getWeight().equals(HnkjzyEncode.DEPARTMENT.getCode()) && roleIds.contains(val);
            //判断接收的角色中有没有跟他一个教研室
        }).collect(Collectors.toList());
    }

    /**
     * 根据角色拿到用户id
     *
     * @param roleIds 目标角色ID集合
     * @return 查询结果列表
     */
    public HashSet<Integer> selectUIdsByRoleIds(List<Integer> roleIds) {
        HashSet<Integer> list = new HashSet<>();
        roleIds.forEach(item -> {
            list.addAll(flowMapper.findUserIdByRoleId(item));
        });
        return list;
    }

    /**
     * 判断什么审批
     *
     * @param userId 用户ID
     * @param item 审批节点信息
     * @param roles 当前用户角色ID集合
     * @return 操作或条件校验结果
     */
    public Boolean checkResult(Integer userId, ApproveVo item, List<Integer> roles) {
        //用户审批
        List<Integer> roleIds = JSONObject.parseArray(item.getRoleId(), Integer.class), uIds = JSONObject.parseArray(item.getUId(), Integer.class);
        if ((uIds.size() > 0 && uIds.contains(userId))) {
            return false;
        }
        //角色审批
        return roleIds.size() > 0 && isCheckApprove(roles, roleIds);
    }

    /**
     * 设置项目状态
     *
     * @param item 考核项目信息
     * @param vo 审批节点数据
     * @param userId 用户ID
     */
    public void setProjectStatus(Project item, ApproveVo vo, HashSet<Integer> userId) {
        QueryWrapper<Result> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(userId) || userId.size() > 0) {
            //已审批人数
            queryWrapper.select("DISTINCT u_id").lambda()
                    .eq(Result::getPId, item.getId())
                    .ge(Result::getStep, vo.getSort())
                    .in(Result::getUId, userId);

            Integer count = resultMapper.selectCount(queryWrapper);
            item.setApproveCount(count);
            //判断是否已全部完成审批. 人数和已审批的结果的记录数
            item.setStatus(userId.size() == count.intValue() ? 1 : 0);
        }
        ;
    }

    /**
     * 获取当前当前审批人信息
     * 单个项目和用户
     *
     * @param user 当前用户
     * @param pId 考核项目ID
     * @return 审批节点信息
     */
    public ApproveVo getApproveVo(User user, Integer pId) {
        //获取当前审批人角色
        List<Integer> roles = userRoleMapper.getRoles(user.getUserId());
        //返回对应的审批列表
        //拿到该项目的审批流程
        List<ApproveVo> approval = flowTaskMapper.getFlowStepList("APPROVAL", pId);

        //流程和流程的详情
        for (ApproveVo item : approval) {
            List<Integer> roleIds = JSONObject.parseArray(item.getRoleId(), Integer.class), uIds = JSONObject.parseArray(item.getUId(), Integer.class);
            if ((uIds.size() > 0 && uIds.contains(user.getUserId())) || (roleIds.size() > 0 && isCheckApprove(roles, roleIds))) {
                return item;
            }
        }
        return null;
    }

    /**
     * 获取该项目所有审批人信息
     *
     * @param pId 考核项目ID
     * @return 审批节点列表
     */
    public List<ApproveVo> getApproveVoList(Integer pId) {
        //拿到该项目的所有审批流程
        return flowTaskMapper.getFlowStepList("APPROVAL", pId);
    }

    //获取用户提交结果列表
    /**
     * {@inheritDoc}
     */
    @Override
    //@RedisCache(key = HnkjxyConstants.RESULT_LIST)
    //@Cacheable(value = {HnkjxyConstants.RESULT_LIST},key = "#user.getUserId() +'-'+ #param.getPId() + '-' + #param.getPage() + '-' + #param.getLimit() + '-' + #param.getStatus()")
    public Map<String, Object> getResultOutcomeList(ApproveParam param, User user) {
        //拿到审批(当前用户的审批的人物)
        ApproveVo approveVo = getApproveVo(user, param.getPId());
        //没有权限审批该项目
        if (ObjectUtil.isNull(approveVo)) {
            throw new RuntimeException("没有权限审批该项目！");
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("total", 0);
        map.put("list", new ArrayList<>());

        int sort = approveVo.getSort();//当前审批用户的审批的sort
        Role roleWeight = userRoleMapper.getRoleWeight(user.getUserId());
        //获取当前项目提交的用户id
        //获取当前用户的所有角色
        List<Integer> roles = userRoleMapper.getRoles(user.getUserId());
        //拿到当前审批的用户id
        Boolean flag = checkResult(user.getUserId(), approveVo, roles);
        //角色审批 又是主任审批
        HashSet<Integer> users;
        if (flag && roleWeight.getWeight().equals(HnkjzyEncode.DIRECTOR.getCode())) {
            //拿到跟自己一个教研室的总接收人数
            users = getUserId(param.getPId(), roles, true, sort - 1);
        } else {
            //总接收人数
            users = getUserId(param.getPId(), roles, false, sort - 1);
        }

        if (ObjectUtil.isEmpty(users)) {
            return map;
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (ObjectUtil.isNotNull(param.getNickName())) {
            wrapper.like(User::getNickName, param.getNickName());
        }
        wrapper.in(User::getUserId, users);
        List<User> records = userService.list(wrapper);//拿到这些用户
        List<User> userList = records.stream().filter(item -> {
            //拿到项目包含用户的记录已经审批的条数
            Integer integer = resultItemMapper.selectStep(param.getPId(), item.getUserId(), user.getUserId(), sort);
            item.setPassword(null);
            //如果该用户有已近审批的结果，那就要对该用户的结果进行审批
            if (integer > 0) {
                item.setStatus(1);//设置为1 该用户要被审批
            } else {
                item.setStatus(0);//没有查到，就不要审批了，没有东西审批
            }

            if (ObjectUtil.isNotNull(param.getStatus())) {
                return item.getStatus().equals(param.getStatus());
            }
            return true;
        }).collect(Collectors.toList());
        Map<String, Object> page = PageUtils.page(userList, param.getPage().intValue(), param.getLimit().intValue());
        return page;
    }

    /**
     * 从用户列表中筛选具有目标角色的用户ID
     *
     * @param users 用户列表
     * @param roles 当前用户角色ID集合
     * @return 查询结果列表
     */
    public List<Integer> getUserIdByRoles(List<User> users, List<Integer> roles) {
        //判断是否两个人是否包含相同角色信息
        return users.stream().map(item -> {
            List<Integer> userRoles = userRoleMapper.getRoles(item.getUserId());
            if (checkRoles(roles, userRoles)) {
                return item.getUserId();
            }
            return null;
        }).filter(ObjectUtil::isNotNull).collect(Collectors.toList());
    }

    /**
     * 获取审批结果详情
     *
     * @param vo 审批查询参数
     * @return 审批结果详情
     */
    @Override
    public ApiResult getAppRoveResultDetail(ApproveParam vo) {
        List<Result> results = resultMapper.findByUIdAndPID(vo.getUId(), vo.getPId());
        results.forEach(item -> {
            item.setTask(taskService.getById(item.getTaskId()));
            item.setEvidenceList(JSON.parseArray(item.getEvidence(), String.class));

            //resultExtend 表中记录了附件信息，关联了result表中的id
            List<ResultExtend> resultExtends = resultExtendService.list(new QueryWrapper<ResultExtend>().eq("result_id", item.getId()));
            resultExtends.forEach(val -> val.setEvidenceList(JSON.parseArray(val.getEvidence(), String.class)));
            item.setResultExtends(resultExtends);
        });
        HashMap<String, Object> map = new HashMap<>();
        map.put("results", results);
        return ApiResult.ok("data", map);
    }

    //提交审批结果
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = {HnkjxyConstants.APPROVE_LIST, HnkjxyConstants.RADAR_CHART, HnkjxyConstants.COLUMNAR_CHART,
            HnkjxyConstants.END_PROJECT, HnkjxyConstants.NOT_SUB_LIST, HnkjxyConstants.RESULT_WEEK_COUNT,
            HnkjxyConstants.PROJECT_SCORE, HnkjxyConstants.PROJECT_SCALE, HnkjxyConstants.USER_PROJECTS, HnkjxyConstants.RESULT_LIST,
            HnkjxyConstants.USER_PROJECT_YEAR, HnkjxyConstants.PROJECT_FLOW, HnkjxyConstants.NOTICE_LIST, HnkjxyConstants.NOTICES, HnkjxyConstants.ASSESS_LIST}, allEntries = true)
    public void submitApprove(ResultItem resultItem, User user) {
        projectTaskGuard.lock(resultItem.getPId());
        projectTaskGuard.validateTasks(resultItem.getPId(), resultItem.getUId(), resultItem.getResults(), true);
        ApproveVo vo = getApproveVo(user, resultItem.getPId());
        if (ObjectUtil.isNull(vo)) {
            throw new RuntimeException("没有审批权限！");
        }

        //TODO 判断是否已审批
        LambdaQueryWrapper<ResultItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResultItem::getPId, resultItem.getPId())
                .eq(ResultItem::getUId, resultItem.getUId())
                .eq(ResultItem::getUserId, user.getUserId())
                .eq(ResultItem::getStatus, 1)
                .eq(ResultItem::getStep, vo.getSort());
        Integer count = resultItemService.count(wrapper);
        if (count > 0) {
            throw new RuntimeException("已进行审批，不能再重复审批！");
        }

        //设置当前审批步骤
        resultItem.setStep(vo.getSort());
        //设置当前审批人
        resultItem.setUserId(user.getUserId());

        //未审批开始审批
        Integer maxSort = getFLowMaxSort(resultItem.getPId());

        //判断是否已完成审批

        resultItem.getResults().forEach(item -> {
            item.setStep(vo.getSort());
            if (maxSort.equals(vo.getSort())) { //已完成审批
                item.setIsFinish(1);
            }
        });

        if (!resultService.updateBatchById(resultItem.getResults())) throw new IllegalStateException("审批结果更新失败，已回滚");
        //提交审批结果
        if (resultItemMapper.insert(resultItem) != 1) throw new IllegalStateException("审批保存失败，已回滚");

        //打回
        if (resultItem.getIsFlag().equals(1)) {
            resultService.lambdaUpdate().eq(Result::getPId, resultItem.getPId())
                    .eq(Result::getUId, resultItem.getUId())
                    .set(Result::getIsFinish, 3)
                    .set(Result::getStep, 0).update();
            resultItemService.lambdaUpdate().eq(ResultItem::getPId, resultItem.getPId())
                    .eq(ResultItem::getUId, resultItem.getUId())
                    .set(ResultItem::getStatus, 0).update();
            noticeService.noticeReturn(resultItem);
        }

    }


    //查询项目流程信息
    /**
     * {@inheritDoc}
     */
    @Override
    //@Cacheable(value = {HnkjxyConstants.FLOW_DETAIL},key = "#pId",sync = true)
    //@RedisCache(key = HnkjxyConstants.FLOW_DETAIL)
    public FlowQueryVo findFlowByPId(String pId) {
        if (ObjectUtil.isNull(pId)) {
            throw new RuntimeException("项目id不能为空！");
        }
        Flow flow = flowMapper.findFlowByPId(pId);
        if (ObjectUtil.isNull(flow)) {
            return null;
        }
        //项目流程信息
        FlowQueryVo vo = new FlowQueryVo();
        vo.setTitle(flow.getFlowName());
        ArrayList<FlowQueryTaskVo> taskVos = new ArrayList<>();
        List<FlowTask> flowTasks = flowTaskMapper.selectByParentId(flow.getId());
        flowTasks.forEach(item -> {
            String type = item.getType();
            if ("CC".equals(type) || "APPROVAL".equals(type)) {
                List<Integer> uIds = JSONArray.parseArray(item.getUId(), Integer.class), roleIds = JSONArray.parseArray(item.getRoleId(), Integer.class);
                if (ObjectUtil.isNotEmpty(uIds) && uIds.size() > 0) {
                    taskVos.add(getFlowQueryTaskVo(uIds, null, type));
                } else if (ObjectUtil.isNotEmpty(roleIds) && roleIds.size() > 0) {
                    taskVos.add(getFlowQueryTaskVo(null, roleIds, type));
                }
            }
        });
        vo.setChildren(taskVos);
        return vo;
    }

    /**
     * 汇总指定用户及角色对应的流程节点展示信息
     *
     * @param uIds 用户ID集合
     * @param roleIds 目标角色ID集合
     * @param type 查询或节点类型
     * @return 流程节点展示信息
     */
    private FlowQueryTaskVo getFlowQueryTaskVo(List<Integer> uIds, List<Integer> roleIds, String type) {
        FlowQueryTaskVo taskVo = new FlowQueryTaskVo();
        if ("CC".equals(type)) {
            taskVo.setTitle("接收人");
        } else if ("APPROVAL".equals(type)) {
            taskVo.setTitle("审批人");
        }
        ArrayList<String> list = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(uIds) && uIds.size() > 0) {
            List<User> users = userMapper.selectList(new QueryWrapper<User>().in("user_id", uIds));
            users.forEach(item -> {
                list.add(item.getNickName());
            });
        } else if (ObjectUtil.isNotEmpty(roleIds) && roleIds.size() > 0) {
            List<Role> roleId = roleMapper.selectList(new QueryWrapper<Role>().in("role_id", roleIds));
            roleId.forEach(item -> {
                list.add(item.getRoleName());
            });
        }
        taskVo.setList(list);
        return taskVo;
    }

    /**
     * 获取提交的结果信息
     *
     * @param pId 考核项目ID
     * @param userId 用户ID
     * @return 统一接口响应
     */
    @Override
    public ApiResult getProjectResultDetail(Integer pId, Integer userId) {
        Project project = projectService.getById(pId);
        if (ObjectUtil.isNull(project)) {
            throw new RuntimeException("项目不存在！");
        }
        List<Result> results = resultMapper.findByUIdAndPID(userId, pId);
        results.forEach(item -> {
            item.setTask(taskService.getById(item.getTaskId()));
            item.setEvidenceList(JSON.parseArray(item.getEvidence(), String.class));
            List<ResultExtend> resultExtends = resultExtendService.list(new QueryWrapper<ResultExtend>().eq("result_id", item.getId()));
            resultExtends.forEach(val -> val.setEvidenceList(JSON.parseArray(val.getEvidence(), String.class)));
            item.setResultExtends(resultExtends);
        });
        List<Task> tasks = taskService.list(new QueryWrapper<Task>().eq("p_id", pId));
        HashMap<String, Object> map = new HashMap<>();
        map.put("results", results);
        map.put("task", tasks);
        return ApiResult.ok("data", map);
    }

    //获取当前项目审批流程状态
    /**
     * {@inheritDoc}
     */
    @Override
    //@RedisCache(key = HnkjxyConstants.PROJECT_FLOW)
    //@Cacheable(value = {HnkjxyConstants.PROJECT_FLOW},key = "#pId +'-'+ #userId",sync = true)
    public FlowStatusVo getProjectFLow(String pId, Integer userId) {
        //拿到该项目的流程
        List<ApproveVo> vos = getApproveVoList(Integer.valueOf(pId));
        //拿到提交结果
        List<Result> result = resultMapper.findByUIdAndPID(userId, Integer.valueOf(pId));
        if (ObjectUtil.isNull(result)) {
            return null;
        }
        FlowStatusVo statusVo = new FlowStatusVo();
        //拿到当前项目的流程
        List<FlowStatus> statusVos = vos.stream().map(item -> {
            //创建对应的流程
            FlowStatus vo = new FlowStatus();
            //设置流程名称
            statusVo.setTitle(item.getFlowName());
            //0未审批、1已审批、3已打回
            //当前流程
            List<ResultItem> currentFlows = resultItemService.lambdaQuery()
                    .eq(ResultItem::getPId, pId)
                    .eq(ResultItem::getUId, userId)
                    .eq(ResultItem::getStep, item.getSort())
                    .eq(ResultItem::getStatus, 1)
                    .orderByDesc(ResultItem::getCreateTime)
                    .list();
            if (ObjectUtil.isNotNull(currentFlows) && currentFlows.size() > 0) {
                ResultItem currentFlow = currentFlows.get(0);
                User user = userMapper.selectById(currentFlow.getUserId());
                vo.setTimestamp(currentFlow.getCreateTime());
                vo.setTitle("审批人：" + item.getSort());
                vo.setContent("审批人：".concat(user.getNickName()));
                vo.setScore(currentFlow.getScore());
                vo.setFlag(1);
            } else {
                vo.setTitle("审批人：" + item.getSort());
                vo.setContent("审批人：" + StringUtils.join(getApproveBySortName(item), ','));
                vo.setFlag(0);
            }
            vo.setHistory(getHistoryApprove(Integer.parseInt(pId), userId, item.getSort()));
            return vo;
        }).collect(Collectors.toList());
        statusVo.setChildren(statusVos);
        return statusVo;
    }

    /**
     * 查询审批节点指定的用户姓名或角色名称
     *
     * @param item 审批节点信息
     * @return 查询结果列表
     */
    public List<String> getApproveBySortName(ApproveVo item) {
        List<Integer> uIds = JSONArray.parseArray(item.getUId(), Integer.class),
                roleIds = JSONArray.parseArray(item.getRoleId(), Integer.class);
        ArrayList<String> list = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(uIds)) {
            List<User> users = userMapper.selectList(new QueryWrapper<User>().in("user_id", uIds));
            users.forEach(val -> {
                list.add(val.getNickName());
            });
        } else {
            List<Role> roleId = roleMapper.selectList(new QueryWrapper<Role>().in("role_id", roleIds));
            roleId.forEach(val -> {
                list.add(val.getRoleName());
            });
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    //@RedisCache(key = HnkjxyConstants.APPROVE_YEARS)
    //@Cacheable(value = {HnkjxyConstants.APPROVE_YEARS},key = "#user.getUserId()")
    public List<String> getProjectApproveYears(User user) {
        List<Integer> roles = userRoleMapper.getRoles(user.getUserId());
        //获取当前用户最大权重角色
        Role roleWeight = userRoleMapper.getRoleWeight(user.getUserId());
        //拿到审批人列表
        List<ApproveVo> approval = flowTaskMapper.getApproveVoList("APPROVAL");
        //项目列表
        List<Integer> pIds = getApproveProjectPIds(approval, user, roles, roleWeight.getWeight());
        if (ObjectUtil.isNotEmpty(pIds) && pIds.size() > 0) {
            QueryWrapper<Project> wrapper = new QueryWrapper<>();
            wrapper
                    .ne("status", 3)
                    .in("id", pIds)
                    .orderByDesc("start_time");
            List<String> years = projectMapper.getUserProjectYears(wrapper);
            HashSet<String> set = new HashSet<>(years);
            return new ArrayList<String>(set);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    //@RedisCache(key = HnkjxyConstants.FLOW_YEARS)
    //@Cacheable(value = {HnkjxyConstants.FLOW_YEARS},key = "#userId",sync = true)
    public List<String> getFlowYears(Integer userId) {
        Role role = userRoleMapper.getRoleWeight(userId);
        if (role.getWeight().equals(HnkjzyEncode.ADMIN.getCode())) {
            userId = null;
        }
        List<String> years = flowMapper.getFlowYears(userId);
        return years;
    }

    //获取未提交人员列表
    /**
     * {@inheritDoc}
     */
    @Override
    //@RedisCache(key = HnkjxyConstants.NOT_SUB_LIST)
    //@Cacheable(value = {HnkjxyConstants.NOT_SUB_LIST},key = "#user.getUserId()+'-'+#queryVo.getPId()+'-'+#queryVo.getPage()+'-'+#queryVo.getLimit()+'-'+#queryVo.getName()")
    public Map<String, Object> getNotSubmittedList(UserParam queryVo, User user) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("total", 0);
        map.put("list", new ArrayList<>());

        ApproveVo vo = getApproveVo(user, queryVo.getPId());
        if (ObjectUtil.isNull(vo)) {
            return map;
        }

        List<Integer> roles = userRoleMapper.getRoles(user.getUserId());
        Role role = userRoleMapper.getRoleWeight(user.getUserId());

        Boolean aBoolean = checkResult(user.getUserId(), vo, roles);

        //获取已经提交的用户id
        List<Integer> userIds = resultMapper.getUserByResult(queryVo.getPId(), vo.getSort() - 1);

        HashSet<Integer> userId = new HashSet<>();
        if (aBoolean && role.getWeight().equals(HnkjzyEncode.DIRECTOR.getCode())) {
            //获取总接收人
            userId = getUsers(queryVo.getPId(), roles, true);
        } else {
            userId = getUsers(queryVo.getPId(), roles, false);
        }

        //过滤出未提交的用户id
        List<Integer> uIds = userId.stream().filter(item -> !userIds.contains(item)).collect(Collectors.toList());

        if (ObjectUtil.isEmpty(uIds)) {
            return map;
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (ObjectUtil.isNotNull(queryVo.getNickName())) {
            wrapper.like(User::getNickName, queryVo.getNickName());
        }
        wrapper.in(User::getUserId, new ArrayList<>(uIds));

        Page<User> page = new Page<>(queryVo.getPage(), queryVo.getLimit());
        userService.page(page, wrapper);

        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return map;
    }


    /**
     * 获取当前审批信息
     *
     * @param itemList 审批明细列表
     * @param sort 审批步骤排序值
     * @return 审批明细信息
     */
    private ResultItem getCurrentFlow(List<ResultItem> itemList, Integer sort) {
        for (ResultItem item : itemList) {
            if (item.getStep().equals(sort) && item.getIsFlag().equals(0)) {
                return item;
            }
        }
        return null;
    }


    /**
     * 获取历史打回
     *
     * @param pId 考核项目ID
     * @param uId 被考核用户ID
     * @param step 审批步骤
     * @return 查询结果列表
     */
    private List<FlowStatus> getHistoryApprove(Integer pId, Integer uId, Integer step) {
        List<ResultItem> opinion = resultItemMapper.findResultOpinion(pId, uId, step);
        List<FlowStatus> collect = opinion.stream().map(item -> {
            //创建对应的流程
            FlowStatus vo = new FlowStatus();
            //0未审批、1已审批、3已打回
            User user = userMapper.selectById(item.getUserId());
            vo.setTimestamp(item.getCreateTime());
            vo.setContent("审批人：".concat(user.getNickName()));
            vo.setOpinion("打回意见：".concat(item.getOpinion()));
            vo.setScore(item.getScore());
            vo.setFlag(3);
            return vo;
        }).filter(ObjectUtil::isNotNull).collect(Collectors.toList());
        return collect;
    }


    /**
     * 检查是否包含角色,除了普通用户之外
     *
     * @param roles 当前用户角色ID集合
     * @param roleIds 目标角色ID集合
     * @return 操作或条件校验结果
     */
    public Boolean checkRoles(List<Integer> roles, List<Integer> roleIds) { //roles 当前登录用户角色，roleIds用户id角色
        //除了普通用户角色之外, 属于同一个教研室
        //判断接收的角色中有没有跟他一个教研室
        return roles.stream().anyMatch(role -> {
            Role one = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_id", role));
            return one.getWeight().equals(HnkjzyEncode.DEPARTMENT.getCode()) && roleIds.contains(role); //判断接收的角色中有没有跟他一个教研室
        });
    }

    /**
     * 拿到该项目流程最大步骤数
     *
     * @param pId 考核项目ID
     * @return 查询得到的数值
     */
    public Integer getFLowMaxSort(Integer pId) {
        return flowTaskMapper.getFlowMaxSort("APPROVAL", pId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateProjectFlowStatus(Integer pId, Integer status) {
        flowMapper.updateFLowStatusByPId(pId, status);
    }
}
