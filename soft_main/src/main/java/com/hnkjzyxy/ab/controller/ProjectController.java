package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.export.ExcelResponseExporter;
import com.hnkjzyxy.ab.export.EvidenceWordExporter;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.dto.SubTaskDto;
import com.hnkjzyxy.ab.dto.SubTaskIdDto;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.exception.AuthPermissionException;
import com.hnkjzyxy.ab.mapper.ProjectItemMapper;
import com.hnkjzyxy.ab.mapper.ProjectMapper;
import com.hnkjzyxy.ab.mapper.ResultMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.ResultExtend;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectParam;
import com.hnkjzyxy.ab.params.ProjectQueryParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.security.user.AccountUser;
import com.hnkjzyxy.ab.service.FlowService;
import com.hnkjzyxy.ab.service.FlowTaskService;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.ProjectTaskImportService;
import com.hnkjzyxy.ab.service.ResultExtendService;
import com.hnkjzyxy.ab.service.ResultItemService;
import com.hnkjzyxy.ab.service.ResultService;
import com.hnkjzyxy.ab.service.RoleService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.service.utils.ProjectTaskRules;
import com.hnkjzyxy.ab.utils.FilePathUtils;
import com.hnkjzyxy.ab.service.excel.TaskExcelPreviewService;
import com.hnkjzyxy.ab.utils.UploadUtils;
import com.hnkjzyxy.ab.vo.ProjectItemVo;
import com.hnkjzyxy.ab.params.ProjectItemSaveParam;
import com.hnkjzyxy.ab.params.ProjectItemImportParam;
import com.hnkjzyxy.ab.vo.ProjectVo;
import com.hnkjzyxy.ab.vo.ResultVo;
import com.hnkjzyxy.ab.params.ProjectResultSubmitParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * 项目管理
 * 提供项目创建、发布、考核、结果提交、子项维护与佐证材料导出等接口
 *
 * @author 16702
 */
@RestController
public class ProjectController {

    /**
     * 模板下载文件目录
     */
    @Value("${download.fileUrl}")
    private String downFile;
    /**
     * 考核项目业务服务
     */
    @Autowired
    private ProjectService projectService;
    /**
     * 旧任务 Excel 模板预览服务
     */
    @Autowired
    private TaskExcelPreviewService taskExcelPreviewService;
    /**
     * 用户业务服务
     */
    @Autowired
    private UserService userService;
    /**
     * 上传、下载及材料压缩工具
     */
    @Autowired
    private UploadUtils uploadUtils;
    /**
     * 审批流程业务服务
     */
    @Autowired
    private FlowService flowService;
    /**
     * 结果扩展项业务服务
     */
    @Autowired
    private ResultExtendService  resultExtendService;
    /**
     * 流程节点业务服务
     */
    @Resource
    private FlowTaskService flowTaskService;
    /**
     * 考核结果数据访问接口
     */
    @Resource
    private ResultMapper resultMapper;
    /**
     * 角色业务服务
     */
    @Resource
    private RoleService roleService;
    /**
     * 用户角色关联数据访问接口
     */
    @Resource
    private UserRoleMapper userRoleMapper;
    /**
     * 审批明细业务服务
     */
    @Autowired
    ResultItemService resultItemService;

    /**
     * 项目任务导入业务服务
     */
    @Resource
    private ProjectTaskImportService projectTaskImportService;

