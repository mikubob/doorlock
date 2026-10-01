package com.hnkjzyxy.ab.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.dto.SubTaskDto;
import com.hnkjzyxy.ab.dto.SubTaskIdDto;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
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
import com.hnkjzyxy.ab.service.utils.ExcelUtils;
import com.hnkjzyxy.ab.service.utils.ProjectTaskRules;
import com.hnkjzyxy.ab.utils.FilePathUtils;
import com.hnkjzyxy.ab.utils.ReadExcelUtils;
import com.hnkjzyxy.ab.utils.UploadUtils;
import com.hnkjzyxy.ab.vo.ProjectItemVo;
import com.hnkjzyxy.ab.vo.ProjectVo;
import com.hnkjzyxy.ab.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
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

    @Value("${download.fileUrl}")
    private String downFile;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ReadExcelUtils readExcelUtils;
    @Autowired
    private UserService userService;
    @Autowired
    private UploadUtils uploadUtils;
    @Autowired
    private FlowService flowService;
    @Autowired
    private ResultExtendService  resultExtendService;
    @Resource
    private FlowTaskService flowTaskService;
    @Resource
    private ResultMapper resultMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Autowired
    ResultItemService resultItemService;

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

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ResultService resultService;

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
        List<Task> tasks = readExcelUtils.readExcel(file);
        return ApiResult.ok("data", tasks);
    }

    /**
     * 发布项目
     *
     * @param id 项目ID
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
     * @return 操作结果
     */
    @PostMapping("/project/result")
    //@RepeatSubmit
    public ApiResult projectResult(@Valid @RequestBody ResultVo resultVo, Authentication authentication) {
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
     * 暂存项目结果
     *
     * @param result 项目结果信息
     * @return 操作结果
     */
    @PostMapping("/project/Staging")
    public ApiResult projectStaging(@Valid @RequestBody ResultVo result, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        result.getResults().forEach(item -> item.setUId(user.getUserId()));
        projectService.projectStaging(result, user);
        return ApiResult.ok("保存成功！");
    }

    /**
     * 获取项目考核暂存数据（开始考核）
     *
     * @param pId 项目ID
     * @return 该项目的暂存结果
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
        ExcelUtils.exportAssess(projectVo.getAssessList(), response);
    }

    /**
     * 查询用户子任务得分（已废弃）
     * 推荐使用 /project/subTaskScores
     *
     * @param dto 子任务查询条件
     * @return 子任务得分数据
     */
    @GetMapping("/project/subTaskScore")
    public ApiResult getEveryScore(SubTaskDto dto, Authentication authentication) {
        //需要的内容 前端的子任务的名称和项目的名称
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

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
     *
     * @param dto 子任务查询条件
     * @return 子任务得分数据
     */
    @GetMapping("/project/subTaskScores")
    public ApiResult getEveryScores(SubTaskIdDto dto, Authentication authentication) {
        //每个下拉框都是独立写一个接口，然后通过大的接口写入
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        Map<String, Object> map = resultService.getLists(dto, user);

        return ApiResult.ok("data", map);
    }

    /**
     * 查询项目下的全部分类
     *
     * @param projectId 项目ID
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
     *
     * @param dto 待更新的子任务得分列表
     * @return 操作结果
     */
    @PutMapping("/project/subTaskScore")
    public ApiResult updateEveryScore(@Valid @RequestBody List<SubTaskIdDto> dto, Authentication authentication) {
        //获得项目的名称和项目的分类还有子任务的名称和分数

        //需要的内容 还是用查询的返回结果来接收，是一个list
        //只能做修改
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        resultService.updateSubTaskScore(dto, user);
        return ApiResult.ok("修改成功");
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
        resultService.downloadEvidenceToWord(evidences, response);

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
     */
    @PostMapping("/project/item")
    public void addProjectItem(@RequestBody ProjectItemVo projectItemVo, Authentication authentication) {
        projectService.addOrUpdateProjectItem(projectItemVo, taskOperator(authentication));
    }

    /**
     * 删除项目子项
     *
     * @param id 项目子项ID
     */
    @DeleteMapping("/project/item/{id}")
    public void deleteProjectItem(@PathVariable("id") Integer id, Authentication authentication) {
        projectTaskImportService.deleteItem(id, taskOperator(authentication));
    }

    /**
     * 将项目子项导入为任务
     *
     * @param projectItems 项目子项列表
     */
    @PostMapping("/project/item/insertIntoTask")
    public ApiResult insertIntoTask(@RequestBody(required = false) List<ProjectItemVo> projectItems, Authentication authentication) {
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
