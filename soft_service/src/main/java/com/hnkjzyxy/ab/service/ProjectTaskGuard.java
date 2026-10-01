package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.mapper.ProjectTaskImportMapper;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.ProjectImportState;
import com.hnkjzyxy.ab.model.ProjectItemSource;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.ResultExtend;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.utils.ProjectTaskRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Resource;

/**
 * 项目任务生命周期与并发保护服务
 * <p>
 * 业务事务使用 READ_COMMITTED 并持有项目行锁，统一校验管理范围、历史审核、冻结及任务归属。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
@Service
public class ProjectTaskGuard {
    /**
     * 项目任务导入专用数据访问接口
     */
    @Resource
    private ProjectTaskImportMapper mapper;

    /**
     * 取得项目行锁并校验项目状态
     *
     * @param id 项目ID
     * @return 锁定后的项目信息
     * @throws ProjectTaskException 项目不存在或状态不允许操作时抛出
     * @throws IllegalStateException 未处于业务事务中时抛出
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Project lock(Integer id) {
        if (id == null || id <= 0) throw new ProjectTaskException(400, "项目ID不合法");
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("项目锁必须在业务事务中取得");
        }
        Project project = mapper.lockProject(id);
        if (project == null) throw new ProjectTaskException(404, "项目不存在");
        if (!Objects.equals(project.getStatus(), 0) && !Objects.equals(project.getStatus(), 1)) {
            throw new ProjectTaskException(409, "项目已删除或状态不合法");
        }
        return project;
    }

    /**
     * 校验操作人的有效身份和项目管理权限
     *
     * @param project 已锁定的项目
     * @param operator 从认证主体取得的操作人
     * @throws ProjectTaskException 用户无效或无管理权限时抛出
     */
    public void manage(Project project, User operator) {
        if (operator == null || operator.getUserId() == null || operator.getUserName() == null ||
                mapper.activeUser(operator.getUserId(), operator.getUserName()) != 1) {
            throw new ProjectTaskException(401, "认证用户无效或已禁用");
        }
        if (mapper.activeAdmin(operator.getUserId()) == 0 && !operator.getUserName().equals(project.getCreateName())) {
            throw new ProjectTaskException(403, "仅项目创建者或有效管理员可管理该项目");
        }
    }

    /**
     * 校验历史来源及旧暂存审核是否完成
     *
     * @param id 项目ID
     * @throws ProjectTaskException 审核状态或报告凭据不完整时抛出
     */
    public void reviewed(Integer id) {
        ProjectImportState state = mapper.state(id);
        if (state == null || !Objects.equals(state.getLegacyReviewed(), 1) ||
                state.getReviewReference() == null || state.getReviewReference().trim().isEmpty()) {
            throw new ProjectTaskException(409, "历史来源及旧暂存尚未核验，项目任务变更未开放");
        }
    }

    /**
     * 校验项目是否允许变更任务及评分来源
     *
     * @param project 已锁定的项目
     * @throws ProjectTaskException 项目已发布、未审核或已发生业务使用时抛出
     */
    public void mutable(Project project) {
        if (!Objects.equals(project.getStatus(), 0)) throw new ProjectTaskException(409, "项目已发布，任务及评分来源已冻结");
        reviewed(project.getId());
        ProjectImportState state = mapper.state(project.getId());
        if (state.getFirstStagedAt() != null || mapper.used(project.getId())) {
            throw new ProjectTaskException(409, "项目已有暂存、结果、材料或审批历史，任务及评分来源已冻结");
        }
    }

    /**
     * 在新建项目事务中初始化导入审核状态
     *
     * @param id 新建项目ID
     * @throws IllegalStateException 状态初始化失败时抛出
     */
    public void initialize(Integer id) {
        if (mapper.initialize(id) != 1) throw new IllegalStateException("初始化项目导入状态失败");
    }

    /**
     * 在项目锁内校验结果、任务及扩展项的归属
     * <p>
     * 重新读取当前任务和结果；客户端提供任务快照时校验评分字段，并统一结果的项目与用户ID。
     * </p>
     *
     * @param projectId 已锁定的项目ID
     * @param userId 结果所属用户ID
     * @param results 待暂存、提交或审批的结果列表
     * @param requireExisting 是否要求所有结果已存在于数据库
     * @throws ProjectTaskException 任务失效、重复或关联归属不匹配时抛出
     */
    public void validateTasks(Integer projectId, Integer userId, List<Result> results, boolean requireExisting) {
        if (userId == null || userId <= 0) throw new ProjectTaskException(400, "结果所属用户ID不能为空");
        if (results == null || results.isEmpty()) throw new ProjectTaskException(400, "结果列表不能为空");
        Map<String, Task> tasks = new HashMap<>();
        for (Task task : mapper.tasks(projectId)) tasks.put(task.getId(), task);
        Set<String> selected = new HashSet<>();
        for (Result result : results) {
            if (result == null || !tasks.containsKey(result.getTaskId()) || !selected.add(result.getTaskId()) ||
                    (result.getPId() != null && !projectId.equals(result.getPId()))) {
                throw new ProjectTaskException(409, "结果包含重复、失效或其他项目任务，请重新加载项目");
            }
            Task task = tasks.get(result.getTaskId());
            if (result.getTask() != null && (!Objects.equals(result.getTask().getId(), task.getId()) ||
                    !Objects.equals(result.getTask().getPId(), projectId) || !ProjectTaskRules.same(result.getTask(), task))) {
                throw new ProjectTaskException(409, "任务评分规则已变化，请重新加载项目");
            }
            Result stored = result.getId() == null ? null : mapper.result(result.getId());
            if ((requireExisting && stored == null) || (result.getId() != null && (stored == null ||
                    !projectId.equals(stored.getPId()) || !userId.equals(stored.getUId()) ||
                    !result.getTaskId().equals(stored.getTaskId())))) {
                throw new ProjectTaskException(409, "结果ID与当前项目、用户或任务不匹配");
            }
            if (result.getResultExtends() != null) {
                for (ResultExtend extension : result.getResultExtends()) {
                    if (extension == null || (extension.getTaskId() != null && !result.getTaskId().equals(extension.getTaskId())) ||
                            (extension.getResultId() != null && !Objects.equals(extension.getResultId(), result.getId())) ||
                            (extension.getUId() != null && !userId.equals(extension.getUId()))) {
                        throw new ProjectTaskException(409, "扩展项归属不匹配");
                    }
                    if (extension.getId() != null) {
                        ResultExtend existing = mapper.extension(extension.getId());
                        if (existing == null || stored == null || !Objects.equals(existing.getResultId(), stored.getId()) ||
                                !userId.equals(existing.getUId()) || !stored.getTaskId().equals(existing.getTaskId())) {
                            throw new ProjectTaskException(409, "扩展项ID与结果归属不匹配");
                        }
                    }
                }
            }
            result.setPId(projectId);
            result.setUId(userId);
        }
    }

    /**
     * 校验暂存任务并独立提交首次暂存冻结标记
     * <p>
     * 冻结标记先于 Redis 写入提交，缓存失败或过期不会重新开放任务变更。
     * </p>
     *
     * @param projectId 项目ID
     * @param user 当前用户
     * @param results 待暂存的结果列表
     * @throws ProjectTaskException 项目、任务或迁移状态不合法时抛出
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public void recordStaging(Integer projectId, User user, List<Result> results) {
        lock(projectId);
        validateTasks(projectId, user.getUserId(), results, false);
        if (mapper.state(projectId) == null) throw new ProjectTaskException(409, "项目导入状态尚未迁移");
        mapper.markStaged(projectId);
    }

    /**
     * 校验项目发布前的审核状态及来源一致性
     *
     * @param project 已锁定的未发布项目
     * @throws ProjectTaskException 项目未审核或任务与来源存在差异时抛出
     */
    public void publication(Project project) {
        if (!Objects.equals(project.getStatus(), 0)) throw new ProjectTaskException(409, "仅未发布项目可发布");
        reviewed(project.getId());
        for (Task task : mapper.tasks(project.getId())) {
            if (task.getSourceProjectItemId() == null) continue;
            ProjectItemSource item = mapper.source(task.getSourceProjectItemId());
            if (item == null || !ProjectTaskRules.same(task,
                    ProjectTaskRules.map(item, mapper.source(item.getParentId()), project.getId()))) {
                throw new ProjectTaskException(409, "任务与来源不一致，请先重新导入子项");
            }
        }
    }
}