    /**
     * 从认证主体提取任务管理操作人的身份
     *
     * @param authentication 当前登录认证信息
     * @return 包含认证用户ID及用户名的操作人，有效性由Service重读数据库核验
     * @throws ProjectTaskException 未登录或认证主体不合法时抛出
     */
    private User taskOperator(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof AccountUser)) {
            throw new ProjectTaskException(401, "请先登录");
        }
        AccountUser account = (AccountUser) authentication.getPrincipal();
        User operator = new User();
        operator.setUserId(account.getUserId());
        operator.setUserName(account.getUsername());
        return operator;
    }

    /**
     * 考核项目数据访问接口
     */
    @Resource
    private ProjectMapper projectMapper;

    /**
     * 考核结果业务服务
     */
    @Resource
    private ResultService resultService;

    /**
     * 佐证材料 Word 下载组件
     */
    @Autowired
    private EvidenceWordExporter evidenceWordExporter;

    /**
     * 按查询类型将数据列表或用户列表包装为接口响应
     *
     * @param type 查询或节点类型
     * @param list 待处理数据列表
     * @param userList 用户信息列表
     * @return 查询类型对应的接口响应；类型不支持时返回错误响应
     */
    public static ApiResult getApiResult(String type, List<String> list, List<String> userList) {
        if ("0".equals(type)) {
            return ApiResult.ok("data", list);
        } else if ("1".equals(type)) {
            return ApiResult.ok("data", userList);
        } else {
            HashSet<String> set = new HashSet<>();
            if (ObjectUtil.isNotEmpty(list) && list.size() > 0) {
                set.addAll(list);
            }
            if (ObjectUtil.isNotEmpty(userList) && userList.size() > 0) {
                set.addAll(userList);
            }
            return ApiResult.ok("data", set);
        }
    }

    /**
     * 获取项目年份列表
     *
     * @param type 查询类型（0=我创建的项目年份，1=我参与的项目年份，其他=全部）
     * @param param 考核项目操作或查询参数
     * @param authentication 当前登录认证信息
     * @return 项目年份列表
     */
    @GetMapping("/project/years/{type}")
    public ApiResult getProjectYears(@PathVariable("type") String type, ProjectParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        param.setUserId(user.getUserId());
        param.setCreateName(user.getUserName());
        List<String> list = projectService.getProjectYears(param);
        List<String> userList = projectService.getUserProjectYears(param);
        return getApiResult(type, list, userList);
    }

    /**
     * 查询我创建的项目列表
     *
     * @param param 项目分页查询条件
     * @param authentication 当前登录认证信息
     * @return 项目列表及分页数据
     */
    @GetMapping("/project/list")
    public ApiResult getProjectList(ProjectParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        param.setCreateName(user.getUserName());
        param.setUserId(user.getUserId());
        Map<String, Object> map = projectService.getProjectList(param);
        return ApiResult.ok("data", map);
    }

    /**
     * 查询我参与的项目列表
     *
     * @param param 项目分页查询条件
     * @param authentication 当前登录认证信息
     * @return 项目列表及分页数据
     */
    @GetMapping("/user/project/list")
    public ApiResult getUserProjectList(ProjectParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        param.setUserId(user.getUserId());
        Map<String, Object> map = projectService.getUserProjectList(param);
        return ApiResult.ok("data", map);
    }

    /**
     * 根据项目ID查询项目详情
     *
     * @param id 项目ID
     * @param authentication 当前登录认证信息
     * @return 项目详情
     */
    @GetMapping("/project/{id}")
    public ApiResult getProjectById(@PathVariable String id, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (StringUtils.isEmpty(id)) {
            throw new RuntimeException("项目id不能为空！");
        }
        Project project = projectService.getProjectById(id, user);
        return ApiResult.ok("data", project);
    }

    /**
     * 新建或编辑项目
     *
     * @param project 项目信息（含 id 时为编辑，否则为新建）
     * @param authentication 当前登录认证信息
     * @return 操作结果
     */
    @PostMapping("/new/project")
    public ApiResult addProject(@Validated Project project, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        projectService.addProject(project, user);
        return ApiResult.ok("新建项目成功！");
    }

    /**
     * 下载项目导入模板
     *
     * @param response HTTP 响应流，直接输出 Excel 模板文件
     * @throws IOException 文件读取或输出失败时抛出
     */
    @GetMapping("/project/download/excel")
    public void download(HttpServletResponse response) throws IOException {
        uploadUtils.download(response, downFile);
    }

    /**
     * 删除项目
     * 已发布的项目不允许删除，删除后状态置为已删除
     *
     * @param id 项目ID
     * @param authentication 当前登录认证信息
     * @return 操作结果
     */
    @PostMapping("/delProject/{id}")
    //@CacheEvict(value = {HnkjxyConstants.PROJECT_BY_ID,HnkjxyConstants.FLOW_LIST, HnkjxyConstants.PROJECTS,HnkjxyConstants.PROJECT_RECYCLE_BIN},allEntries = true)
    public ApiResult delProjectById(@PathVariable String id, Authentication authentication) {
        projectTaskImportService.deleteProject(
                ProjectTaskRules.integer(id, "项目ID"), taskOperator(authentication));
        return ApiResult.ok("删除成功！");
    }

    /**
     * 解析项目任务 Excel
     *
     * @param file 项目任务 Excel 文件
     * @return 解析后的任务列表
     */
    @PostMapping("/uploadFile")
    public ApiResult uploadFile(@RequestParam("file") MultipartFile file) {
        List<Task> tasks = taskExcelPreviewService.preview(file);
        return ApiResult.ok("data", tasks);
    }

    /**
     * 发布项目
     *
     * @param id 项目ID
     * @param authentication 当前登录认证信息
     * @return 操作结果
     */
    @PostMapping("/project/publish/{id}")
    public ApiResult projectPublish(@PathVariable String id, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        if (StringUtils.isEmpty(id)) {
            throw new RuntimeException("项目id不能为空！");
        }

        Project project = projectService.getById(id);
        if (ObjectUtil.isNull(project)) {
            throw new RuntimeException("项目不存在！");
        }

        if (project.getStatus().equals(1)) {
            throw new RuntimeException("项目已发布！");
        }

        if (project.getStatus().equals(3)) {
            throw new RuntimeException("项目已删除不能发布！");
        }

        projectService.publishProject(id, user);
        return ApiResult.ok("发布成功！");
    }

    /**
     * 提交项目结果
     *
     * @param resultVo 项目结果信息
     * @param authentication 当前登录认证信息
     * @return 操作结果
     */
    @PostMapping("/project/result")
    //@RepeatSubmit
    public ApiResult projectResult(@Valid @RequestBody ProjectResultSubmitParam resultVo, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (ObjectUtil.isEmpty(resultVo.getResults())) {
            throw new RuntimeException("项目结果不能为空！");
        }
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        projectService.resultProject(resultVo, user);
        return ApiResult.ok();
    }

    /**
     * 上传项目佐证材料（支持多文件）
     *
     * @param files 材料文件数组
     * @param authentication 当前登录认证信息
     * @return 文件访问地址列表
     */
    @PostMapping("/fileUpload")
    public ApiResult fileUpload(@RequestParam("file") MultipartFile[] files, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        String dirName = user.getUserName();
        List<String> list = Arrays.stream(files)
                .map(file -> uploadUtils.uploadFile(file, dirName))
                .filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toList());
        return ApiResult.ok("data", list);
    }

    /**
     * 校验任务后保存用户项目暂存数据
     * <p>
     * 先提交任务首次暂存冻结标记，再将暂存 JSON 写入 Redis；缓存有效期为15天。
     * 缓存写入失败不会撤销冻结标记，旧格式的暂存 JSON 仍可由读取接口解析。
     * </p>
     *
     * @param result 项目结果暂存参数
     * @param authentication 当前登录认证信息
     * @return 统一接口响应
     */
    @PostMapping("/project/Staging")
    public ApiResult projectStaging(@Valid @RequestBody ProjectResultSubmitParam result, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        result.getResults().forEach(item -> item.setUId(user.getUserId()));
        projectService.projectStaging(result, user);
        return ApiResult.ok("保存成功！");
    }

    /**
     * 读取用户指定项目的暂存结果
     *
     * @param pId 考核项目ID
     * @param authentication 当前登录认证信息
     * @return 暂存结果；缓存键不存在或缓存值为空时返回 null
     */
    @GetMapping("/project/Staging/{pId}")
    public ApiResult getProjectStaging(@PathVariable String pId, Authentication authentication) {
        if (StringUtils.isEmpty(pId)) {
            throw new RuntimeException("项目id不能为空！");
        }
        User user = userService.getUserByName(authentication.getName());
        ResultVo result = projectService.getProjectStaging(user, Integer.valueOf(pId));
        return ApiResult.ok("data", result);
    }

    /**
     * 分页查询项目考核列表
     *
     * @param param 项目考核查询条件
     * @param authentication 当前登录认证信息
     * @return 项目考核列表及分页数据
     */
    @GetMapping("/project/assess/list")
    public ApiResult projectAssess(ProjectQueryParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        return projectService.getProjectAssessList(param, user);
    }

    /**
     * 获取全部已发布项目列表
     *
     * @param authentication 当前登录认证信息
     * @return 已发布项目列表
     */
    @GetMapping("/projects")
    public ApiResult getProjects(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        List<Project> list = projectService.list(new QueryWrapper<Project>().eq("status", 1));
        return ApiResult.ok("data", list);
    }

    /**
     * 获取所有部门（教研室）列表
     *
     * @param authentication 当前登录认证信息
     * @return 部门列表
     */
    @GetMapping("/department")
    public ApiResult getDepartment(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        //roleMapper.getRoleWeight()
        List<Role> list = roleService.list(new QueryWrapper<Role>().eq("weight", 2));
        return ApiResult.ok("data", list);
    }

    /**
     * 下载项目佐证材料
     * 按项目与用户汇总佐证材料并打包下载
     *
     * @param param    查询条件（项目ID、用户ID）
     * @param response HTTP 响应流，直接输出压缩包
     */
    @GetMapping("/downloadEvidence")
    public void downloadEvidence(@Validated ProjectQueryParam param, HttpServletResponse response) {

        //获得这个用户某个项目的全部的附件的证明，是一个字符串的list
        List<String> evidence = resultMapper.selectEvidenceList(param.getProjectId(), param.getUserId());
        ArrayList<String> evidences = new ArrayList<>();
        evidence.forEach(item -> {
            //获得这个文件的真实的路径地址（操作系统的区别）
            String path = FilePathUtils.getRealFilePath("/pdf/file/");
            List<String> collect = JSON.parseArray(item, String.class).stream().map(val -> {
                return val.substring(path.length());
            }).collect(Collectors.toList());
            evidences.addAll(collect);
        });
        uploadUtils.downloadEvidence(evidences, response);
    }

    /**
     * 导出项目考核结果
     *
     * @param projectVo 待导出的考核数据
     * @param response  HTTP 响应流，直接输出 Excel 文件
     */
    @PostMapping("/assess/export")
    public void exportAssess(@RequestBody ProjectVo projectVo, HttpServletResponse response) {
        if (ObjectUtil.isEmpty(projectVo.getAssessList())) {
            throw new RuntimeException("导出结果不能为空！");
        }
        ExcelResponseExporter.exportAssess(projectVo.getAssessList(), response);
    }

    /**
     * 查询用户子任务得分（已废弃）
     * <p>
     * 保留旧路径及名称筛选兼容，推荐使用 {@code /project/subTaskScores}。
     * 两条查询共用 Service 查看策略：管理员查看全部学院，院长查看当前所属学院。
     * </p>
     *
     * @param dto 子任务查询条件
     * @param authentication 当前已认证主体，用于解析操作人，不能由DTO目标用户代替
     * @return 统一成功响应，{@code data} 中包含可见成绩的 {@code total} 和 {@code list}
     * @throws AuthPermissionException 主体或当前账号无效时返回401；无查看权限或学院配置异常时返回403
     * @see #getEveryScores(SubTaskIdDto, Authentication)
     */
    @GetMapping("/project/subTaskScore")
    public ApiResult getEveryScore(SubTaskDto dto, Authentication authentication) {
        // 只从认证主体解析操作人；Service再查询当前数据库账号、角色及学院。
        User user = resultOperator(authentication);

        Map<String, Object> map = resultService.getList(dto, user);


        return ApiResult.ok("data", map);
    }

    /**
     * 根据分类名称查询项目下的子任务名称
     *
     * @param param 分类查询条件
     * @return 子任务名称数据
     */
    @GetMapping("/project/subTaskName")
    public ApiResult getEveryScore(Task param) {

        Map<String, Object> map = resultService.getListByCategoryName(param);

        return ApiResult.ok(map);
    }


    /**
     * 根据ID查询用户子任务得分
     * <p>
     * DTO 中的用户ID仅作为目标筛选条件；实际查看范围由 Service 的当前账号及角色策略生成。
     * 院长查询其他学院目标时返回正常空列表，成功响应结构与旧查询一致。
     * </p>
     *
     * @param dto 子任务查询条件
     * @param authentication 当前已认证主体，用于解析实际操作人
     * @return 统一成功响应，{@code data} 中包含可见成绩的 {@code total} 和 {@code list}
     * @throws AuthPermissionException 主体或当前账号无效时返回401；无查看权限或学院配置异常时返回403
     */
    @GetMapping("/project/subTaskScores")
    public ApiResult getEveryScores(SubTaskIdDto dto, Authentication authentication) {
        // 请求只提供筛选条件，不接受由客户端决定的角色或学院授权范围。
        User user = resultOperator(authentication);

        Map<String, Object> map = resultService.getLists(dto, user);

        return ApiResult.ok("data", map);
    }

    /**
     * 查询项目下的全部分类
     *
     * @param projectId 项目ID
     * @param authentication 当前登录认证信息
     * @return 该项目下的分类名称列表
     */
    @GetMapping("/project/projectCategory/{projectId}")
    public ApiResult getEveryScoreByTitle(@PathVariable("projectId") Integer projectId, Authentication authentication) {
        //需要的内容 前端的子任务的名称和项目的名称
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        //获得该项目的分类名称的列表
        List<String> list = projectMapper.getCategoryByProjectName(projectId);
        Map<String, Object> map = new HashMap<>();
        map.put("data", list);
        map.put("total", list.size());

        //返回项目的名称列表
        return ApiResult.ok("data", map);

    }


    /**
     * 批量修改用户子任务得分
     * <p>
     * 仅允许具有有效院长角色的操作人修改当前学院成绩，管理员角色本身不授予评分权限。
     * Service 在同一事务内核验整批目标并更新，任何越界目标均拒绝整批评分。
     * </p>
     *
     * @param dto 待更新的子任务得分列表
     * @param authentication 当前已认证主体，用于解析实际评分操作人
     * @return 保留现有“修改成功”提示的统一响应
     * @throws AuthPermissionException 主体或当前账号无效时返回401；无评分权限或越界时返回403
     * @throws ProjectTaskException 参数、任务归属、结果有效性或项目状态不符合评分要求时抛出
     */
    @PutMapping("/project/subTaskScore")
    public ApiResult updateEveryScore(@Valid @RequestBody List<SubTaskIdDto> dto, Authentication authentication) {
        // DTO中的用户ID是评分目标；先解析认证操作人，再交由Service校验权限和事务范围。
        User user = resultOperator(authentication);

        resultService.updateSubTaskScore(dto, user);
        return ApiResult.ok("修改成功");
    }

    /**
     * 从认证主体解析三个成绩入口的操作人
     * <p>
     * 拒绝缺失、未认证或匿名主体，再按主体工号取得用户资料。
     * 此处取得的用户可能来自缓存，其状态、角色及学院必须由 Service 重新查库核验；
     * 本方法不代替实际授权，也不改变其他接口或认证过滤器的响应契约。
     * </p>
     *
     * @param authentication 当前请求的认证信息
     * @return 与认证主体用户名对应的用户资料，供 Service 进一步核验
     * @throws AuthPermissionException 主体无效或无法取得对应用户时返回401
     */
    private User resultOperator(Authentication authentication) {
        // 匿名认证令牌也可能标记为已认证，需要单独拒绝，避免将匿名主体当作业务账号。
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null || authentication.getName().trim().isEmpty()) {
            throw new AuthPermissionException(401, "当前登录身份无效，请重新登录");
        }
        // 认证用户名只用于取得身份线索，缓存资料不直接决定本次学院或角色权限。
        User user = userService.getUserByName(authentication.getName());
        if (user == null) throw new AuthPermissionException(401, "认证用户无效或已禁用，请重新登录");
        return user;
    }

    /**
     * 导出子任务佐证材料为 Word
     *
     * @param dto      子任务信息（含佐证材料地址）
     * @param response HTTP 响应流，直接输出 Word 文件
     */
    @PostMapping("/evidence/exportToWord")
    public void exportEvidenceToWord(@RequestBody SubTaskIdDto dto, HttpServletResponse response) {
        //需要的参数是什么？ 用户的id，taskId , projectId, 文件的路径地址（一个String类型的字符串，里面是由很多的数组

        String evidences = dto.getEvidence();//这个字符串其实就是所有的evidence的路径的字符串，用逗号分割的
        evidenceWordExporter.export(evidences, response);

    }


    /**
     * 导出子任务佐证材料为 ZIP 压缩包
     *
     * @param dto      子任务信息（含佐证材料地址）
     * @param response HTTP 响应流，直接输出 ZIP 文件
     */
    @PostMapping("/evidence/exportToZip")
    public void exportEvidenceToZip(@RequestBody SubTaskIdDto dto, HttpServletResponse response) {
        //但是其实这个接口只需要获得文件所在的路径就好，不需要获得是谁的文件，因为文件的名称在服务器中的名字是唯一的
        //业务：获得服务器上存放的文件的地址
//        if(dto.getEvidence().isEmpty()){return;}
        String evidence = dto.getEvidence();

        List<String> array = JSON.parseArray(evidence, String.class);//变成字符串类型的数组

        String path = FilePathUtils.getRealFilePath("/pdf/file/");//目的就是去除字符串前面的/pdf/file的内容
        List<String> collect = array.stream().map(s -> {
            return s.substring(path.length());
        }).collect(Collectors.toList());

        uploadUtils.downloadEvidence(collect, response);

    }


    /**
     * 根据项目ID查询项目子项
     *
     * @param projectId 项目ID
     * @param authentication 当前登录认证信息
     * @return 项目子项列表
     */
    @GetMapping("/project/item/{projectId}")
    public ApiResult getProjectItemById(@PathVariable("projectId") Integer projectId, Authentication authentication) {
        List<ProjectItemVo> list = projectMapper.getProjectItemById(projectId);
        return ApiResult.ok("data", list);
    }

    /**
     * 新增或修改项目子项
     * 既可用于新增分类，也可用于新增分类下的子项
     *
     * @param projectItemVo 项目子项信息
     * @param authentication 当前登录认证信息
     */
    @PostMapping("/project/item")
    public void addProjectItem(@RequestBody ProjectItemSaveParam projectItemVo, Authentication authentication) {
        projectService.addOrUpdateProjectItem(projectItemVo, taskOperator(authentication));
    }

    /**
     * 删除项目子项
     *
     * @param id 项目子项ID
     * @param authentication 当前登录认证信息
     */
    @DeleteMapping("/project/item/{id}")
    public void deleteProjectItem(@PathVariable("id") Integer id, Authentication authentication) {
        projectTaskImportService.deleteItem(id, taskOperator(authentication));
    }

    /**
     * 将项目子项导入为任务
     *
     * @param projectItems 项目子项列表
     * @param authentication 当前登录认证信息
     * @return 统一接口响应
     */
    @PostMapping("/project/item/insertIntoTask")
    public ApiResult insertIntoTask(@RequestBody(required = false) List<ProjectItemImportParam> projectItems, Authentication authentication) {
        return ApiResult.ok("data", projectTaskImportService.merge(projectItems, taskOperator(authentication)));
    }


    /**
     * 查询指定用户在某任务下的全部佐证材料
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 佐证材料文件路径列表
     */
    @GetMapping("/project/item/selectIntoTaskFile")
    public ApiResult selectIntoTaskFile(@RequestParam("taskId") String taskId,@RequestParam("userId") String userId) {
        ObjectMapper objectMapper = new ObjectMapper();

        // 获取该任务的所有佐证材料记录
        List<ResultExtend> taskEvidences = resultExtendService.list(
                new QueryWrapper<ResultExtend>().eq("task_id", taskId).eq("u_id",userId)
        );

        // 处理证据数据，解析JSON字符串
        List<String> filepath = new ArrayList<>();
        for (ResultExtend evidence : taskEvidences) {
            String evidenceStr = evidence.getEvidence();
            if (evidenceStr != null && !evidenceStr.trim().isEmpty() && !evidenceStr.equals("[]")) {
                try {
                    // 解析JSON字符串数组
                    List<String> paths = objectMapper.readValue(evidenceStr, new TypeReference<List<String>>() {});
                    filepath.addAll(paths);
                } catch (Exception e) {
                    // 如果解析失败，记录日志或处理异常
                    e.printStackTrace();
                }
            }
        }

        // 返回所有佐证材料的文件路径列表
        return ApiResult.ok("data", filepath);
    }

    /**
     * 查询项目下的全部参与教师
     *
     * @param projectId 项目ID
     * @return 参与教师信息（key 为工号）
     */
    @GetMapping("/project/selectTeachAllByProjectId")
    public ApiResult selectTeachAllByProjectId(@RequestParam("projectId") Integer projectId) {
        List<Result> projectId1 = resultService.list(new QueryWrapper<Result>().eq("p_id", projectId));
        Map<String,User> users = new HashMap<>();
        projectId1.forEach(item -> {
            User byId = userService.getById(item.getUId());
            users.put(byId.getUserName(),byId);
        });
        return ApiResult.ok("data", users);
    }

}
