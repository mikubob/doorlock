package com.hnkjzyxy.ab.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hnkjzyxy.ab.enums.HnkjzyEncode;
import com.hnkjzyxy.ab.annotation.RedisCache;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.dto.TaskDto;
import com.hnkjzyxy.ab.mapper.ProjectMapper;
import com.hnkjzyxy.ab.mapper.ResultMapper;
import com.hnkjzyxy.ab.mapper.RoleMapper;
import com.hnkjzyxy.ab.mapper.TaskMapper;
import com.hnkjzyxy.ab.mapper.UserMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.HomeParam;
import com.hnkjzyxy.ab.service.HomeService;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.utils.ResultUtils;
import com.hnkjzyxy.ab.vo.RadarChartVo;
import com.hnkjzyxy.ab.vo.TeacherAndDepartmentVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * 首页统计Service实现类
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-17 10:58
 */
@Service
public class HomeServiceImpl implements HomeService {

    /**
     * 考核项目业务服务
     */
    @Resource
    private ProjectService projectService;
    /**
     * 考核项目数据访问接口
     */
    @Resource
    private ProjectMapper projectMapper;
    /**
     * 用户角色关联数据访问接口
     */
    @Resource
    private UserRoleMapper userRoleMapper;
    /**
     * TaskMapper数据访问接口
     */
    @Resource
    private TaskMapper taskMapper;
    /**
     * 考核结果数据访问接口
     */
    @Resource
    private ResultMapper resultMapper;
    /**
     * 角色数据访问接口
     */
    @Resource
    private RoleMapper roleMapper;

