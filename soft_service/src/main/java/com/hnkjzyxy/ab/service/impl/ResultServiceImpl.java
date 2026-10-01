package com.hnkjzyxy.ab.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.Enum.HnkjzyEncode;
import com.hnkjzyxy.ab.annotation.RedisCache;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.dto.SubTaskDto;
import com.hnkjzyxy.ab.dto.SubTaskIdDto;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.mapper.ProjectMapper;
import com.hnkjzyxy.ab.mapper.ProjectTaskImportMapper;
import com.hnkjzyxy.ab.mapper.ResultItemMapper;
import com.hnkjzyxy.ab.mapper.ResultMapper;
import com.hnkjzyxy.ab.mapper.RoleMapper;
import com.hnkjzyxy.ab.mapper.TaskMapper;
import com.hnkjzyxy.ab.mapper.UserMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.HomeParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.ResultService;
import com.hnkjzyxy.ab.service.utils.ProjectTaskRules;
import com.hnkjzyxy.ab.utils.FilePathUtils;
import com.hnkjzyxy.ab.utils.PdfToWordConverter;
import com.hnkjzyxy.ab.utils.UploadUtils;
import com.hnkjzyxy.ab.vo.DataVo;
import com.hnkjzyxy.ab.vo.LineDataVo;
import com.hnkjzyxy.ab.vo.LineVo;
import com.hnkjzyxy.ab.vo.PieDataVo;
import com.hnkjzyxy.ab.vo.PieVo;
import com.hnkjzyxy.ab.vo.SubTaskVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

@Service
public class ResultServiceImpl extends ServiceImpl<ResultMapper, Result> implements ResultService {

    /**
     * 评分更新共用的项目锁及生命周期保护服务
     */
    @Resource
    private ProjectTaskGuard projectTaskGuard;
    /**
     * 任务及结果真实归属查询接口
     */
    @Resource
    private ProjectTaskImportMapper projectTaskImportMapper;

    @Resource
    private ResultMapper resultMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private ResultItemMapper resultItemMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private TaskMapper taskMapper;

    @Value("${upload.fileUrl}")
    private String filepath;

    public String getStringWeek(int i) {
        switch (i) {
            case 1:
                return "星期一";
            case 2:
                return "星期二";
            case 3:
                return "星期三";
            case 4:
                return "星期四";
            case 5:
                return "星期五";
            case 6:
                return "星期六";
            case 7:
                return "星期天";
        }
        return "";
    }

