package com.hnkjzyxy.ab.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.enums.HnkjzyEncode;
import com.hnkjzyxy.ab.annotation.RedisCache;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.dto.SubTaskDto;
import com.hnkjzyxy.ab.dto.SubTaskIdDto;
import com.hnkjzyxy.ab.dto.ResultAccessScope;
import com.hnkjzyxy.ab.exception.AuthPermissionException;
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
import com.hnkjzyxy.ab.service.security.ResultPermissionPolicy;
import com.hnkjzyxy.ab.service.utils.ProjectTaskRules;
import com.hnkjzyxy.ab.vo.DataVo;
import com.hnkjzyxy.ab.vo.LineDataVo;
import com.hnkjzyxy.ab.vo.LineVo;
import com.hnkjzyxy.ab.vo.PieDataVo;
import com.hnkjzyxy.ab.vo.PieVo;
import com.hnkjzyxy.ab.vo.SubTaskVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * 考核结果Service实现类
 */
@Service
public class ResultServiceImpl extends ServiceImpl<ResultMapper, Result> implements ResultService {

    /**
     * 三个子任务成绩入口共用的查看、评分权限及学院范围策略
     */
    @Resource
    private ResultPermissionPolicy resultPermissionPolicy;

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

    /**
     * 考核结果数据访问接口
     */
    @Resource
    private ResultMapper resultMapper;
    /**
     * 用户角色关联数据访问接口
     */
    @Resource
    private UserRoleMapper userRoleMapper;
    /**
     * 审批明细数据访问接口
     */
    @Resource
    private ResultItemMapper resultItemMapper;
    /**
     * 用户数据访问接口
     */
    @Resource
    private UserMapper userMapper;
    /**
     * 角色数据访问接口
     */
    @Resource
    private RoleMapper roleMapper;
    /**
     * 考核项目数据访问接口
     */
    @Resource
    private ProjectMapper projectMapper;
    /**
     * TaskMapper数据访问接口
     */
    @Resource
    private TaskMapper taskMapper;

    /**
     * 将一至七的编号转换为中文星期名称
     *
     * @param i 星期编号，一至七
     * @return 中文星期名称；编号不在一至七时返回空字符串
     */
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

    /**
     * 生成本周周一至周日的日期字符串
     *
     * @return 日期字符串数组
     */
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

    /**
     * 获取当前年度减四的统计起始年度
     *
     * @return 当前年份减四的年度
     */
    public Integer getYear() {
        Calendar date = Calendar.getInstance();
        String year = String.valueOf(date.get(Calendar.YEAR));
        return Integer.valueOf(year) - 4;
    }

    /**
     * 生成年份及其之前四年的年度列表
     *
     * @param year 统计年度
     * @return 查询结果列表
     */
    public List<String> getYears(Integer year) {
        ArrayList<String> years = new ArrayList<>();
        for (int i = year; i >= year - 4; i--) {
            years.add(String.valueOf(i));
        }
        return years;
    }


    /**
     * {@inheritDoc}
     */
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

    /**
     * 按名称条件查询当前操作人可查看的子任务成绩
     * <p>
     * 旧查询与新查询使用相同查看策略：管理员可查看全部学院，院长仅查看当前所属学院。
     * 先核验当前数据库账号及全部有效角色，再执行带学院范围的查询，保留旧接口成功结构。
     * </p>
     *
     * @param dto 项目名称、任务名称及任务分类等旧查询条件
     * @param user 认证链路取得的操作人，不是查询目标用户
     * @return 包含 {@code total} 和 {@code list} 的结果，越界或无匹配数据时为空列表
     * @throws AuthPermissionException 身份无效时返回401；无查看权限或院长学院配置异常时返回403
     */
    @Override
    public Map<String, Object> getList(SubTaskDto dto, User user) {
        // 校验子任务成绩查看权限，范围来自当前数据库账号。
        ResultAccessScope scope = resultPermissionPolicy.requireView(user);
        HashMap<String, Object> map = new HashMap<>();
        // 范围在SQL中参与过滤，total只统计本次可见数据，不先查全校后在内存中过滤。
        List<SubTaskVo> score = resultMapper.getUsersSubTaskScore(dto.getTitle(), dto.getTaskName(), dto.getTaskCategory(), scope);

        map.put("total", score.size());
        map.put("list", score);
        return map;
    }

    /**
     * 按项目、用户及任务条件查询当前操作人可查看的子任务成绩
     * <p>
     * 使用与旧查询相同的角色及学院策略，DTO 中的用户ID只用于筛选查询目标。
     * 指定其他学院的用户时正常返回空结果，不通过响应泄露目标是否存在。
     * </p>
     *
     * @param dto 项目ID、目标用户ID、任务条件及最低分数等查询条件
     * @param user 认证链路取得的操作人，不是DTO中的目标用户
     * @return 包含 {@code total} 和 {@code list} 的结果，越界或无匹配数据时为空列表
     * @throws AuthPermissionException 身份无效时返回401；无查看权限或院长学院配置异常时返回403
     */
    @Override
    public Map<String, Object> getLists(SubTaskIdDto dto, User user) {
        // 新旧查询使用同一查看策略。
        ResultAccessScope scope = resultPermissionPolicy.requireView(user);
        HashMap<String, Object> map = new HashMap<>();
        // DTO只传递筛选条件，scope独立传递已核验的服务端授权范围。
        List<SubTaskVo> score = resultMapper.getUsersSubTaskScoreById(dto, scope);

        map.put("total", score.size());
        map.put("list", score);
        return map;
    }


