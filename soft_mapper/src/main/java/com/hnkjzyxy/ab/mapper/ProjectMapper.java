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

//@CacheNamespace(implementation = RedisCacheConfig.class)
public interface ProjectMapper extends BaseMapper<Project> {

    @Select("select A.id,A.title,A.describe,A.send_name sendName,A.start_time startTime,A.end_time endTime,A.create_time createTime,A.status,A.is_collect isCollect " +
            "from sys_project A where id = #{id}")
    Project getProjectById(@Param("id") String id);

    @Update("update sys_project set status = 1,create_time = now() where id = #{id}")
    void publishProject(@Param("id") String id);

    @Select("<script>select year(start_time) as startTime from sys_project" +
            " where status != 3 <if test='createName != null'>and create_name = #{createName}</if> order by startTime desc</script>")
    List<String> getProjectYears(@Param("createName") String createName);

    @Select("select year(start_time) as startTime from sys_project ${ew.customSqlSegment}")
    List<String> getUserProjectYears(@Param("ew") QueryWrapper<Project> wrapper);

    @Update("update sys_project set status = 0,create_time = null where id = #{id}")
    void updateProjectById(Integer id);

    @Select("select po.id as projectId,fo.id as flowId,po.`title` as projectName,po.`create_time` as createTime,po.`end_time` as endTime from sys_project as po,sys_flow as fo " +
            "where po.status != 3 and po.id = fo.p_id")
    List<ProjectVo> getProjectAssessList();

    List<FlowTask> getProjectAssessFlow(@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("pId") Integer projectId);

    @Select("select title from sys_project where status != 3 and id = #{pId}")
    String getProjectName(@Param("pId") Integer projectId);

    Project getProjectByTime(@Param("pId") Integer pId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select("select start_time as startTime,end_time as endTime from sys_project where id = #{pId}")
    Project getProjectTime(@Param("pId") Integer projectId);

    @Select("select count(*) from sys_project where title = #{title}")
    Integer selectProjectName(@Param("title") String title);

    FlowTask getRadarProjects(@Param("pId") Integer projectId);

    List<FlowTask> getColumnarProjects(@Param("year") String year, @Param("pId") Integer projectId);

    //@Select("select id,title from sys_project where date_format(start_time,'%Y') = #{year} and status = 1")
    List<Project> getProjectByYear(@Param("year") String year, @Param("pIds") List<Integer> pIds);
    List<Project> getProjectAndCollegeByYear(@Param("year") String year, @Param("pIds") List<Integer> pIds,String college);

    //@Select("select distinct year(start_time) from sys_project where status = 1")
    List<String> getYearByProject(@Param("pIds") List<Integer> pIds);


    @Select("select distinct category  from sys_task where p_id=#{projectId}")
    List<String> getCategoryByProjectName(@Param("projectId") Integer projectId);

    List<ProjectItemVo> getProjectItemById(@Param("projectId") Integer projectId);
}
