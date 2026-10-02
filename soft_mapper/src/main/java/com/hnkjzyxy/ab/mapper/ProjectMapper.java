package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.vo.ProjectItemVo;
import com.hnkjzyxy.ab.vo.ProjectVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

//@CacheNamespace(implementation = MybatisRedisCache.class)
/**
 * 考核项目数据访问接口
 */
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 查询考核项目详情
     *
     * @param id 考核项目ID
     * @return 考核项目信息
     */
    @Select("select A.id,A.title,A.describe,A.send_name sendName,A.start_time startTime,A.end_time endTime,A.create_time createTime,A.status,A.is_collect isCollect " +
            "from sys_project A where id = #{id}")
    Project getProjectById(@Param("id") String id);

    /**
     * 将考核项目状态更新为已发布并记录发布时间
     *
     * @param id 考核项目ID
     */
    @Update("update sys_project set status = 1,create_time = now() where id = #{id}")
    void publishProject(@Param("id") String id);

    /**
     * 查询考核项目年度
     *
     * @param createName 项目创建人用户名
     * @return 查询结果列表
     */
    @Select("<script>select year(start_time) as startTime from sys_project" +
            " where status != 3 <if test='createName != null'>and create_name = #{createName}</if> order by startTime desc</script>")
    List<String> getProjectYears(@Param("createName") String createName);

    /**
     * 查询用户相关考核项目年度
     *
     * @param wrapper 数据库查询条件
     * @return 查询结果列表
     */
    @Select("select year(start_time) as startTime from sys_project ${ew.customSqlSegment}")
    List<String> getUserProjectYears(@Param("ew") QueryWrapper<Project> wrapper);

    /**
     * 将考核项目恢复为未发布状态并清空发布时间
     *
     * @param id 考核项目ID
     */
    @Update("update sys_project set status = 0,create_time = null where id = #{id}")
    void updateProjectById(Integer id);

    /**
     * 查询项目考核结果列表
     *
     * @return 项目考核展示结果列表
     */
    @Select("select po.id as projectId,fo.id as flowId,po.`title` as projectName,po.`create_time` as createTime,po.`end_time` as endTime from sys_project as po,sys_flow as fo " +
            "where po.status != 3 and po.id = fo.p_id")
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
     * @return 查询得到的文本信息
     */
    @Select("select title from sys_project where status != 3 and id = #{pId}")
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
    @Select("select start_time as startTime,end_time as endTime from sys_project where id = #{pId}")
    Project getProjectTime(@Param("pId") Integer projectId);

    /**
     * 统计使用指定名称的考核项目数量
     *
     * @param title 项目名称
     * @return 同名考核项目数量
     */
    @Select("select count(*) from sys_project where title = #{title}")
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

    //@Select("select id,title from sys_project where date_format(start_time,'%Y') = #{year} and status = 1")
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
    List<Project> getProjectAndCollegeByYear(@Param("year") String year, @Param("pIds") List<Integer> pIds,String college);

    //@Select("select distinct year(start_time) from sys_project where status = 1")
    /**
     * 查询相关考核项目的年度
     *
     * @param pIds 考核项目ID集合
     * @return 查询结果列表
     */
    List<String> getYearByProject(@Param("pIds") List<Integer> pIds);


    /**
     * 查询项目的任务分类名称
     *
     * @param projectId 考核项目ID
     * @return 查询结果列表
     */
    @Select("select distinct category  from sys_task where p_id=#{projectId}")
    List<String> getCategoryByProjectName(@Param("projectId") Integer projectId);

    /**
     * 查询项目分类及子项展示信息
     *
     * @param projectId 考核项目ID
     * @return 查询结果列表
     */
    List<ProjectItemVo> getProjectItemById(@Param("projectId") Integer projectId);
}
