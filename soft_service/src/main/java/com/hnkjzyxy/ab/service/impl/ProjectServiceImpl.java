package com.hnkjzyxy.ab.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.dto.ProjectAssessRow;
import com.hnkjzyxy.ab.dto.ProjectAssessScope;
import com.hnkjzyxy.ab.enums.HnkjzyEncode;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.mapper.FlowMapper;
import com.hnkjzyxy.ab.mapper.FlowTaskMapper;
import com.hnkjzyxy.ab.mapper.NoticeMapper;
import com.hnkjzyxy.ab.mapper.ProjectItemMapper;
import com.hnkjzyxy.ab.mapper.ProjectMapper;
import com.hnkjzyxy.ab.mapper.ResultMapper;
import com.hnkjzyxy.ab.mapper.TaskMapper;
import com.hnkjzyxy.ab.mapper.UserMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.Flow;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.ResultExtend;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectItemSaveParam;
import com.hnkjzyxy.ab.params.ProjectParam;
import com.hnkjzyxy.ab.params.ProjectQueryParam;
import com.hnkjzyxy.ab.params.ProjectResultSubmitParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.ProjectTaskImportService;
import com.hnkjzyxy.ab.service.ResultExtendService;
import com.hnkjzyxy.ab.service.ResultService;
import com.hnkjzyxy.ab.service.TaskService;
import com.hnkjzyxy.ab.service.excel.TaskExcelImportService;
import com.hnkjzyxy.ab.service.support.ProjectTaskTreeSupport;
import com.hnkjzyxy.ab.service.utils.ProjectAssessScopeResolver;
import com.hnkjzyxy.ab.service.utils.ProjectTaskRules;
import com.hnkjzyxy.ab.vo.FlowTaskVo;
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
import java.util.Collections;
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
     * 项目考核列表的认证用户范围解析器
     */
    @Resource
    private ProjectAssessScopeResolver projectAssessScopeResolver;
    /**
     * 审批流程数据访问接口
     */
    @Resource
    private FlowMapper flowMapper;
    /**
     * 用户数据访问接口
     */
    @Resource
    private UserMapper userMapper;
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
     * 考核结果业务服务
     */
    @Autowired
    private ResultService resultService;
    /**
     * 通知公告数据访问接口
     */
    @Resource
    private NoticeMapper noticeMapper;
    /**
     * 流程节点数据访问接口
     */
    @Resource
    private FlowTaskMapper flowTaskMapper;
    /**
     * 项目流程及任务统计信息补充组件
     */
    @Resource
    private ProjectTaskTreeSupport projectTaskTreeSupport;
    /**
     * Redis 数据操作模板
     */
    @Resource
    private RedisTemplate redisTemplate;
    /**
     * 结果扩展项业务服务
     */
    @Resource
    private ResultExtendService resultExtendService;
    /**
     * TaskService业务服务
     */
    @Resource
    private TaskService taskService;
    /**
     * 任务 Excel 导入服务
     */
    @Autowired
    private TaskExcelImportService taskExcelImportService;
    /**
     * 编程式事务模板
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 项目子项数据访问接口
     */
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
    /**
     * {@inheritDoc}
     */
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
        projectTaskTreeSupport.buildProject(projectList);//这个没有起到作用
        HashMap<String, Object> map = new HashMap<>();
        map.put("total", page.getTotal());
        map.put("list", projectList);
        return map;
    }

    //获取用户项目列表
    /**
     * {@inheritDoc}
     */
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
     *
     * @param userId 用户ID
     * @return 查询结果列表
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
     *
     * @param roles 当前用户角色ID集合
     * @param roleIds 目标角色ID集合
     * @return 操作或条件校验结果
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
     * @return 包含 total 总条数及 list 当前页项目的映射
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
        projectTaskTreeSupport.buildProject(projectList);*/
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
     * @return 查询得到的数值
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
    /**
     * {@inheritDoc}
     */
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
                        taskExcelImportService.readTaskExcel(project.getFile(), project.getId());
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
    /**
     * {@inheritDoc}
     */
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
    /**
     * {@inheritDoc}
     */
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
     *
     * @param item 考核项目信息
     * @return 操作或条件校验结果
     */
    public Boolean checkProjectOverDue(Project item) {
        //比较：前者大于后者
        int i = item.getEndTime().compareTo(new Date());
        int j = item.getEndTime().compareTo(item.getStartTime());
        //大于零未过期，小于等于零已过期
        return i <= 0 || j <= 0;
    }

    //提交项目结果
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = {HnkjxyConstants.APPROVE_LIST, HnkjxyConstants.RADAR_CHART, HnkjxyConstants.ASSESS_LIST,
            HnkjxyConstants.COLUMNAR_CHART, HnkjxyConstants.NOT_SUB_LIST, HnkjxyConstants.RESULT_DETAIL, HnkjxyConstants.RESULT_LIST,
            HnkjxyConstants.END_PROJECT, HnkjxyConstants.USER_PROJECTS, HnkjxyConstants.NOTICE_LIST}, allEntries = true)
    public void resultProject(ProjectResultSubmitParam result, User user) {
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
     * 校验任务后保存用户项目暂存数据
     * <p>
     * 先提交任务首次暂存冻结标记，再将暂存 JSON 写入 Redis；缓存有效期为15天。
     * 缓存写入失败不会撤销冻结标记，旧格式的暂存 JSON 仍可由读取接口解析。
     * </p>
     *
     * @param result 项目结果暂存参数
     * @param user 当前认证用户
     */
    @Override
    public void projectStaging(ProjectResultSubmitParam result, User user) {
        projectTaskGuard.recordStaging(result.getProjectId(), user, result.getResults());
        String key = user.getUserName().concat("-" + result.getProjectId());
        try {
            redisTemplate.opsForValue().set(key, JSONObject.toJSONString(result), 15, TimeUnit.DAYS);
        } catch (RuntimeException e) {
            throw new ProjectTaskException(503, "暂存缓存写入失败；任务冻结标记已保留，请重试保存");
        }
    }

    /**
     * 读取用户指定项目的暂存结果
     *
     * @param user 当前认证用户
     * @param pId 考核项目ID
     * @return 暂存结果；缓存键不存在或缓存值为空时返回 null
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


    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    //@RedisCache(key = HnkjxyConstants.ASSESS_LIST)
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ApiResult getProjectAssessList(ProjectQueryParam param, User user) {
        validateAssessQuery(param);
        ProjectAssessScope scope = projectAssessScopeResolver.resolve(user);
        if (scope.getType() != ProjectAssessScope.Type.NONE
                && scope.getType() != ProjectAssessScope.Type.SELF_RESULTS) {
            Integer invalidNode = projectMapper.findInvalidAssessRecipient(param);
            if (invalidNode != null) {
                throw new ProjectTaskException(500, "项目考核接收名单格式异常，流程节点ID：" + invalidNode);
            }
        }
        long offset = ((long) param.getPage() - 1L) * param.getLimit();
        long total = scope.getType() == ProjectAssessScope.Type.NONE
                ? 0L : projectMapper.countAssessList(param, scope);
        List<ProjectVo> list = new ArrayList<>();
        if (offset < total) {
            List<ProjectAssessRow> rows = projectMapper.selectAssessListPage(param, scope, offset, param.getLimit());
            for (int i = 0; i < rows.size(); i++) {
                ProjectAssessRow row = rows.get(i);
                long score = row.getTotalScore() == null ? 0L : row.getTotalScore();
                long rank = offset + i + 1L;
                if (score < Integer.MIN_VALUE || score > Integer.MAX_VALUE || rank > Integer.MAX_VALUE) {
                    throw new ProjectTaskException(500, "项目考核总分或排名超出接口数值范围");
                }
                ProjectVo vo = new ProjectVo();
                BeanUtils.copyProperties(row, vo);
                vo.setScore((int) score);
                vo.setRank((int) rank);
                list.add(vo);
            }
        }
        Map<String, Object> page = new HashMap<>();
        page.put("total", total);
        page.put("totalPage", total / param.getLimit() + (total % param.getLimit() == 0 ? 0 : 1));
        page.put("page", param.getPage());
        page.put("limit", param.getLimit());
        page.put("list", list.isEmpty() ? Collections.emptyList() : list);
        return ApiResult.ok(page);
    }

    /**
     * 校验本接口可选筛选及分页边界，不改变共享参数类的下载材料校验
     *
     * @param param 项目考核查询条件
     * @throws ProjectTaskException 分页、ID、状态或时间区间不合法时返回400
     */
    private void validateAssessQuery(ProjectQueryParam param) {
        if (param == null || param.getPage() < 1 || param.getLimit() < 1 || param.getLimit() > 100) {
            throw new ProjectTaskException(400, "页码必须大于0，每页条数必须在1至100之间");
        }
        if ((param.getProjectId() != null && param.getProjectId() <= 0)
                || (param.getRoleId() != null && param.getRoleId() <= 0)) {
            throw new ProjectTaskException(400, "项目ID和角色ID必须大于0");
        }
        if (param.getStatus() != null && param.getStatus() != 0 && param.getStatus() != 1) {
            throw new ProjectTaskException(400, "考核状态仅允许0或1");
        }
        if (param.getStartTime() != null && param.getEndTime() != null
                && param.getStartTime().after(param.getEndTime())) {
            throw new ProjectTaskException(400, "查询开始时间不能晚于结束时间");
        }
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void addOrUpdateProjectItem(ProjectItemSaveParam projectItemVo, User operator) {
        projectTaskImportService.saveItem(projectItemVo, operator);
    }

    /**
     * {@inheritDoc}
     */
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

}
