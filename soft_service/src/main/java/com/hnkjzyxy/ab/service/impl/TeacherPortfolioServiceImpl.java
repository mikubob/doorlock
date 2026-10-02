package com.hnkjzyxy.ab.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.hnkjzyxy.ab.enums.HnkjzyEncode;
import com.hnkjzyxy.ab.dto.TaskDto;
import com.hnkjzyxy.ab.mapper.ProjectMapper;
import com.hnkjzyxy.ab.mapper.ResultMapper;
import com.hnkjzyxy.ab.mapper.RoleMapper;
import com.hnkjzyxy.ab.mapper.TaskMapper;
import com.hnkjzyxy.ab.mapper.UserMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.TeachAnalysistHomeParam;
import com.hnkjzyxy.ab.params.TeachHomeParam;
import com.hnkjzyxy.ab.service.TeacherPortfolioService;
import com.hnkjzyxy.ab.service.utils.ResultUtils;
import com.hnkjzyxy.ab.vo.RadarChartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * 教师档案统计Service实现类
 */
@Service
public class TeacherPortfolioServiceImpl implements TeacherPortfolioService {
    /**
     * 用户角色关联数据访问接口
     */
    @Autowired
    UserRoleMapper userRoleMapper;
    /**
     * 考核项目数据访问接口
     */
    @Autowired
    ProjectMapper projectMapper;
    /**
     * TaskMapper数据访问接口
     */
    @Autowired
    TaskMapper taskMapper;
    /**
     * 考核结果数据访问接口
     */
    @Autowired
    ResultMapper resultMapper;
    /**
     * 角色数据访问接口
     */
    @Autowired
    RoleMapper roleMapper;
    /**
     * 用户数据访问接口
     */
    @Autowired
    UserMapper userMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public RadarChartVo getradarChart(TeachHomeParam param, User user, String college) {
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
                if (role.getWeight() >= HnkjzyEncode.DEAN.getCode()) {
                    //2.二级学院
                    HashSet<Integer> userIds = new HashSet<>();
                    uIds.forEach(item -> {
                        List<Integer> valRoles = userRoleMapper.getRoles(item);//获得项目中用户的角色
                        List<Integer> userRoles = userRoleMapper.getRoles(user.getUserId());//获得当前登录用户的角色
                        if (user.getCollege().equals(college)) {
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
        }
        return chartVo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RadarChartVo getChartByTeacher(TeachHomeParam param, User user) {
        RadarChartVo chartVo = new RadarChartVo();
        ArrayList<String> metrics = new ArrayList<>();
        ArrayList<Integer> value = new ArrayList<>();
        List<String> taskId = resultMapper.selectTaskByProjectAndTeacher(param.getProjectId(), param.getTeacherId());
        if(!taskId.isEmpty()){

        List<TaskDto> taskDto = taskMapper.selectTeachGroupMetrics(param.getProjectId(), taskId);

        //按照项目的分类获得每个分类的总的分数
//        List<TaskDto> taskDto = taskMapper.selectGroupMetrics(param.getProjectId());

        taskDto.forEach(item -> {
            List<Integer> score = resultMapper.selectScoreByTeacher(param.getProjectId(),param.getTeacherId());
            System.out.println(score);
            Optional.ofNullable(score)
                    .filter(s -> !s.isEmpty())
                    .ifPresent(s -> {
                        s.stream().mapToInt(Integer::intValue).average().ifPresent(averageValue -> {
                            // 调试输出，查看中间值
                            System.out.println("平均分: " + averageValue + ", 总分: " + item.getScore() + ", 比例: " + (averageValue / item.getScore().doubleValue()));

                            metrics.add(item.getCategory());
                            value.add(ResultUtils.calc(averageValue / item.getScore().doubleValue()));
                        });
                    });

        });
        chartVo.setMetrics(metrics);
        chartVo.setValue(value);
        }
        return chartVo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public  List<Map<String, Map<String, Integer>>>  getChartByTeacher(TeachAnalysistHomeParam param, User user) {
        List<Integer> projectIds = resultMapper.getProjectIds(Integer.valueOf(param.getTeacherId()));
        Map<String, Map<String, List<Integer>>> tempData = new HashMap<>();

        projectIds.forEach(pid -> {
            List<String> task_id = resultMapper.selectTaskByProjectAndTeacher(pid, param.getTeacherId());
            if(!task_id.isEmpty()) {

                List<TaskDto> taskDto = taskMapper.selectTeachGroupMetrics(pid, task_id);

                taskDto.forEach(item -> {
                    List<Integer> score = resultMapper.selectScoreByTeacher(pid, param.getTeacherId());

                    Optional.ofNullable(score)
                            .filter(s -> !s.isEmpty())
                            .ifPresent(s -> {
                                double average = s.stream().mapToInt(t -> t).average().orElse(0);
                                Project projectById = projectMapper.getProjectById(String.valueOf(pid));
                                String year = String.valueOf(projectById.getStartTime().getYear() + 1900);
                                int calculatedScore = ResultUtils.calc(average / item.getScore().doubleValue());

                                // 按年份和类别组织数据
                                tempData.computeIfAbsent(year, k -> new HashMap<>())
                                        .computeIfAbsent(item.getCategory(), k -> new ArrayList<>())
                                        .add(calculatedScore);
                            });
                });
            }
        });

// 计算平均值并转换为最终格式
        List<Map<String, Map<String, Integer>>> projectRadarChartVos = tempData.entrySet().stream()
                .sorted(Comparator.comparing(entry -> Integer.parseInt(entry.getKey()))) // 按年份数值排序
                .map(yearEntry -> {
                    Map<String, Integer> categoryScores = yearEntry.getValue().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    categoryEntry -> (int) categoryEntry.getValue().stream()
                                            .mapToInt(Integer::intValue)
                                            .average()
                                            .orElse(0)
                            ));

                    Map<String, Map<String, Integer>> yearMap = new HashMap<>();
                    yearMap.put(yearEntry.getKey(), categoryScores);
                    return yearMap;
                })
                .collect(Collectors.toList());
        return projectRadarChartVos;
    }


}

