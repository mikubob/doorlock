package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.mapper.FlowMapper;
import com.hnkjzyxy.ab.mapper.FlowTaskMapper;
import com.hnkjzyxy.ab.mapper.ProjectItemMapper;
import com.hnkjzyxy.ab.mapper.ProjectMapper;
import com.hnkjzyxy.ab.mapper.ProjectTaskImportMapper;
import com.hnkjzyxy.ab.mapper.TaskMapper;
import com.hnkjzyxy.ab.model.Flow;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.ProjectItem;
import com.hnkjzyxy.ab.model.ProjectItemSource;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.ProjectTaskImportService;
import com.hnkjzyxy.ab.service.utils.ProjectTaskRules;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;
import com.hnkjzyxy.ab.vo.ProjectItemVo;
import com.hnkjzyxy.ab.vo.ProjectTaskImportResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Resource;

/**
 * 项目子项任务导入与来源维护Service实现
 * <p>
 * 在完整事务内校验身份、原始来源和项目冻结状态，按来源合并任务并保留既有任务主键。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
public class ProjectTaskImportServiceImpl implements ProjectTaskImportService {
    /**
     * 项目锁、权限与生命周期保护服务
     */
    @Resource
    private ProjectTaskGuard guard;

    /**
     * 导入来源及审核状态数据访问接口
     */
    @Resource
    private ProjectTaskImportMapper mapper;

    /**
     * 任务持久化数据访问接口
     */
    @Resource
    private TaskMapper taskMapper;

    /**
     * 项目分类与子项数据访问接口
     */
    @Resource
    private ProjectItemMapper itemMapper;

    /**
     * 项目数据访问接口
     */
    @Resource
    private ProjectMapper projectMapper;

    /**
     * 项目流程数据访问接口
     */
    @Resource
    private FlowMapper flowMapper;

    /**
     * 流程步骤数据访问接口
     */
    @Resource
    private FlowTaskMapper flowTaskMapper;

    /**
     * 新增任务字符串主键生成器
     */
    @Resource
    private SnowFlowUtils snowFlowUtils;

    /**
     * 将选中的项目子项按来源差量合并为任务
     * <p>
     * 新增缺失任务、原位更新允许变更的任务并跳过无变化项，不删除来源或未选任务；失败时整批回滚。
     * </p>
     *
     * @param request 项目及来源子项ID列表，评分字段从数据库重读
     * @param operator 当前认证操作人
     * @return 本次合并的执行计数
     * @throws ProjectTaskException 输入、权限、审核或生命周期校验失败时抛出
     */
    @Override
    public ProjectTaskImportResult merge(List<ProjectItemVo> request, User operator) {
        long started = System.currentTimeMillis();
        if (request == null || request.isEmpty() || request.size() > 1000) {
            throw new ProjectTaskException(400, "请选择1至1000个不同的项目子项");
        }
        Integer projectId = request.get(0) == null ? null : request.get(0).getProjectId();
        Set<Integer> ids = new LinkedHashSet<>();
        for (ProjectItemVo row : request) {
            if (row == null || projectId == null || !projectId.equals(row.getProjectId()) ||
                    row.getId() == null || row.getId() <= 0 || !ids.add(row.getId())) {
                throw new ProjectTaskException(400, "子项ID不能为空或重复，所有子项必须属于同一项目");
            }
        }
        Project project = guard.lock(projectId);
        guard.manage(project, operator);
        Map<Integer, Task> existing = new HashMap<>();
        ProjectTaskImportResult result = new ProjectTaskImportResult();
        result.setProjectId(projectId);
        result.setRequested(ids.size());
        for (Task task : mapper.tasks(projectId)) {
            if (task.getSourceProjectItemId() == null) {
                result.setRetainedOtherTasks(result.getRetainedOtherTasks() + 1);
            } else if (existing.put(task.getSourceProjectItemId(), task) != null) {
                throw new ProjectTaskException(409, "存在重复来源任务，请先审核历史映射");
            }
        }
        List<Task> created = new ArrayList<>(), updated = new ArrayList<>();
        for (Integer id : ids) {
            ProjectItemSource item = mapper.source(id);
            if (item == null) throw new ProjectTaskException(400, "子项" + id + "不存在");
            Task desired = ProjectTaskRules.map(item, mapper.source(item.getParentId()), projectId);
            Task current = existing.get(id);
            if (current == null) {
                created.add(desired);
            } else if (ProjectTaskRules.same(current, desired)) {
                result.setSkipped(result.getSkipped() + 1);
            } else {
                desired.setId(current.getId());
                updated.add(desired);
            }
        }
        // 先完成整批来源校验、差量和冻结检查，再进行任何写入。
        if (!created.isEmpty() || !updated.isEmpty()) guard.mutable(project);
        List<String> changedIds = new ArrayList<>();
        for (Task task : created) {
            task.setId(String.valueOf(snowFlowUtils.nextId()));
            if (taskMapper.insert(task) != 1) throw new IllegalStateException("任务新增失败，导入已回滚");
            changedIds.add(task.getId());
        }
        for (Task task : updated) {
            // 专用 SQL 显式写 NULL，允许备注由有值改为空；保留任务主键。
            if (mapper.updateTask(task) != 1) throw new IllegalStateException("任务更新失败，导入已回滚");
            changedIds.add(task.getId());
        }
        result.setCreated(created.size());
        result.setUpdated(updated.size());
        log.info("T04 MERGE operator={} project={} requested={} created={} updated={} skipped={} tasks={} elapsedMs={}",
                operator.getUserId(), projectId, result.getRequested(), result.getCreated(), result.getUpdated(),
                result.getSkipped(), changedIds, System.currentTimeMillis() - started);
        return result;
    }

    /**
     * 新增或修改项目分类、子项
     *
     * @param request 分类或子项维护信息
     * @param operator 当前认证操作人
     * @throws ProjectTaskException 无管理权限、项目已冻结或来源字段不合法时抛出
     */
    @Override
    public void saveItem(ProjectItemVo request, User operator) {
        if (request == null || request.getProjectId() == null) throw new ProjectTaskException(400, "项目ID不能为空");
        ProjectItemSource old = request.getId() == null ? null : mapper.source(request.getId());
        if (request.getId() != null && old == null) throw new ProjectTaskException(404, "子项不存在");
        Integer projectId = old == null ? request.getProjectId() : ProjectTaskRules.integer(old.getProjectId(), "子项project_id");
        Project project = guard.lock(projectId);
        guard.manage(project, operator);
        guard.mutable(project);
        // 锁后重新读取，拒绝通过请求换项目或改变已有分类/子项层级。
        if (old != null) {
            old = mapper.source(request.getId());
            if (old == null || !projectId.equals(request.getProjectId()) ||
                    ProjectTaskRules.integer(old.getProjectId(), "子项project_id") != projectId ||
                    !Objects.equals(old.getGrade(), request.getGrade())) {
                throw new ProjectTaskException(409, "不允许改变已有子项的项目归属或层级");
            }
        }
        ProjectItemSource source = new ProjectItemSource();
        source.setId(request.getId());
        source.setProjectId(projectId.toString());
        source.setPname(ProjectTaskRules.text(request.getPname(), "名称", true));
        source.setGrade(request.getGrade());
        source.setParentId(request.getParentId());
        source.setStandard(request.getStandard());
        source.setScore(request.getScore() == null ? null : request.getScore().toString());
        source.setIsFile(request.getIsFile());
        source.setIsExtend(request.getIsExtend());
        source.setRemark(request.getRemark());
        Task mapped = null;
        if (Objects.equals(source.getGrade(), 2)) {
            mapped = ProjectTaskRules.map(source, mapper.source(source.getParentId()), projectId);
        } else if (!Objects.equals(source.getGrade(), 1) || !Objects.equals(source.getParentId(), 0)) {
            throw new ProjectTaskException(400, "仅允许顶层分类或其下的二级子项");
        }
        ProjectItem item = new ProjectItem();
        item.setId(source.getId());
        item.setProjectId(projectId);
        item.setPname(source.getPname());
        item.setGrade(source.getGrade());
        item.setParentId(source.getParentId());
        item.setStandard(mapped == null ? null : mapped.getStandard());
        item.setScore(mapped == null ? null : mapped.getScore());
        item.setIsFile(mapped == null ? 0 : mapped.getIsFile());
        item.setIsExtend(mapped == null ? 0 : mapped.getIsExtend());
        item.setRemark(ProjectTaskRules.text(source.getRemark(), "备注", false));
        if (item.getId() == null) {
            item.setOrderBy(itemMapper.selectCount(new QueryWrapper<ProjectItem>().eq("project_id", projectId)
                    .eq("grade", item.getGrade()).eq("parent_id", item.getParentId())) + 1);
            if (itemMapper.insert(item) != 1) throw new IllegalStateException("新增子项失败");
        } else {
            UpdateWrapper<ProjectItem> update = new UpdateWrapper<ProjectItem>().eq("id", item.getId())
                    .set("pname", item.getPname()).set("parent_id", item.getParentId())
                    .set("standard", item.getStandard()).set("score", item.getScore())
                    .set("is_file", item.getIsFile()).set("is_extend", item.getIsExtend()).set("remark", item.getRemark());
            if (itemMapper.update(null, update) != 1) throw new IllegalStateException("更新子项失败");
        }
    }

    /**
     * 删除允许维护的来源分类或子项
     * <p>
     * 有子项的分类不能直接删除，来源删除不级联删除任务、结果或材料。
     * </p>
     *
     * @param id 来源分类或子项ID
     * @param operator 当前认证操作人
     * @throws ProjectTaskException 来源不存在、项目已冻结或无管理权限时抛出
     */
    @Override
    public void deleteItem(Integer id, User operator) {
        ProjectItemSource item = mapper.source(id);
        if (item == null) throw new ProjectTaskException(404, "子项不存在");
        Integer projectId = ProjectTaskRules.integer(item.getProjectId(), "子项project_id");
        Project project = guard.lock(projectId);
        guard.manage(project, operator);
        guard.mutable(project);
        item = mapper.source(id);
        if (item == null || ProjectTaskRules.integer(item.getProjectId(), "子项project_id") != projectId) {
            throw new ProjectTaskException(409, "子项归属已变化");
        }
        if (itemMapper.selectCount(new QueryWrapper<ProjectItem>().eq("parent_id", id)) > 0) {
            throw new ProjectTaskException(409, "分类仍有子项，请先处理子项");
        }
        // 不级联删除任何任务、结果或材料。
        if (itemMapper.deleteById(id) != 1) throw new IllegalStateException("删除子项失败");
    }

    /**
     * 在项目锁内删除未发布且未使用的项目
     * <p>
     * 保留原有项目软删除语义，相关流程维护与项目状态变更在同一事务中完成。
     * </p>
     *
     * @param id 项目ID
     * @param operator 当前认证操作人
     * @throws ProjectTaskException 项目生命周期或管理权限不允许删除时抛出
     */
    @Override
    public void deleteProject(Integer id, User operator) {
        Project project = guard.lock(id);
        guard.manage(project, operator);
        guard.mutable(project);
        for (Flow flow : flowMapper.selectList(new QueryWrapper<Flow>().eq("p_id", id))) {
            flowTaskMapper.delete(new QueryWrapper<FlowTask>().eq("parent_id", flow.getId()));
        }
        flowMapper.delete(new QueryWrapper<Flow>().eq("p_id", id));
        if (projectMapper.update(null, new UpdateWrapper<Project>().eq("id", id).set("status", 3)) != 1) {
            throw new IllegalStateException("删除项目失败");
        }
    }
}
