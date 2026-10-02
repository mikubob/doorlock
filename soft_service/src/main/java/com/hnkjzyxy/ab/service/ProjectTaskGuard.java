package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.User;

import java.util.List;

/**
 * 项目任务生命周期与并发保护Service接口
 * <p>
 * 定义项目锁、权限、审核状态及任务归属校验，事务边界由实现类保持。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
public interface ProjectTaskGuard {
    /**
     * 取得项目行锁并校验项目状态
     *
     * @param id 项目ID
     * @return 锁定后的项目信息
     * @throws ProjectTaskException 项目不存在或状态不允许操作时抛出
     * @throws IllegalStateException 未处于业务事务中时抛出
     */
    Project lock(Integer id);

    /**
     * 校验操作人的有效身份和项目管理权限
     *
     * @param project 已锁定的项目
     * @param operator 从认证主体取得的操作人
     * @throws ProjectTaskException 用户无效或无管理权限时抛出
     */
    void manage(Project project, User operator);

    /**
     * 校验历史来源及旧暂存审核是否完成
     *
     * @param id 项目ID
     * @throws ProjectTaskException 审核状态或报告凭据不完整时抛出
     */
    void reviewed(Integer id);

    /**
     * 校验项目是否允许变更任务及评分来源
     *
     * @param project 已锁定的项目
     * @throws ProjectTaskException 项目已发布、未审核或已发生业务使用时抛出
     */
    void mutable(Project project);

    /**
     * 在新建项目事务中初始化导入审核状态
     *
     * @param id 新建项目ID
     * @throws IllegalStateException 状态初始化失败时抛出
     */
    void initialize(Integer id);

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
    void validateTasks(Integer projectId, Integer userId, List<Result> results, boolean requireExisting);

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
    void recordStaging(Integer projectId, User user, List<Result> results);

    /**
     * 校验项目发布前的审核状态及来源一致性
     *
     * @param project 已锁定的未发布项目
     * @throws ProjectTaskException 项目未审核或任务与来源存在差异时抛出
     */
    void publication(Project project);
}