    /**
     * 用户数据访问接口
     */
    @Resource
    private UserMapper userMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @RedisCache(key = HnkjxyConstants.RADAR_CHART)
    public RadarChartVo radarChart(HomeParam param, User user) {
        //判断角色
        Role role = userRoleMapper.getRoleWeight(user.getUserId());
        RadarChartVo chartVo = new RadarChartVo();
        ArrayList<String> metrics = new ArrayList<>();
        ArrayList<Integer> value = new ArrayList<>();
        if (role.getWeight().intValue() >= HnkjzyEncode.DIRECTOR.getCode().intValue()) {
            //根据年度项目名称查询项目
            FlowTask flowTask = projectMapper.getRadarProjects(param.getProjectId());
            HashSet<Integer> uIds = new HashSet<>();
            List<Integer> roleIds = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(flowTask)) {
                //添加这个项目流程中参与者的用户Id和角色Id
                uIds.addAll(JSON.parseArray(flowTask.getUId(), Integer.class));
                roleIds.addAll(JSON.parseArray(flowTask.getRoleId(), Integer.class));
            }
            roleIds.forEach(item -> {
                //把角色对应的用户Id添加到用户集合中
                uIds.addAll(userRoleMapper.selectUserIdByRoleId(item));
            });

            //开始对项目参与者的用户进行处理
            if (ObjectUtil.isNotEmpty(uIds)) {
                //1、如果当前的用户的校领导来进行查看
                if (role.getWeight().equals(HnkjzyEncode.LEADER.getCode())) {
                    //校领导
                    List<TaskDto> taskDtos = taskMapper.selectGroupMetrics(param.getProjectId());
                    for (TaskDto val : taskDtos) {
                        List<Integer> list = resultMapper.checkResultScore(param.getProjectId(), uIds, val.getCategory());
                        OptionalDouble average = list.stream().mapToInt(t -> t.intValue()).average();
                        double avege = 0;
                        if (average.isPresent()) {//是否存在双精度的值
                            avege = average.getAsDouble();
                        }
                        metrics.add(val.getCategory());//获得分类名称
                        value.add(ResultUtils.calc(avege / val.getScore().doubleValue()));//分数变为等级
                    }
                } else if (role.getWeight().equals(HnkjzyEncode.DEAN.getCode())) {
                    //2.二级学院
                    //过滤二级学院下教研室的用户数据
                    HashSet<Integer> userIds = new HashSet<>();
                    uIds.forEach(item -> {
                        List<Integer> valRoles = userRoleMapper.getRoles(item);//获得项目中用户的角色
                        List<Integer> userRoles = userRoleMapper.getRoles(user.getUserId());//获得当前登录用户的角色
                        if (checkDeanRoles(userRoles, valRoles)) {
                            userIds.add(item);
                        }
                    });
                    if (ObjectUtil.isNotEmpty(userIds)) {
                        List<TaskDto> taskDtos = taskMapper.selectGroupMetrics(param.getProjectId());
                        for (TaskDto val : taskDtos) {
                            List<Integer> list = resultMapper.checkResultScore(param.getProjectId(), userIds, val.getCategory());
                            OptionalDouble average = list.stream().mapToInt(t -> t.intValue()).average();
                            double avege = 0;
                            if (average.isPresent()) {
                                avege = average.getAsDouble();
                            }
                            metrics.add(val.getCategory());
                            value.add(ResultUtils.calc(avege / val.getScore().doubleValue()));
                        }
                    }
                } else if (role.getWeight().equals(HnkjzyEncode.DIRECTOR.getCode())) {
                    //3、如果当前的用户是教研室主任
                    //过滤自己教研室的用户数据
                    HashSet<Integer> userIds = new HashSet<>();
                    uIds.forEach(item -> {
                        List<Integer> valRoles = userRoleMapper.getRoles(item);//获得项目中用户的角色
                        List<Integer> userRoles = userRoleMapper.getRoles(user.getUserId());//获得当前登录用户的角色
                        if (checkRoles(userRoles, valRoles)) {
                            userIds.add(item);
                        }
                    });
                    if (ObjectUtil.isNotEmpty(userIds)) {
                        List<TaskDto> taskDtos = taskMapper.selectGroupMetrics(param.getProjectId());
                        for (TaskDto val : taskDtos) {
                            List<Integer> list = resultMapper.checkResultScore(param.getProjectId(), userIds, val.getCategory());
                            OptionalDouble average = list.stream().mapToInt(t -> t.intValue()).average();
                            double avege = 0;
                            if (average.isPresent()) {
                                avege = average.getAsDouble();
                            }
                            metrics.add(val.getCategory());
                            value.add(ResultUtils.calc(avege / val.getScore().doubleValue()));
                        }
                    }
                }
            }
            chartVo.setMetrics(metrics);
            chartVo.setValue(value);
        } else {
            //3、如果当前的用户是个人，普通的用户
            List<TaskDto> taskDtos = taskMapper.selectGroupMetrics(param.getProjectId());
            HashSet<Integer> userIds = new HashSet<>();
            userIds.add(user.getUserId());
            if (ObjectUtil.isNotEmpty(userIds)) {
                for (TaskDto val : taskDtos) {
                    List<Integer> list = resultMapper.checkResultScore(param.getProjectId(), userIds, val.getCategory());
                    OptionalDouble average = list.stream().mapToInt(t -> t.intValue()).average();
                    double avege = 0;
                    if (average.isPresent()) {
                        avege = average.getAsDouble();
                    }
                    metrics.add(val.getCategory());
                    value.add(ResultUtils.calc(avege / val.getScore().doubleValue()));
                }
            }
            chartVo.setMetrics(metrics);
            chartVo.setValue(value);
        }
        return chartVo;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public RadarChartVo getChartByTeacherOrDepartment(HomeParam param, User user) {
        RadarChartVo chartVo = new RadarChartVo();
        ArrayList<String> metrics = new ArrayList<>();
        ArrayList<Integer> value = new ArrayList<>();
        //判断是否为空
        Optional<String> department = Optional.ofNullable(param.getDepartment());
        Optional<String> teacher = Optional.ofNullable(param.getTeacher());


        //按照项目的分类获得每个分类的总的分数
        List<TaskDto> taskDto = taskMapper.selectGroupMetrics(param.getProjectId());

        if (ObjectUtil.isNotEmpty(department.get())) {
            taskDto.forEach(item -> {
                List<Integer> score = resultMapper.selectScoreByDepartment(param.getProjectId(), item.getCategory(), department.get());

                System.out.println(score);
                Optional.ofNullable(score)
                        .filter(s -> !s.isEmpty())
                        .ifPresent(s -> {
                            s.stream().mapToInt(t -> t).average().ifPresent(t -> {
                                metrics.add(item.getCategory());
                                value.add(ResultUtils.calc(t / item.getScore().doubleValue()));
                            });
                        });

            });
        } else {
            //按用户，有的是用户的姓名，需要的是结果表，任务表

            taskDto.forEach(item -> {
                List<Integer> score = resultMapper.selectScoreByTeacher(param.getProjectId(), teacher.get());

                Optional.ofNullable(score)
                        .filter(s -> !s.isEmpty())
                        .ifPresent(s -> {
                            // 执行后续逻辑
                            s.stream().mapToInt(t -> t).average().ifPresent(t -> {
                                metrics.add(item.getCategory());
                                value.add(ResultUtils.calc(t / item.getScore().doubleValue()));
                            });
                        });

            });
        }
        //根据部门或者教师查询用户的分数
        chartVo.setMetrics(metrics);
        chartVo.setValue(value);

        return chartVo;
    }


