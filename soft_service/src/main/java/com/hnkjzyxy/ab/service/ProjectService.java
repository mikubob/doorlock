package com.hnkjzyxy.ab.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectParam;
import com.hnkjzyxy.ab.params.ProjectQueryParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.params.ProjectItemSaveParam;
import com.hnkjzyxy.ab.vo.ResultVo;
import com.hnkjzyxy.ab.params.ProjectResultSubmitParam;

import java.util.List;
import java.util.Map;

/**
 * 考核项目Service接口
 *
 * @author 16702
 */
public interface ProjectService extends IService<Project> {
    /**
     * 分页查询考核项目
     *
     * @param param 考核项目操作或查询参数
     * @return 查询数据及相关统计信息
     */
    Map<String, Object> getProjectList(ProjectParam param);

    /**
     * 分页查询用户参与的考核项目
     *
     * @param param 考核项目操作或查询参数
     * @return 查询数据及相关统计信息
     */
    Map<String, Object> getUserProjectList(ProjectParam param);

    /**
     * 查询用户角色ID集合
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Integer> getRoles(Integer userId);

    /**
     * 查询用户相关的已结束考核项目
     *
     * @param userId 用户ID
     * @return 考核项目信息
     */
    Project getEndProject(Integer userId);

    /**
     * 创建考核项目并初始化任务导入审核状态
     *
     * @param project 考核项目信息
     * @param user 当前用户
     */
    void addProject(Project project, User user);

    /**
     * 查询考核项目详情
     *
     * @param id 考核项目ID
     * @param user 当前用户
     * @return 考核项目信息
     */
    Project getProjectById(String id, User user);

    /**
     * 校验项目任务来源及审核状态后发布考核项目
     *
     * @param id 考核项目ID
     * @param user 当前用户
     */
    void publishProject(String id, User user);

    /**
     * 校验任务归属及项目状态后提交考核结果
     *
     * @param result 项目结果提交及暂存参数信息
     * @param user 当前用户
     */
    void resultProject(ProjectResultSubmitParam result, User user);

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
    void projectStaging(ProjectResultSubmitParam result, User user);

    /**
     * 读取用户指定项目的暂存结果
     *
     * @param user 当前认证用户
     * @param pId 考核项目ID
     * @return 暂存结果；缓存键不存在或缓存值为空时返回 null
     */
    ResultVo getProjectStaging(User user, Integer pId);

    /**
     * 查询考核项目年度
     *
     * @param param 考核项目操作或查询参数
     * @return 查询结果列表
     */
    List<String> getProjectYears(ProjectParam param);

    /**
     * 查询用户相关考核项目年度
     *
     * @param param 考核项目操作或查询参数
     * @return 查询结果列表
     */
    List<String> getUserProjectYears(ProjectParam param);

    /**
     * 查询项目考核结果列表
     *
     * @param param 考核项目操作或查询参数
     * @param user 当前用户
     * @return 统一接口响应
     */
    ApiResult getProjectAssessList(ProjectQueryParam param, User user);

    /**
     * 按年度查询考核项目
     *
     * @param year 统计年度
     * @param user 当前用户
     * @return 考核项目列表
     */
    List<Project> getProjectByYear(String year, User user);

    /**
     * 查询相关考核项目的年度
     *
     * @param user 当前用户
     * @return 查询结果列表
     */
    List<String> getYearByProject(User user);

    /**
     * 新增或修改项目分类、子项
     *
     * @param projectItemVo 分类或子项维护信息
     * @param operator 当前认证操作人
     */
    void addOrUpdateProjectItem(ProjectItemSaveParam projectItemVo, User operator);
    /**
     * 按年度和学院查询考核项目
     *
     * @param year 统计年度
     * @param college 学院名称
     * @param user 当前用户
     * @return 考核项目列表
     */
    List<Project> getProjectAndCollegeByYear(String year, String college,User user);
}