    private String[] getAllDateAndWeek() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_WEEK, -1);
        }
        String[] dates = new String[7];
        for (int i = 0; i < 7; i++) {
            dates[i] = (dateFormat.format(calendar.getTime()));
            calendar.add(Calendar.DATE, 1);
        }
        return dates;
    }

    public Integer getYear() {
        Calendar date = Calendar.getInstance();
        String year = String.valueOf(date.get(Calendar.YEAR));
        return Integer.valueOf(year) - 4;
    }

    public List<String> getYears(Integer year) {
        ArrayList<String> years = new ArrayList<>();
        for (int i = year; i >= year - 4; i--) {
            years.add(String.valueOf(i));
        }
        return years;
    }


    @Override
    @RedisCache(key = HnkjxyConstants.COLUMNAR_CHART)
    public DataVo columnarChart(HomeParam param, User user) {
        //判断角色
        Role role = userRoleMapper.getRoleWeight(user.getUserId());
        DataVo dataVo = new DataVo();
        ArrayList<String> x = new ArrayList<>();
        ArrayList<String> y = new ArrayList<>();
        if (role.getWeight().intValue() >= HnkjzyEncode.DIRECTOR.getCode().intValue()) {
            //根据年度项目名称查询项目
            List<FlowTask> flowTaskList = projectMapper.getColumnarProjects(param.getYear(), param.getProjectId());
            if (ObjectUtil.isNotEmpty(flowTaskList)) {
                flowTaskList.forEach(flowTask -> {
                    HashSet<Integer> uIds = new HashSet<>(JSON.parseArray(flowTask.getUId(), Integer.class));
                    List<Integer> roleIds = JSON.parseArray(flowTask.getRoleId(), Integer.class);
                    roleIds.forEach(item -> {
                        uIds.addAll(userRoleMapper.selectUserIdByRoleId(item));
                    });
                    if (ObjectUtil.isNotEmpty(uIds)) {
                        if (role.getWeight().equals(HnkjzyEncode.LEADER.getCode())) {
                            //校领导
                            List<Integer> list = resultMapper.getProjectResult(flowTask.getProjectId(), uIds);
                            OptionalDouble average = list.stream().mapToInt(t -> t.intValue()).average();
                            double avege = 0;
                            if (average.isPresent()) {
                                avege = average.getAsDouble();
                            }
                            x.add(flowTask.getProjectName());
                            y.add(String.valueOf(list.size() == 0 ? 0 : avege / list.size()));
                        } else if (role.getWeight().equals(HnkjzyEncode.DEAN.getCode())) {
                            //二级学院院长
                            //过滤自己教研室的用户数据
                            HashSet<Integer> userIds = new HashSet<>();
                            uIds.forEach(item -> {
                                List<Integer> valRoles = userRoleMapper.getRoles(item);
                                List<Integer> userRoles = userRoleMapper.getRoles(user.getUserId());
                                if (checkDeanRoles(userRoles, valRoles)) {
                                    userIds.add(item);
                                }
                            });
                            if (ObjectUtil.isNotEmpty(userIds)) {
                                List<Integer> list = resultMapper.getProjectResult(flowTask.getProjectId(), userIds);
                                OptionalDouble average = list.stream().mapToInt(t -> t.intValue()).average();
                                double avege = 0;
                                if (average.isPresent()) {
                                    avege = average.getAsDouble();
                                }
                                x.add(flowTask.getProjectName());
                                y.add(String.valueOf(list.size() == 0 ? 0 : avege / list.size()));
                            }
                        } else if (role.getWeight().equals(HnkjzyEncode.DIRECTOR.getCode())) {
                            //教研室主任
                            //过滤自己教研室的用户数据
                            HashSet<Integer> userIds = new HashSet<>();
                            uIds.forEach(item -> {
                                List<Integer> valRoles = userRoleMapper.getRoles(item);
                                List<Integer> userRoles = userRoleMapper.getRoles(user.getUserId());
                                if (checkRoles(userRoles, valRoles)) {
                                    userIds.add(item);
                                }
                            });
                            if (ObjectUtil.isNotEmpty(userIds)) {
                                List<Integer> list = resultMapper.getProjectResult(flowTask.getProjectId(), userIds);
                                OptionalDouble average = list.stream().mapToInt(t -> t.intValue()).average();
                                double avege = 0;
                                if (average.isPresent()) {
                                    avege = average.getAsDouble();
                                }
                                x.add(flowTask.getProjectName());
                                y.add(String.valueOf(list.size() == 0 ? 0 : avege / list.size()));
                            }
                        }
                    }
                });
            }
        } else {
            //个人
            List<FlowTask> flowTaskList = projectMapper.getColumnarProjects(param.getYear(), param.getProjectId());
            HashSet<Integer> userIds = new HashSet<>();
            userIds.add(user.getUserId());
            if (ObjectUtil.isNotEmpty(userIds) && ObjectUtil.isNotEmpty(flowTaskList)) {
                flowTaskList.forEach(flowTask -> {
                    HashSet<Integer> uIds = new HashSet<>(JSON.parseArray(flowTask.getUId(), Integer.class));
                    List<Integer> roleIds = JSON.parseArray(flowTask.getRoleId(), Integer.class);
                    roleIds.forEach(item -> {
                        uIds.addAll(userRoleMapper.selectUserIdByRoleId(item));
                    });
                    if (uIds.contains(user.getUserId())) {
                        List<Integer> list = resultMapper.getProjectResult(flowTask.getProjectId(), userIds);
                        OptionalDouble average = list.stream().mapToInt(t -> t.intValue()).average();
                        double avege = 0;
                        if (average.isPresent()) {
                            avege = average.getAsDouble();
                        }
                        x.add(flowTask.getProjectName());
                        y.add(String.valueOf(list.size() == 0 ? 0 : avege / list.size()));
                    }
                });
            }
        }
        dataVo.setX(x);
        dataVo.setY(y);
        return dataVo;
    }

    @Override
    public Map<String, Object> getList(SubTaskDto dto, User user) {
        //1、判断用户是否为院长的角色
        Integer userId = user.getUserId();

        //TODO 只有院长才能查看和修改
        Role role = userRoleMapper.getRoleWeight(userId);//查到用户的角色
        if (!HnkjzyEncode.DEAN.getName().equals(role.getRoleName())) {
            throw new RuntimeException("该列表只有院长可以查看");
        }

        //2、如果是院长的角色，就返回查询的列表
        HashMap<String, Object> map = new HashMap<>();
        List<SubTaskVo> score = resultMapper.getUsersSubTaskScore(dto.getTitle(), dto.getTaskName(), dto.getTaskCategory());

        map.put("total", score.size());
        map.put("list", score);
        return map;
    }

    @Override
    public Map<String, Object> getLists(SubTaskIdDto dto, User user) {
        //1、判断用户是否为院长的角色
        Integer userId = user.getUserId();

        //TODO 只有院长才能查看和修改
        Role role = userRoleMapper.getRoleWeight(userId);//查到用户的角色
        if (!role.getRoleName().equals(HnkjzyEncode.ADMIN.getName())) {
            if (!HnkjzyEncode.DEAN.getName().equals(role.getRoleName())) {
                throw new RuntimeException("该列表只有院长可以查看");
            }
        }


        //2、如果是院长的角色，就返回查询的列表
        HashMap<String, Object> map = new HashMap<>();
        List<SubTaskVo> score = resultMapper.getUsersSubTaskScoreById(dto);

        map.put("total", score.size());
        map.put("list", score);
        return map;
    }


    /**
     * 下载证据到word文件
     *
     * @param evidences 证据集合
     * @param response  response对象
     */
    @Override
    public void downloadEvidenceToWord(String evidences, HttpServletResponse response) {

        //1、通过pdf的文件的路径，生成word文件（调用工具，先pdf变成image，然后再image变成word）
        File tempFile = null;
        try {
            //创建临时文件word
            tempFile = File.createTempFile("tempFile", ".docx");

            //pdf文件存在的根部路径
            List<String> strings = JSON.parseArray(evidences, String.class);
            String path = FilePathUtils.getRealFilePath("/pdf/file/");//目的就是去除字符串前面的/pdf/file的内容
            List<String> collect = strings.stream().map(item -> {
                return item.substring(path.length());
            }).collect(Collectors.toList());

            //遍历pdf文件，将pdf文件转成word文件
            for (String item : collect) {
                File file = new File(filepath + item);
                File parentFile = file.getParentFile();
                //调用工具类，将pdf文件写入到临时的word文件中
                PdfToWordConverter.pdfFilesToWordFile(parentFile.getAbsolutePath(), tempFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("生成word文件失败！", e);
            throw new RuntimeException(e.getMessage());

        }

        //2、下载word文件到response对象中
        try {
            UploadUtils.download(response, tempFile.getPath());
        } catch (IOException e) {
            log.error("下载word文件失败！", e);
            throw new RuntimeException(e);
        }

        //3、临时的word文件
        if (tempFile.exists()) {
            boolean delete = tempFile.delete();
            if (!delete) {
                System.out.println("临时文件删除失败！");
            } else {
                System.out.println("临时文件删除成功！");
            }
        }
    }

    @Override
    public Map<String, Object> getListByCategoryName(Task param) {
        if (param.getPId() == null) {
            throw new RuntimeException("项目id不能为空");
        }
        if (param.getCategory() == null) {
            throw new RuntimeException("分类名称不能为空");
        }
        List<String> list = taskMapper.getTaskByCategoryName(param.getPId(), param.getCategory());
        HashMap<String, Object> map = new HashMap<>();
        map.put("data", list);
        return map;
    }


    @Override
    public ApiResult getUserResultWeekCount(Integer userId) {
        String[] week = getAllDateAndWeek();
        ArrayList<String> x = new ArrayList<>();
        ArrayList<String> y = new ArrayList<>();
        for (int i = 0; i < week.length; i++) {
            Integer count = resultMapper.getUserResultWeekCount(week[i], userId);
            x.add(getStringWeek(i + 1));
            y.add(count + "");
        }
        DataVo vo = new DataVo();
        vo.setX(x);
        vo.setY(y);
        return ApiResult.ok("data", vo);
    }

    @Override
    //@Cacheable(value = {HnkjxyConstants.PROJECT_SCORE},key = "#year+'-'+#user.getUserId()")
    public LineVo finishProjectScore(Integer year, User user) {
        Role role = userRoleMapper.getRoleWeight(user.getUserId());
        LineVo vo = new LineVo();

        //当前是普通用户
        if (role.getWeight().intValue() < 5) {
            vo = getFinishProjectScore(year, user, 1);
        }

        //当前用户是教研室主任
        if (role.getWeight().equals(5)) {
            vo = getFinishProjectScore(year, user, 2);
        }

        //当前用户高权限用户
        if (role.getWeight().intValue() > 5) {
            //查看每个用户的完成成绩
            vo = getFinishProjectScore(year);
        }
        return vo;
    }

    @Override
    //@Cacheable(value = {HnkjxyConstants.PROJECT_SCALE},key = "#year+'-'+#user.getUserId()")
    //@Cacheable(value = {HnkjxyConstants.PROJECT_SCALE})
    public PieVo finishProjectScale(User user, Integer year) {
        Role role = userRoleMapper.getRoleWeight(user.getUserId());
        PieVo vo = new PieVo();
        //当前是普通用户
        if (role.getWeight().intValue() < 5) {
            vo = getFinishProjectScale(user, year, 1);
        }

        //当前用户是教研室主任
        if (role.getWeight().equals(5)) {
            vo = getFinishProjectScale(user, year, 2);
        }

        //当前用户高权限
        if (role.getWeight().intValue() > 5) {
            //查看每个用户的完成成绩
            vo = getFinishProjectScale(year);
        }
        return vo;
    }

    public LineVo getFinishProjectScore(Integer year) {
        //拿到所有教研室角色信息
        List<Role> roles = roleMapper.getRoleWeight(HnkjzyEncode.DEPARTMENT.getCode());
        //创建折线图对象
        LineVo vo = new LineVo();
        ArrayList<String> legend = new ArrayList<>();
        ArrayList<LineDataVo> series = new ArrayList<>();
        //拿到用户数据信息
        roles.forEach(role -> {
            legend.add(role.getRoleName());
            //创建折线图 value
            LineDataVo dataVo = new LineDataVo();
            dataVo.setName(role.getRoleName());
            //存储近五年的成绩
            ArrayList<String> scores = new ArrayList<>();
            //通过角色id 拿到用户
            List<Integer> list = userRoleMapper.selectUserIdByRoleId(role.getRoleId());
            //拿到用户每年的成绩
            for (int i = year; i >= year - 4; i--) {
                //总成绩
                BigDecimal total = new BigDecimal(0);
                //总数
                Integer count = 0;
                for (Integer item : list) {
                    List<Result> results = resultMapper.selectListByYear(i, item);
                    /*int sum = results.stream().mapToInt(val -> Integer.parseInt(val.getScore())).sum();
                    total = total.add(new BigDecimal(ObjectUtil.isNotNull(sum) ? sum : 0));
                    count += results.size();*/
                }
                scores.add(String.valueOf((total.intValue() > 0 && count > 0) ? total.divide(new BigDecimal(count), 2, BigDecimal.ROUND_DOWN).doubleValue() : new BigDecimal(0).intValue()));
            }
            dataVo.setData(scores);
            series.add(dataVo);
        });
        vo.setXAxis(getYears(year));
        vo.setLegend(legend);
        vo.setSeries(series);
        return vo;
    }

    public LineVo getFinishProjectScore(Integer year, User user, Integer flag) {
        List<Integer> roles = userRoleMapper.getRoles(user.getUserId());
        //拿到跟自己一个教研室的用户信息
        List<User> users = userMapper.selectList(null);
        List<User> userList = new ArrayList<>();
        //普通用户
        if (flag.equals(1)) {
//            userList = userMapper.selectList(new QueryWrapper<User>().eq("user_id",user.getUserId()));
            userList = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getUserId, user.getUserId()));
        } else if (flag.equals(2)) { //主任审批
            userList = users.stream().map(item -> {
                List<Integer> rolesId = userRoleMapper.getRoles(item.getUserId());
                Role role = userRoleMapper.getRoleWeight(item.getUserId());
                if (checkRoles(roles, rolesId) && role.getWeight().intValue() <= 5) {
                    return item;
                }
                return null;
            }).filter(ObjectUtil::isNotNull).collect(Collectors.toList());
        }
        //创建折线图对象
        LineVo vo = new LineVo();
        ArrayList<String> legend = new ArrayList<>();
        ArrayList<LineDataVo> series = new ArrayList<>();
        //拿到用户数据信息
        userList.forEach(userItem -> {
            legend.add(ObjectUtil.isNull(userItem.getNickName()) ? userItem.getUserName() : userItem.getNickName());
            //创建折线图 value
            LineDataVo dataVo = new LineDataVo();
            dataVo.setName(ObjectUtil.isNull(userItem.getNickName()) ? userItem.getUserName() : userItem.getNickName());
            //存储近五年的成绩
            ArrayList<String> scores = new ArrayList<>();
            for (int i = year; i >= year - 4; i--) {
                List<Result> results = resultMapper.selectListByYear(i, userItem.getUserId());
                int sum = results.stream().mapToInt(val -> val.getScore()).sum();
                BigDecimal total = new BigDecimal(ObjectUtil.isNotNull(sum) ? sum : 0);
                String s = String.valueOf((total.intValue() > 0 && results.size() > 0) ? total.divide(new BigDecimal(results.size()), 2, BigDecimal.ROUND_DOWN).doubleValue() : new BigDecimal(0).intValue());
                scores.add(s);
            }
            dataVo.setData(scores);
            series.add(dataVo);
        });
        vo.setXAxis(getYears(year));
        vo.setLegend(legend);
        vo.setSeries(series);
        return vo;
    }

    @Override
    public List<String> getFinishScaleYears(User user) {
        return resultMapper.getFinishScaleYears(null);
    }

    @Override
    public List<String> getFinishProjectYears(User user) {
        return resultMapper.getFinishScaleYears(null);
    }

    /**
     * 做批量修改
     *
     * @param dto  子任务分数列表
     * @param user 当前用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class,
            isolation = Isolation.READ_COMMITTED)
    public void updateSubTaskScore(List<SubTaskIdDto> dto, User user) {
        //1、判断用户是否为院长的角色
        Integer userId = user.getUserId();
        Role role = userRoleMapper.getRoleWeight(userId);//查到用户的角色
        if (!HnkjzyEncode.DEAN.getName().equals(role.getRoleName())) {
            throw new RuntimeException("该列表只有院长可以修改");
        }

        //2、进行批量修改（用原生的sql写1）
        //用来修改，业务不会修改其他的内容了，只会修改这个分数
        dto.stream().forEach(s -> {
            if (s.getUserId() == null || s.getTaskId() == null || s.getProjectId() == null) {
                throw new RuntimeException("没有传入需要的参数");
            }
        });
        // 按真实任务归属取得项目锁，多项目固定升序；原有评分权限规则保持不变。
        SortedSet<Integer> projects = new TreeSet<>();
        if (dto.isEmpty()) throw new ProjectTaskException(400, "评分列表不能为空");
        for (SubTaskIdDto row : dto) {
            Task task = projectTaskImportMapper.task(row.getTaskId());
            if (task == null || !task.getPId().toString().equals(row.getProjectId())) {
                throw new ProjectTaskException(409, "评分任务与项目归属不匹配");
            }
            projects.add(task.getPId());
        }
        for (Integer projectId : projects) projectTaskGuard.lock(projectId);
        for (SubTaskIdDto row : dto) {
            int projectId = ProjectTaskRules.integer(row.getProjectId(), "项目ID");
            Task task = projectTaskImportMapper.task(row.getTaskId());
            if (task == null || task.getPId() != projectId || projectTaskImportMapper.scoreResults(projectId,
                    row.getTaskId(), ProjectTaskRules.integer(row.getUserId(), "用户ID")) == 0) {
                throw new ProjectTaskException(409, "评分结果已失效，请重新加载");
            }
        }
        resultMapper.updateBySubTaskName(dto);

    }

    private PieVo getFinishProjectScale(Integer year) {
        //拿到所有教研室角色信息
        List<Role> roles = roleMapper.getRoleWeight(2);
        //创建饼图对象 PieVo
        PieVo vo = new PieVo();
        ArrayList<PieDataVo> pieDataVos = new ArrayList<>();
        //拿到用户数据信息
        roles.forEach(role -> {
            //创建折线图 value
            PieDataVo dataVo = new PieDataVo();
            dataVo.setName(role.getRoleName());
            //通过角色id 拿到用户
            List<Integer> list = userRoleMapper.selectUserIdByRoleId(role.getRoleId());
            //拿到用户每年的成绩
            int count = 0;
            for (Integer item : list) {
                List<Result> results = resultMapper.selectListByYear(year, item);
                count += results.size();
            }
            dataVo.setValue(count);
            pieDataVos.add(dataVo);
        });
        vo.setData(pieDataVos);
        return vo;
    }

    private PieVo getFinishProjectScale(User user, Integer year, Integer flag) {
        List<Integer> roles = userRoleMapper.getRoles(user.getUserId());
        //拿到跟自己一个教研室的用户信息
        List<User> users = userMapper.selectList(null);
        List<User> userList = new ArrayList<>();
        if (flag == 1) { //普通用户
            userList = userMapper.selectList(new QueryWrapper<User>().eq("user_id", user.getUserId()));
        } else if (flag == 2) { //主任审批
            userList = users.stream().map(item -> {
                List<Integer> rolesId = userRoleMapper.getRoles(item.getUserId());
                Role role = userRoleMapper.getRoleWeight(item.getUserId());
                if (checkRoles(roles, rolesId) && role.getWeight().intValue() <= 5) {
                    return item;
                }
                return null;
            }).filter(ObjectUtil::isNotNull).collect(Collectors.toList());
        }
        //创建饼图对象 PieVo
        PieVo vo = new PieVo();
        ArrayList<PieDataVo> pieDataVos = new ArrayList<>();
        //拿到用户数据信息
        userList.forEach(userItem -> {
            //创建饼图 value
            PieDataVo dataVo = new PieDataVo();
            dataVo.setName(userItem.getNickName());
            //本年度完成的
            List<Result> results = resultMapper.selectListByYear(year, userItem.getUserId());
            //本年度未完成的
            dataVo.setValue(results.size());
            pieDataVos.add(dataVo);
        });
        vo.setData(pieDataVos);
        return vo;
    }

    /**
     * 判断是否属于同一教研室
     */
    public Boolean checkRoles(List<Integer> roles, List<Integer> roleIds) {
        //除了普通用户角色之外 并且是一个教研室的 weight 2
        return roles.stream().anyMatch(item -> {
            Role one = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_id", item));
            return one.getWeight().equals(2) && roleIds.contains(item);
        });
    }


    /**
     * 判断二级学院的多个教研室
     *
     * @param roles
     * @param roleIds
     * @return
     */
    public Boolean checkDeanRoles(List<Integer> roles, List<Integer> roleIds) {
        return roles.stream().anyMatch(item -> {
            Role one = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_id", item));
            return roleIds.contains(item);
        });
    }

}