    /**
     * 根据项目ID查询教师和部门信息
     *
     * @param projectId 项目ID
     * @return 项目关联的老师与教研室
     */
    @Override
    public TeacherAndDepartmentVo getTeacherAndDepartment(Integer projectId) {
        List<Integer> userList = this.getUserListByProject(projectId);
        Optional<List<Integer>> list = Optional.ofNullable(userList);

        TeacherAndDepartmentVo res = new TeacherAndDepartmentVo();
        //根据用户的Id查询用户的名称，所属的教研室
        list.ifPresent(item -> {
            //1、 查询老师的名称
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                    .select(User::getNickName)
                    .in(User::getUserId, list.get());
            List<User> users = userMapper.selectList(wrapper);
            List<String> collect = users.stream().map(User::getNickName).collect(Collectors.toList());
            res.setTeacherNameList(collect);
            //2、查询教研室的名称
            List<String> set = list.get().stream()
                    .map(userId -> userRoleMapper.selectRoleByUserId(userId))
                    .flatMap(List::stream)
                    .filter(roleName -> roleName.contains("教研室"))
                    .distinct()
                    .collect(Collectors.toList());
            res.setDepartmentNameList(set);
        });

        return res;
    }

    /**
     * 判断角色集合是否共有教研室角色
     *
     * @param roles 当前用户角色ID集合
     * @param roleIds 目标角色ID集合
     * @return 操作或条件校验结果
     */
    public Boolean checkRoles(List<Integer> roles, List<Integer> roleIds) { //roles 当前登录用户角色，roleIds用户id角色:这个都是一个用户所拥有的所有角色
        //除了普通用户角色之外, 属于同一个教研室
        //判断接收的角色中有没有跟他一个教研室
        return roles.stream().anyMatch(role -> {
            Role one = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_id", role));
            return one.getWeight().equals(HnkjzyEncode.DEPARTMENT.getCode()) && roleIds.contains(role); //判断接收的角色中有没有跟他一个教研室
        });
    }


    /**
     * 判断角色集合是否存在共有角色
     *
     * @param roles 当前用户角色ID集合
     * @param roleIds 目标角色ID集合
     * @return 操作或条件校验结果
     */
    private Boolean checkDeanRoles(List<Integer> roles, List<Integer> roleIds) { //roles 当前登录用户角色，roleIds用户id角色:这个都是一个用户所拥有的所有角色
        //二级学院下所有的教研室
        //判断接收的角色中有没有跟他一个教研室
        return roles.stream().anyMatch(role -> {
            Role one = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_id", role));
            return roleIds.contains(role);
        });
    }

    /**
     * 根据项目id查询参与者的用户id
     *
     * @param projectId 考核项目ID
     * @return 查询结果列表
     */
    public List<Integer> getUserListByProject(Integer projectId) {
        //根据年度项目名称查询项目
        FlowTask flowTask = projectMapper.getRadarProjects(projectId);
        HashSet<Integer> uIds = new HashSet<>();
        HashSet<Integer> roleIds = new HashSet<>();
        Optional.ofNullable(flowTask).ifPresent(item -> {
            //添加这个项目流程中参与者的用户Id和角色Id
            uIds.addAll(JSON.parseArray(item.getUId(), Integer.class));
            roleIds.addAll(JSON.parseArray(item.getRoleId(), Integer.class));
        });

        roleIds.forEach(item -> {
            //把角色对应的用户Id添加到用户集合中
            uIds.addAll(userRoleMapper.selectUserIdByRoleId(item));
        });
        return new ArrayList<>(uIds);
    }
}