    /**
     * {@inheritDoc}
     */
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


    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * 汇总指定年度的项目完成评分数据
     *
     * @param year 统计年度
     * @return 折线图统计数据
     */
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

    /**
     * 汇总指定年度的项目完成评分数据
     *
     * @param year 统计年度
     * @param user 当前用户
     * @param flag 处理标记
     * @return 折线图统计数据
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getFinishScaleYears(User user) {
        return resultMapper.getFinishScaleYears(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getFinishProjectYears(User user) {
        return resultMapper.getFinishScaleYears(null);
    }

    /**
     * 在院长当前学院范围内批量修改子任务成绩
     * <p>
     * 授权通过后先校验完整请求，再按真实项目ID升序取得项目锁、按目标用户ID升序取得用户锁。
     * 全部目标的学院及结果归属均有效后才执行批量写入；任一越界或失效项使整批拒绝。
     * 使用 READ_COMMITTED 事务并对异常回滚，保留既有任务归属及项目生命周期保护。
     * </p>
     *
     * @param dto 非空评分列表，包含目标用户ID、项目ID、字符串任务ID及待更新分数
     * @param user 认证链路取得的操作人，必须具有有效院长角色及规范学院资料
     * @throws AuthPermissionException 身份无效时返回401；无评分权限、操作人学院异常，
     *                                或任一目标学院缺失、不匹配时返回403
     * @throws ProjectTaskException 列表为空或整数ID非法时返回400；任务归属或结果失效时返回409；
     *                              项目锁校验失败时保留对应项目异常
     * @throws RuntimeException 任一评分项为空或缺少必要ID时抛出，保留原有参数错误契约
     */
    @Override
    @Transactional(rollbackFor = Exception.class,
            isolation = Isolation.READ_COMMITTED)
    public void updateSubTaskScore(List<SubTaskIdDto> dto, User user) {
        // 授权拒绝发生在业务查询、项目锁及更新之前。
        ResultAccessScope scope = resultPermissionPolicy.requireEdit(user);
        if (dto == null || dto.isEmpty()) throw new ProjectTaskException(400, "评分列表不能为空");
        // 先校验整批ID，避免处理到后面的非法元素时才发现问题；用户去重便于固定加锁次序。
        SortedSet<Integer> targetIds = new TreeSet<>();
        for (SubTaskIdDto row : dto) {
            if (row == null || row.getUserId() == null || row.getTaskId() == null || row.getProjectId() == null) {
                throw new RuntimeException("没有传入需要的参数");
            }
            ProjectTaskRules.integer(row.getProjectId(), "项目ID");
            // 任务ID沿用字符串（含既有雪花ID），只将实际为int的项目和用户ID解析为整数。
            targetIds.add(ProjectTaskRules.integer(row.getUserId(), "用户ID"));
        }
        // 使用数据库任务的真实项目归属，不能仅信任客户端项目ID；TreeSet固定多项目锁顺序。
        SortedSet<Integer> projects = new TreeSet<>();
        for (SubTaskIdDto row : dto) {
            Task task = projectTaskImportMapper.task(row.getTaskId());
            if (task == null || !task.getPId().toString().equals(row.getProjectId())) {
                throw new ProjectTaskException(409, "评分任务与项目归属不匹配");
            }
            projects.add(task.getPId());
        }
        for (Integer projectId : projects) projectTaskGuard.lock(projectId);
        // 固定项目锁→用户锁顺序，学院调整必须等待评分事务完成。
        for (Integer targetId : targetIds) {
            User target = resultMapper.lockScoreTargetUser(targetId);
            if (target == null) throw new ProjectTaskException(409, "评分结果已失效，请重新加载");
            // 不按目标启用状态过滤历史成绩；学院必须精确相等，缺失学院同样整批拒绝。
            if (!scope.getCollege().equals(target.getCollege())) {
                throw new AuthPermissionException(403, "无权限修改其他学院成绩");
            }
        }
        // 持有项目及用户锁后复查任务归属和结果存在性，保留T-04的失效结果保护。
        for (SubTaskIdDto row : dto) {
            int projectId = ProjectTaskRules.integer(row.getProjectId(), "项目ID");
            Task task = projectTaskImportMapper.task(row.getTaskId());
            if (task == null || task.getPId() != projectId || projectTaskImportMapper.scoreResults(projectId,
                    row.getTaskId(), ProjectTaskRules.integer(row.getUserId(), "用户ID")) == 0) {
                throw new ProjectTaskException(409, "评分结果已失效，请重新加载");
            }
        }
        // 完整校验后才写入，SQL再次限定学院；合法同分数提交不按影响行数误判为失败。
        resultMapper.updateBySubTaskName(dto, scope);

    }

    /**
     * 汇总指定年度的项目完成占比数据
     *
     * @param year 统计年度
     * @return 饼图统计数据
     */
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

    /**
     * 汇总指定年度的项目完成占比数据
     *
     * @param user 当前用户
     * @param year 统计年度
     * @param flag 处理标记
     * @return 饼图统计数据
     */
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
     *
     * @param roles 当前用户角色ID集合
     * @param roleIds 目标角色ID集合
     * @return 操作或条件校验结果
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
     * @param roles 当前用户角色ID集合
     * @param roleIds 目标角色ID集合
     * @return 操作或条件校验结果
     */
    public Boolean checkDeanRoles(List<Integer> roles, List<Integer> roleIds) {
        return roles.stream().anyMatch(item -> {
            Role one = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_id", item));
            return roleIds.contains(item);
        });
    }

}
