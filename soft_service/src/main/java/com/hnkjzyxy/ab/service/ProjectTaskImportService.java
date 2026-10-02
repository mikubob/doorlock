package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectItemSaveParam;
import com.hnkjzyxy.ab.params.ProjectItemImportParam;
import com.hnkjzyxy.ab.vo.ProjectTaskImportResult;

import java.util.List;

/**
 * 项目子项任务导入与来源维护Service接口
 * <p>
 * 定义第一阶段 MERGE 导入以及使用共同项目锁和生命周期规则的来源维护操作。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
public interface ProjectTaskImportService {
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
    ProjectTaskImportResult merge(List<ProjectItemImportParam> request, User operator);
    /**
     * 新增或修改项目分类、子项
     *
     * @param request 分类或子项维护信息
     * @param operator 当前认证操作人
     * @throws ProjectTaskException 无管理权限、项目已冻结或来源字段不合法时抛出
     */
    void saveItem(ProjectItemSaveParam request, User operator);
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
    void deleteItem(Integer id, User operator);
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
    void deleteProject(Integer id, User operator);
}
