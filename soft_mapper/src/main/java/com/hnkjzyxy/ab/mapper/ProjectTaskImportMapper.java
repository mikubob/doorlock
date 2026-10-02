package com.hnkjzyxy.ab.mapper;

import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.ProjectImportState;
import com.hnkjzyxy.ab.model.ProjectItemSource;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.ResultExtend;
import com.hnkjzyxy.ab.model.Task;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目任务导入专用数据访问接口
 * <p>
 * 提供原始来源查询、项目行锁、审核状态及业务引用检查；写入链路先锁项目再访问相关数据。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
public interface ProjectTaskImportMapper {
    /**
     * 锁定项目记录并读取当前状态
     * <p>
     * 行锁持有到业务事务结束，同时清空 MyBatis 本地缓存以避免使用锁前快照。
     * </p>
     *
     * @param id 项目ID
     * @return 项目信息，不存在时返回 null
     */
    Project lockProject(@Param("id") Integer id);

    /**
     * 读取项目分类或子项的原始字段
     *
     * @param id 来源记录ID
     * @return 原始来源数据，不存在时返回 null
     */
    ProjectItemSource source(@Param("id") Integer id);

    /**
     * 查询项目当前任务集合
     *
     * @param id 项目ID
     * @return 项目任务列表
     */
    List<Task> tasks(@Param("id") Integer id);

    /**
     * 查询项目导入审核及暂存冻结状态
     *
     * @param id 项目ID
     * @return 持久化状态，尚未迁移时返回 null
     */
    ProjectImportState state(@Param("id") Integer id);

    /**
     * 初始化新建项目的导入审核状态
     *
     * @param id 新建项目ID
     * @return 插入记录数
     */
    int initialize(@Param("id") Integer id);

    /**
     * 记录首次暂存时间，已有标记保持原值
     *
     * @param id 项目ID
     * @return 更新记录数
     */
    int markStaged(@Param("id") Integer id);

    /**
     * 核验认证主体对应的有效用户
     *
     * @param id 认证用户ID
     * @param name 认证用户名（工号）
     * @return 匹配的启用用户数量
     */
    int activeUser(@Param("id") Integer id, @Param("name") String name);

    /**
     * 查询用户是否拥有有效 admin 角色
     *
     * @param id 用户ID
     * @return 有效管理员角色关系数量
     */
    int activeAdmin(@Param("id") Integer id);

    // 不过滤零分、空材料或逻辑删除的审批历史；同时按任务与结果归属保护异常关联。
    /**
     * 检查项目是否存在结果、扩展材料或审批历史
     * <p>
     * 包含零分结果、未完成结果及逻辑删除的审批历史，扩展项同时按任务和结果归属检查。
     * </p>
     *
     * @param id 项目ID
     * @return 存在任意业务引用时返回 true
     */
    boolean used(@Param("id") Integer id);

    /**
     * 按项目及来源更新任务评分字段
     * <p>
     * 保留任务主键，并显式写入空备注以支持字段清空。
     * </p>
     *
     * @param task 待更新的完整导入字段
     * @return 更新记录数
     */
    int updateTask(Task task);

    /**
     * 查询待校验的结果记录
     *
     * @param id 结果ID
     * @return 结果记录，不存在时返回 null
     */
    Result result(@Param("id") Integer id);

    /**
     * 查询待校验的扩展项记录
     *
     * @param id 扩展项ID
     * @return 扩展项记录，不存在时返回 null
     */
    ResultExtend extension(@Param("id") Integer id);

    /**
     * 查询任务及其真实项目归属
     *
     * @param id 任务ID
     * @return 任务记录，不存在时返回 null
     */
    Task task(@Param("id") String id);

    /**
     * 查询指定用户在项目任务下的结果数量
     *
     * @param pId 项目ID
     * @param taskId 任务ID
     * @param uId 用户ID
     * @return 匹配的结果记录数
     */
    int scoreResults(@Param("pId") Integer pId, @Param("taskId") String taskId, @Param("uId") Integer uId);
}
