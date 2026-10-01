package com.hnkjzyxy.ab.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.Enum.HnkjzyEncode;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.mapper.FlowMapper;
import com.hnkjzyxy.ab.mapper.FlowTaskMapper;
import com.hnkjzyxy.ab.mapper.InfoMapper;
import com.hnkjzyxy.ab.mapper.NoticeMapper;
import com.hnkjzyxy.ab.mapper.ProjectItemMapper;
import com.hnkjzyxy.ab.mapper.ProjectMapper;
import com.hnkjzyxy.ab.mapper.ResultMapper;
import com.hnkjzyxy.ab.mapper.RoleMapper;
import com.hnkjzyxy.ab.mapper.TaskMapper;
import com.hnkjzyxy.ab.mapper.UserMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.Flow;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.ResultExtend;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.model.UserRole;
import com.hnkjzyxy.ab.params.ProjectParam;
import com.hnkjzyxy.ab.params.ProjectQueryParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.ProjectTaskImportService;
import com.hnkjzyxy.ab.service.ResultExtendService;
import com.hnkjzyxy.ab.service.ResultService;
import com.hnkjzyxy.ab.service.TaskService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.service.utils.ExcelUtils;
import com.hnkjzyxy.ab.service.utils.PageUtils;
import com.hnkjzyxy.ab.service.utils.ProjectTaskRules;
import com.hnkjzyxy.ab.service.utils.TaskTreeUtils;
import com.hnkjzyxy.ab.vo.FlowTaskVo;
import com.hnkjzyxy.ab.vo.ProjectItemVo;
import com.hnkjzyxy.ab.vo.ProjectVo;
import com.hnkjzyxy.ab.vo.ResultVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * 要考虑学校，二级学院，教研室主任，普通老师  四级权限
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private FlowMapper flowMapper;
    @Resource
    private UserService userService;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private InfoMapper infoMapper;
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private ResultMapper resultMapper;
    @Autowired
    private ResultService resultService;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private FlowTaskMapper flowTaskMapper;
    @Resource
    private TaskTreeUtils taskTreeUtils;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private ResultExtendService resultExtendService;
    @Resource
    private TaskService taskService;
    @Resource
    private ExcelUtils excelUtils;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Resource
    private ProjectItemMapper projectItemMapper;

    /**
     * 项目任务锁与生命周期保护服务
     */
    @Resource
    private ProjectTaskGuard projectTaskGuard;
    /**
     * 项目子项任务导入与来源维护服务
     */
    @Resource
    private ProjectTaskImportService projectTaskImportService;


    //获取项目列表
    @Override
    //@Cacheable(value = {HnkjxyConstants.PROJECTS},key = "#param.getUserId()+'-'+#param.getYear()+'-'+#param.getPage()+'-'+#param.getLimit()+'-'+#param.getTitle()",sync = true)
    public Map<String, Object> getProjectList(ProjectParam param) {
        Page<Project> page = new Page<>(param.getPage(), param.getLimit());
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(param.getYear())) {
            wrapper
                    .ge(Project::getStartTime, param.getYear().concat("-01-01 00:00:00"))
                    .le(Project::getStartTime, param.getYear().concat("-12-31 23:59:59"));
        }
        wrapper.ne(Project::getStatus, 3);
        Role role = userRoleMapper.getRoleWeight(param.getUserId());
        //判断当前用户是否拥有管理员权限
        if (role.getWeight().intValue() < 100) {
            wrapper.eq(Project::getCreateName, param.getCreateName());
        }
        //项目标题
        if (StrUtil.isNotBlank(param.getTitle())) {
            wrapper.like(Project::getTitle, param.getTitle());
        }
        wrapper.orderByDesc(Project::getId);
        this.page(page, wrapper);//这里的status为0是默认的，表示还没有发布；这是查询创建的项目的接口
        List<Project> projectList = page.getRecords();
        taskTreeUtils.buildProject(projectList);//这个没有起到作用
        HashMap<String, Object> map = new HashMap<>();
        map.put("total", page.getTotal());
        map.put("list", projectList);
        return map;
    }

    //获取用户项目列表
    @Override
    //@RedisCache(key = HnkjxyConstants.USER_PROJECTS)
    //@Cacheable(value = {HnkjxyConstants.USER_PROJECTS},key = "#param.getUserId()+'-'+#param.getYear()+'-'+#param.getPage()+'-'+#param.getLimit()+'-'+#param.getTitle()",sync = true)
    public Map<String, Object> getUserProjectList(ProjectParam param) {
        Map<String, Object> map = new HashMap<>();
        map.put("total", 0);
        map.put("list", new ArrayList<>());
        //通过用户角色&用户id拿到流程项目id
        List<Integer> pIds = findPidByRoleIdFromFlow(param.getUserId());//拿到流程下的项目id
        if (ObjectUtil.isNotEmpty(pIds) && pIds.size() > 0) {
            return getUserProjectByPage(pIds, param);
        }
        return map;
    }

    /**
     * 获取用户角色信息
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    @Override
    public List<Integer> getRoles(Integer userId) {
        //获取当前用户角色id
        return userRoleMapper.getRoles(userId);
    }

    /**
     * 根据流程拿到项目id
     */
    public List<Integer> findPidByRoleIdFromFlow(Integer userId) {   //当前用户的角色列表和用户ID
        List<Integer> roles = getRoles(userId);//获得该用户的角色列表
        List<FlowTaskVo> list = flowTaskMapper.getFlowTaskList("CC");//查看所有的流程任务

        //判断流程任务中是否有包含该用户的角色或者是包含改用户的id
        return list.stream().map(item -> {
            //拿到流程下的userIds和roleIds
            List<Integer> userIds = JSONObject.parseArray(item.getUId(), Integer.class), roleIds = JSONObject.parseArray(item.getRoleId(), Integer.class);
            //判断是否包含该用户
            if (userIds.size() > 0 && userIds.contains(userId)) {
                return item.getPId();
            }
            //判断是否包含角色
            if (roleIds.size() > 0 && roles.size() > 0 && containsRole(roles, roleIds)) {
                return item.getPId();
            }
            return null;

            //返回的是流程下的项目id
        }).filter(ObjectUtil::isNotNull).distinct().collect(Collectors.toList());
    }

    /**
     * 查看用户是否包含某个角色
     */
    public Boolean containsRole(List<Integer> roles, List<Integer> roleIds) {

        //第一参数是用户所拥有的角色，第二个是流程所需要的角色
        for (Integer role : roles) {
            //如果用户所拥有的角色包含流程所需要的角色，则返回true
            if (role != 1 && roleIds.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取项目列表
     *
     * @param pIds  该用户所参与的流程下的项目id
     * @param param 用户的id
     * @return
     */

    public Map<String, Object> getUserProjectByPage(List<Integer> pIds, ProjectParam param) {
        //拿到当前用户所有项目列表
        Page<Project> page = new Page<>(param.getPage(), param.getLimit());
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(param.getYear())) {
            wrapper
                    .ge(Project::getStartTime, param.getYear().concat("-01-01 00:00:00"))
                    .le(Project::getStartTime, param.getYear().concat("-12-31 23:59:59"));
        }
        //拿到项目id
        wrapper.in(Project::getId, pIds);
        //判断项目是否开始
        wrapper.le(Project::getStartTime, new Date(System.currentTimeMillis()));
        //判断项目是否发布状态
        wrapper.eq(Project::getStatus, 1);
        //判断项目是否删除状态
        wrapper.ne(Project::getStatus, 3);
        //模糊查询项目名称
        if (StrUtil.isNotBlank(param.getTitle())) {
            wrapper.like(Project::getTitle, param.getTitle());
        }
        wrapper.orderByDesc(Project::getId);
        this.page(page, wrapper);
        //拿到项目列表
        List<Project> projectList = page.getRecords();
        projectList.forEach(item -> {
            //设置状态
            item.setStatus(checkProjectStatus(item.getId(), param.getUserId(), item));
        });
        /*//拿到项目任务
        taskTreeUtils.buildProject(projectList);*/
        HashMap<String, Object> map = new HashMap<>();
        map.put("total", page.getTotal());
        map.put("list", projectList);
        return map;
    }

    /**
     * 设置项目的状态
     *
     * @param pId  项目id
     * @param uId  用户id
     * @param item 项目对象
     * @return
     */
    public Integer checkProjectStatus(Integer pId, Integer uId, Project item) {

        //获得用户填写项目的第一个结果
        /*Integer isFinish = resultMapper.selectResultByPIdAndUId(uId, pId);*/
        Result result = resultMapper.selectResult(uId, pId);

        //判断是否已过期，已过期不能回答
        if (checkProjectOverDue(item) && ObjectUtil.isNull(result)) {
            return 2;
        }

        //未完成
        if (ObjectUtil.isNull(result) || result.getIsFinish().equals(3)
                || result.getIsFinish().equals(5) || result.getIsFinish().equals(0)) {
            return 0;
        }

        //已完成考核
        if (result.getIsFinish().equals(1)) {
            return 1;
        }

        //待审批
        if (result.getIsFinish().equals(4)) {
            return 4;
        }

        //未完成
        return 0;
    }

    //获取最近结束的哪个项目信息
    @Override
    //@Cacheable(value = {HnkjxyConstants.END_PROJECT},key = "#userId")
    public Project getEndProject(Integer userId) {
        List<Integer> pIds = findPidByRoleIdFromFlow(userId);
        if (ObjectUtil.isNotEmpty(pIds) && pIds.size() > 0) {
            LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Project::getEndTime, new Date());
            wrapper.orderByAsc(Project::getEndTime);
            wrapper.le(Project::getStartTime, new Date());
            wrapper.in(Project::getId, pIds);
            wrapper.eq(Project::getStatus, 1);
            List<Project> list = this.list(wrapper);
            for (Project project : list) {
                Result result = resultMapper.selectResult(userId, project.getId());
                if (!checkProjectOverDue(project) && (ObjectUtil.isNull(result) || result.getIsFinish().equals(3) || result.getIsFinish().equals(5) || result.getIsFinish().equals(0))) {
                    return project;
                }
            }
        }
        return new Project();
    }

    /**
     * 创建项目
     *
     * @param project 项目信息
     * @param user    当前用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = {HnkjxyConstants.PROJECTS, HnkjxyConstants.PROJECT_YEAR, HnkjxyConstants.PROJECT_BY_ID}, allEntries = true)
    public void addProject(Project project, User user) {
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    if (ObjectUtil.isNull(user)) {
                        throw new RuntimeException("用户不能为空！");
                    }
                    //创建项目
                    if (ObjectUtil.isNull(project.getId())) {
                        project.setStatus(0);
                        project.setSendName(user.getNickName());
                        project.setCreateName(user.getUserName());
                    } else { //修改项目
                        Project stored = projectTaskGuard.lock(project.getId());
                        projectTaskGuard.manage(stored, user);
                        projectTaskGuard.mutable(stored);
                    }
                    try {
                        //判断是否包含该项目名称
                        Integer count = projectMapper.selectProjectName(project.getTitle());
                        if (count > 0) {
                            throw new RuntimeException("项目名称已存在！");
                        }
                        //项目
                        projectMapper.insert(project);
                        projectTaskGuard.lock(project.getId());
                        projectTaskGuard.initialize(project.getId());
                        excelUtils.readTaskExcel(project.getFile(), project.getId());
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    //根据id获取项目
    @SuppressWarnings("unchecked")
    @Override
    //@Cacheable(value = {HnkjxyConstants.PROJECT_BY_ID},key = "#id",sync = true)
    public Project getProjectById(String id, User user) {
        Project project = projectMapper.getProjectById(id);
        if (ObjectUtil.isNull(project)) {
            throw new RuntimeException("没有找到该项目！");
        }
        //拿到任务
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(Task::getPId, project.getId());
        //List<Task> tasks = taskMapper.selectList(taskWrapper);

        List<Result> results = resultMapper.findByUIdAndPID(user.getUserId(), Integer.parseInt(id));
        results.forEach(item -> {
            item.setTask(taskService.getById(item.getTaskId()));
            item.setEvidenceList(JSON.parseArray(item.getEvidence(), String.class));
            List<ResultExtend> resultExtends = resultExtendService.list(new QueryWrapper<ResultExtend>().eq("result_id", item.getId()));
            resultExtends.forEach(val -> val.setEvidenceList(JSON.parseArray(val.getEvidence(), String.class)));
            item.setResultExtends(resultExtends);
        });

        project.setResults(results);
        //project.setTask(tasks);
        return project;
    }

    //发布项目
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = {HnkjxyConstants.USER_PROJECT_YEAR, HnkjxyConstants.RADAR_CHART,
            HnkjxyConstants.COLUMNAR_CHART, HnkjxyConstants.USER_PROJECTS, HnkjxyConstants.PROJECT_YEAR,
            HnkjxyConstants.NOTICE_LIST, HnkjxyConstants.PROJECTS, HnkjxyConstants.APPROVE_LIST,
            HnkjxyConstants.APPROVE_YEARS, HnkjxyConstants.END_PROJECT, HnkjxyConstants.ASSESS_LIST}, allEntries = true)
    public void publishProject(String id, User user) {
        Project locked = projectTaskGuard.lock(ProjectTaskRules.integer(id, "项目ID"));
        projectTaskGuard.manage(locked, user);
        projectTaskGuard.publication(locked);
        if (ObjectUtil.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        //先判断该项目是否创建流程,未创建流程不能发布
        LambdaQueryWrapper<Flow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Flow::getPId, id).eq(Flow::getUserId, user.getUserId());
        Integer count = flowMapper.selectCount(wrapper);
        //大于0说明流程已创建
        if (count.intValue() <= 0) {
            throw new RuntimeException("请先创建流程后！再发布项目！");
        }
        //判断项目是否已过期
        Project project = locked;
        if (ObjectUtil.isNull(project) || checkProjectOverDue(project)) {
            throw new RuntimeException("发布失败！项目不存在或项目已过期！");
        }
        projectMapper.publishProject(id);
        if (ObjectUtil.isNull(user.getNickName())) {
            throw new RuntimeException("用户昵称不能为空！");
        }
        noticeMapper.noticeInfo(user.getNickName(), user.getNickName() + " - 发布了一个项目 - " + project.getTitle());
    }

    /**
     * 判断项目是否已过期
     */
    public Boolean checkProjectOverDue(Project item) {
        //比较：前者大于后者
        int i = item.getEndTime().compareTo(new Date());
        int j = item.getEndTime().compareTo(item.getStartTime());
        //大于零未过期，小于等于零已过期
        return i <= 0 || j <= 0;
    }

    //提交项目结果
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = {HnkjxyConstants.APPROVE_LIST, HnkjxyConstants.RADAR_CHART, HnkjxyConstants.ASSESS_LIST,
            HnkjxyConstants.COLUMNAR_CHART, HnkjxyConstants.NOT_SUB_LIST, HnkjxyConstants.RESULT_DETAIL, HnkjxyConstants.RESULT_LIST,
            HnkjxyConstants.END_PROJECT, HnkjxyConstants.USER_PROJECTS, HnkjxyConstants.NOTICE_LIST}, allEntries = true)
    public void resultProject(ResultVo result, User user) {
        //判断项目是否已过期
        Project project = projectTaskGuard.lock(result.getProjectId());
        projectTaskGuard.validateTasks(result.getProjectId(), user.getUserId(), result.getResults(), false);

        if (ObjectUtil.isNull(project)) {
            throw new RuntimeException("项目不存在！");
        }

        if (checkProjectOverDue(project)) {
            throw new RuntimeException("已过期不能进行提交！");
        }

        //判断该项目是否已经提交结果
        Result selected = resultMapper.selectResult(user.getUserId(), result.getProjectId());
        if (ObjectUtil.isNotNull(selected) && selected.getIsFinish().equals(4)) {
            throw new RuntimeException("您已经提交过项目结果了！");
        }

        if (ObjectUtil.isEmpty(result.getResults())) {
            throw new RuntimeException("项目结果不能为空！");
        }

        result.getResults().forEach(item -> {
            item.setEvidenceCount(item.getEvidenceList().size());
            item.setUId(user.getUserId());
            item.setIsFinish(result.getIsFlag());
            item.setEvidence(JSON.toJSONString(item.getEvidenceList()));
            if (!resultService.saveOrUpdate(item)) throw new IllegalStateException("结果保存失败，已回滚");
            resultExtendService.remove(new QueryWrapper<ResultExtend>().eq("result_id", item.getId()));
            if (ObjectUtil.isNotEmpty(item.getResultExtends())) {
                item.getResultExtends().forEach(val -> {
                    val.setUId(item.getUId());
                    val.setTaskId(item.getTaskId());
                    val.setResultId(item.getId());
                    val.setId(null); // 原扩展已删除，重新生成主键而不复用客户端ID。
                    val.setEvidence(JSON.toJSONString(val.getEvidenceList()));
                    if (!resultExtendService.save(val)) throw new IllegalStateException("扩展项保存失败，已回滚");
                });
            }
        });

        if (ObjectUtil.isNull(user.getNickName())) {
            throw new RuntimeException("用户昵称不能为空！");
        }

        noticeMapper.noticeInfo(user.getNickName(), user.getNickName().concat(" - 提交了项目结果 - " + project.getTitle()));
    }


    /**
     * 暂存答案
     *
     * @param result 暂存的结果
     * @param user   当前用户
     */
    @Override
    public void projectStaging(ResultVo result, User user) {
        projectTaskGuard.recordStaging(result.getProjectId(), user, result.getResults());
        String key = user.getUserName().concat("-" + result.getProjectId());
        try {
            redisTemplate.opsForValue().set(key, JSONObject.toJSONString(result), 15, TimeUnit.DAYS);
        } catch (RuntimeException e) {
            throw new ProjectTaskException(503, "暂存缓存写入失败；任务冻结标记已保留，请重试保存");
        }
    }

    /**
     * 拿到暂存答案
     *
     * @param user 当前用户
     * @param pId  项目ID
     * @return 暂存的结果
     */
    @Override
    public ResultVo getProjectStaging(User user, Integer pId) {
        String key = user.getUserName().concat("-" + pId);
        if (redisTemplate.hasKey(key)) {
            Object o = redisTemplate.opsForValue().get(key);
            if (ObjectUtil.isNull(o)) {
                return null;
            }
            ResultVo result = JSONObject.parseObject(o.toString(), ResultVo.class);
            return result;
        }
        return null;
    }


    @Override
    //@Cacheable(value = {HnkjxyConstants.PROJECT_YEAR},key = "#param.getUserId()",sync = true)
    public List<String> getProjectYears(ProjectParam param) {
        Integer userId = param.getUserId();
        List<Integer> roles = getRoles(userId);
        if (roles.contains(2)) {
            param.setCreateName(null);
        }
        HashSet<String> set = new HashSet<>(projectMapper.getProjectYears(param.getCreateName()));
        return new ArrayList<String>(set);
    }

    @Override
    //@Cacheable(value = {HnkjxyConstants.USER_PROJECT_YEAR},key = "#param.getUserId()",sync = true)
    public List<String> getUserProjectYears(ProjectParam param) {
        //通过用户id拿到项目id
        List<Integer> pIds = findPidByRoleIdFromFlow(param.getUserId());
        if (ObjectUtil.isNotEmpty(pIds) && pIds.size() > 0) {
            QueryWrapper<Project> wrapper = new QueryWrapper<>();
            wrapper
                    .ne("status", 3)
                    .in("id", pIds)
                    .orderByDesc("startTime");
            HashSet<String> set = new HashSet<>(projectMapper.getUserProjectYears(wrapper));
            return new ArrayList<String>(set);
        }
        return null;
    }

    @Override
    //@RedisCache(key = HnkjxyConstants.ASSESS_LIST)
    public ApiResult getProjectAssessList(ProjectQueryParam param, User user) {
        //获取当前用户最高角色
        Role role = userRoleMapper.getRoleWeight(user.getUserId());
        //判断用户权限
        //获取所有项目流程id
        //List<ProjectVo> projectVos = projectMapper.getProjectAssessList();
        List<ProjectVo> projectVos = new ArrayList<>();
        //查询所有项目接收的所有用户id
        //TODO
        if (role.getWeight().intValue() >= HnkjzyEncode.LEADER.getCode()) {
            //该用户是校级领导，可以看所有的数据
            List<FlowTask> assessFlows = projectMapper.getProjectAssessFlow(param.getStartTime(), param.getEndTime(), param.getProjectId());
            assessFlows.forEach(flow -> {
                HashSet<Integer> uIds = new HashSet<>(JSON.parseArray(flow.getUId(), Integer.class));
                List<Integer> roleIds = JSON.parseArray(flow.getRoleId(), Integer.class);
                //通过角色id拿到用户id
                roleIds.forEach(item -> {
                    uIds.addAll(userRoleMapper.selectUserIdByRoleId(item));
                });
                uIds.forEach(item -> {
                    addProjectVos(param, projectVos, flow, item);
                });
            });
        } else if (role.getWeight().intValue() == HnkjzyEncode.DEAN.getCode()) {
            //该用户为二级学院院长，可以看该二级学院下的教研室的所有的数据
            List<FlowTask> assessFlows = projectMapper.getProjectAssessFlow(param.getStartTime(), param.getEndTime(), param.getProjectId());
            assessFlows.forEach(flow -> {
                HashSet<Integer> uIds = new HashSet<>(JSON.parseArray(flow.getUId(), Integer.class));
                List<Integer> roleIds = JSON.parseArray(flow.getRoleId(), Integer.class);
                //通过角色id拿到用户id
                roleIds.forEach(item -> {
                    uIds.addAll(userRoleMapper.selectUserIdByRoleId(item));
                });
                uIds.forEach(item -> {
                    //判断是否为二级学院下面的一个教研室
                    //获取当前用户的角色
                    List<Integer> valRoles = userRoleMapper.getRoles(item);
                    List<Integer> userRoles = userRoleMapper.getRoles(user.getUserId());
                    if (checkDeanRoles(userRoles, valRoles)) {
                        addProjectVos(param, projectVos, flow, item);
                    }
                });
            });
        } else if (role.getWeight().equals(HnkjzyEncode.DIRECTOR.getCode())) {
            //教研室主任
            //拿到自己教研室的用户数据
            List<FlowTask> assessFlows = projectMapper.getProjectAssessFlow(param.getStartTime(), param.getEndTime(), param.getProjectId());
            assessFlows.forEach(flow -> {
                HashSet<Integer> uIds = new HashSet<>(JSON.parseArray(flow.getUId(), Integer.class));
                List<Integer> roleIds = JSON.parseArray(flow.getRoleId(), Integer.class);
                //通过角色id拿到用户id
                roleIds.forEach(item -> {
                    uIds.addAll(userRoleMapper.selectUserIdByRoleId(item));
                });
                uIds.forEach(item -> {
                    //判断他们是不是一个教研室
                    //获取当前用户的角色
                    List<Integer> valRoles = userRoleMapper.getRoles(item);
                    List<Integer> userRoles = userRoleMapper.getRoles(user.getUserId());
                    if (checkRoles(userRoles, valRoles)) {
                        addProjectVos(param, projectVos, flow, item);
                    }
                });
            });
        } else if (role.getWeight().compareTo(HnkjzyEncode.DIRECTOR.getCode()) == -1) {
            //普通用户老师
            List<Integer> projectIds = resultMapper.getProjectIds(user.getUserId());
            projectIds.forEach(pId -> {
                ProjectVo vo = new ProjectVo();
                Project project = projectMapper.getProjectByTime(pId, param.getStartTime(), param.getEndTime());
                if (ObjectUtil.isNotNull(project)) {
                    vo.setStartTime(project.getStartTime());
                    vo.setEndTime(project.getEndTime());
                    vo.setProjectId(pId);
                    if ((ObjectUtil.isNotNull(param.getNickName()) && user.getNickName().contains(param.getNickName())) || ObjectUtil.isNull(param.getNickName())) {
                        Integer status = resultMapper.findResultStatus(user.getUserId(), pId);
                        vo.setStatus(status != null && status == 1 ? 1 : 0);
                        if ((ObjectUtil.isNotNull(param.getStatus()) && param.getStatus().equals(vo.getStatus())) || ObjectUtil.isNull(param.getStatus())) {
                            vo.setUserId(user.getUserId());
                            vo.setProjectName(projectMapper.getProjectName(pId));
                            vo.setMajor(user.getMajor());
                            vo.setNickName(user.getNickName());
                            vo.setUserName(user.getUserName());
                            Integer score = resultMapper.findTotalScore(user.getUserId(), pId);
                            vo.setScore(score != null ? score : 0);
                            projectVos.add(vo);
                        }
                    }
                }
            });
        }
        projectVos.sort((a1, a2) -> {
            return a2.getScore().compareTo(a1.getScore());
        });
        int rank = 1;
        for (ProjectVo projectVo : projectVos) {
            projectVo.setRank(rank);  // 设置排名
            rank++;
        }
        //TODO 分页的代码
        Map<String, Object> page = PageUtils.page(projectVos, param.getPage(), param.getLimit());
        return ApiResult.ok(page);
    }

    private void addProjectVos(ProjectQueryParam param, List<ProjectVo> projectVos, FlowTask flow, Integer item) {
        ProjectVo vo = new ProjectVo();
        vo.setProjectId(flow.getProjectId());
        Project project = projectMapper.getProjectTime(flow.getProjectId());
        vo.setStartTime(project.getStartTime());
        vo.setEndTime(project.getEndTime());
        vo.setProjectName(projectMapper.getProjectName(flow.getProjectId()));
        User userInfo = userService.getById(item);
        if (ObjectUtil.isNotEmpty(userInfo)) {
            if ((ObjectUtil.isNotNull(param.getNickName()) && userInfo.getNickName().contains(param.getNickName())) || ObjectUtil.isNull(param.getNickName())) {
                Integer status = resultMapper.findResultStatus(item, flow.getProjectId());
                vo.setStatus(status != null && status == 1 ? 1 : 0);
                if ((ObjectUtil.isNotNull(param.getStatus()) && param.getStatus().equals(vo.getStatus())) || ObjectUtil.isNull(param.getStatus())) {
                    //判断当前用户是否包含某个角色
                    Integer count = 0;
                    if (ObjectUtil.isNotNull(param.getRoleId())) {
                        count = userRoleMapper.selectCount(new QueryWrapper<UserRole>().eq("user_id", userInfo.getUserId()).eq("role_id", param.getRoleId()));
                    }
                    vo.setUserId(userInfo.getUserId());
                    vo.setMajor(userInfo.getMajor());
                    vo.setNickName(userInfo.getNickName());
                    vo.setUserName(userInfo.getUserName());
                    Integer score = resultMapper.findTotalScore(item, flow.getProjectId());
                    vo.setScore(score != null ? score : 0);
                    if ((ObjectUtil.isNotNull(param.getRoleId()) && count.intValue() > 0) || ObjectUtil.isNull(param.getRoleId())) {
                        projectVos.add(vo);
                    }
                }
            }
        }
    }


    @Override
    public List<Project> getProjectByYear(String year, User user) {
        Role role = userRoleMapper.getRoleWeight(user.getUserId());
        List<Integer> pIds = new ArrayList<>();
        if (role.getWeight().compareTo(HnkjzyEncode.DIRECTOR.getCode()) == -1) {
            pIds = findPidByRoleIdFromFlow(user.getUserId());
            if (ObjectUtil.isEmpty(pIds)) {
                return new ArrayList<>();
            }
        }
        return projectMapper.getProjectByYear(year, pIds);
    }

    @Override
    public List<String> getYearByProject(User user) {
        Role role = userRoleMapper.getRoleWeight(user.getUserId());
        List<Integer> pIds = new ArrayList<>();
        if (role.getWeight().compareTo(HnkjzyEncode.DIRECTOR.getCode()) == -1) {
            pIds = findPidByRoleIdFromFlow(user.getUserId());
            if (ObjectUtil.isEmpty(pIds)) {
                return new ArrayList<>();
            }
        }
        return projectMapper.getYearByProject(pIds);
    }

    @Override
    public void addOrUpdateProjectItem(ProjectItemVo projectItemVo, User operator) {
        projectTaskImportService.saveItem(projectItemVo, operator);
    }

    @Override
    public List<Project> getProjectAndCollegeByYear(String year, String college, User user) {
        Role role = userRoleMapper.getRoleWeight(user.getUserId());
        List<Integer> pIds = new ArrayList<>();
        if (role.getWeight().compareTo(HnkjzyEncode.DIRECTOR.getCode()) == -1) {
            pIds = findPidByRoleIdFromFlow(user.getUserId());
            if (ObjectUtil.isEmpty(pIds)) {
                return new ArrayList<>();
            }
        }
        return projectMapper.getProjectAndCollegeByYear(year, pIds,college);
    }

    /**
     * 检查教研室主任看到的数据
     *
     * @param roles
     * @param roleIds
     * @return
     */
    private Boolean checkRoles(List<Integer> roles, List<Integer> roleIds) { //roles 当前登录用户角色，roleIds用户id角色
        //除了普通用户角色之外, 属于同一个教研室
        //判断接收的角色中有没有跟他一个教研室
        return roles.stream().anyMatch(role -> {
            Role one = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_id", role));
            return one.getWeight().equals(HnkjzyEncode.DEPARTMENT.getCode()) && roleIds.contains(role); //判断接收的角色中有没有跟他一个教研室
        });
    }


    /**
     * 检查二级学院下面是否包含对应的教研室   二级学院-》多个教研室
     *
     * @param roles   当前登录用户角色  软件学院院长
     * @param roleIds 所有项目的用户id角色
     * @return
     */
    private Boolean checkDeanRoles(List<Integer> roles, List<Integer> roleIds) {
        return roles.stream().anyMatch(role -> {
            Role one = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_id", role));
            return roleIds.contains(role); //判断接收的角色中有没有跟他一个教研室
        });
    }
}
