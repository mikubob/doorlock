package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.dto.ProjectAssessRow;
import com.hnkjzyxy.ab.dto.ProjectAssessScope;
import com.hnkjzyxy.ab.params.ProjectQueryParam;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.vo.ProjectItemVo;
import com.hnkjzyxy.ab.vo.ProjectVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 考核项目数据访问接口
 */
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 查找筛选项目中接收名单格式异常的首个有效CC节点
     *
     * @param param 项目及时间筛选条件
     * @return 异常节点ID，全部合法时返回null
     */
    Integer findInvalidAssessRecipient(@Param("param") ProjectQueryParam param);

    /**
     * 统计当前权限及全部筛选条件下的项目、用户组合数
     *
     * @param param 项目考核查询条件
     * @param scope 服务端生成的访问范围
     * @return 展示记录总数
     */
    long countAssessList(@Param("param") ProjectQueryParam param, @Param("scope") ProjectAssessScope scope);

    /**
     * 按总分、项目ID及用户ID排序查询当前页，不启用插件自动分页
     *
     * @param param 项目考核查询条件
     * @param scope 服务端生成的访问范围
     * @param offset 使用long计算的页偏移量
     * @param limit 当前页最大条数
     * @return 当前页记录及宽类型聚合分数
     */
    List<ProjectAssessRow> selectAssessListPage(@Param("param") ProjectQueryParam param,
                                              @Param("scope") ProjectAssessScope scope,
                                              @Param("offset") long offset, @Param("limit") int limit);

    /**
     * 查询考核项目详情
     *
     * @param id 考核项目ID
     * @return 考核项目信息
     */
    Project getProjectById(@Param("id") String id);

    /**
     * 将考核项目状态更新为已发布并记录发布时间
     *
     * @param id 考核项目ID
     */
    void publishProject(@Param("id") String id);

    /**
     * 查询考核项目年度
     *
     * @param createName 项目创建人用户名
     * @return 匹配创建人的项目年度列表
     */
    List<String> getProjectYears(@Param("createName") String createName);

    /**
     * 查询用户相关考核项目年度
     *
     * @param wrapper 数据库查询条件
     * @return 满足查询条件的项目年度列表
     */
    List<String> getUserProjectYears(@Param("ew") QueryWrapper<Project> wrapper);

    /**
     * 将考核项目恢复为未发布状态并清空发布时间
     *
     * @param id 考核项目ID
     */
    void updateProjectById(@Param("id") Integer id);

    /**
     * 查询项目考核结果列表
     *
     * @return 项目考核展示结果列表
     */
    List<ProjectVo> getProjectAssessList();

    /**
     * 查询项目在指定时间范围内的流程节点
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @param projectId 考核项目ID
     * @return 流程节点列表
     */
    List<FlowTask> getProjectAssessFlow(@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("pId") Integer projectId);

    /**
     * 查询考核项目名称
     *
     * @param projectId 考核项目ID
     * @return 项目名称，不存在或项目已删除时返回 null
     */
    String getProjectName(@Param("pId") Integer projectId);

    /**
     * 按项目ID和时间范围查询考核项目
     *
     * @param pId 考核项目ID
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 考核项目信息
     */
    Project getProjectByTime(@Param("pId") Integer pId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 查询项目开始及结束时间
     *
     * @param projectId 考核项目ID
     * @return 考核项目信息
     */
    Project getProjectTime(@Param("pId") Integer projectId);

    /**
     * 统计使用指定名称的考核项目数量
     *
     * @param title 项目名称
     * @return 同名考核项目数量
     */
    Integer selectProjectName(@Param("title") String title);

    /**
     * 查询项目雷达图涉及的流程节点
     *
     * @param projectId 考核项目ID
     * @return 流程节点信息
     */
    FlowTask getRadarProjects(@Param("pId") Integer projectId);

    /**
     * 查询年度柱状图涉及的项目流程节点
     *
     * @param year 统计年度
     * @param projectId 考核项目ID
     * @return 流程节点列表
     */
    List<FlowTask> getColumnarProjects(@Param("year") String year, @Param("pId") Integer projectId);

    /**
     * 按年度查询考核项目
     *
     * @param year 统计年度
     * @param pIds 考核项目ID集合
     * @return 考核项目列表
     */
    List<Project> getProjectByYear(@Param("year") String year, @Param("pIds") List<Integer> pIds);

    /**
     * 按年度和学院查询考核项目
     *
     * @param year 统计年度
     * @param pIds 考核项目ID集合
     * @param college 学院名称
     * @return 考核项目列表
     */
    List<Project> getProjectAndCollegeByYear(@Param("year") String year, @Param("pIds") List<Integer> pIds,@Param("college") String college);

    /**
     * 查询相关考核项目的年度
     *
     * @param pIds 考核项目ID集合
     * @return 指定项目集合去重后的年度列表
     */
    List<String> getYearByProject(@Param("pIds") List<Integer> pIds);

    /**
     * 查询项目的任务分类名称
     *
     * @param projectId 考核项目ID
     * @return 指定项目去重后的任务分类名称列表
     */
    List<String> getCategoryByProjectName(@Param("projectId") Integer projectId);

    /**
     * 查询项目分类及子项展示信息
     *
     * @param projectId 考核项目ID
     * @return 项目分类及子项的展示信息列表
     */
    List<ProjectItemVo> getProjectItemById(@Param("projectId") Integer projectId);
}
