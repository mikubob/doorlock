package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.dto.SubTaskIdDto;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.vo.SubTaskVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.HashSet;
import java.util.List;

//@CacheNamespace(implementation = MybatisRedisCache.class)
public interface ResultMapper extends BaseMapper<Result> {

    @Select("select count(*) from sys_result where (p_id = #{pId} and u_id = #{uId}) and (is_finish = 0 or is_finish = 1)")
    Integer selectByPId(@Param("pId") Integer pId, @Param("uId") Integer uId);

    @Select("select * from sys_result where p_id = #{pId} and step >= #{step} and is_finish != 2")
    List<Result> selectResultCountByPId(@Param("pId") Integer pId, @Param("step") Integer step);

    @Select("select count(*) from sys_result where date_format(create_time,'%Y-%m') = concat(#{year},'-',#{month}) and u_id=#{userId}")
    Integer getUserResultMonthCount(@Param("userId") Integer userId, @Param("year") String year, @Param("month") String month);

    @Select("select count(*) from sys_result where date_format(create_time,'%Y-%m-%d') = #{s} and u_id = #{userId} and is_finish = 1")
    Integer getUserResultWeekCount(@Param("s") String s, @Param("userId") Integer userId);

    @Select("select * from sys_result where u_id = #{uId} and p_id = #{pId}")
    List<Result> findByUIdAndPID(@Param("uId") Integer userId, @Param("pId") Integer pId);

    @Select("select evidence from sys_result")
    List<String> getResultEvidence();

    @Select("select count(*) from sys_result where p_id = #{pId} and step >= #{step}")
    Integer selectApproveCountByPId(@Param("pId") Integer pId, @Param("step") Integer step);

    @Select("select is_finish from sys_result where u_id = #{uId} and p_id = #{pId} and step = 0 limit 1")
    Integer selectResultByPIdAndUId(@Param("uId") Integer uId, @Param("pId") Integer pId);

    @Select("select * from sys_result where u_id = #{uId} and p_id = #{pId} limit 1")
    Result selectResult(@Param("uId") Integer uId, @Param("pId") Integer pId);


    @Select("select * from sys_result where DATE_FORMAT(update_time,'%Y') = #{num} and u_id = #{uId} and is_finish = 1")
    List<Result> selectListByYear(@Param("num") Integer num, @Param("uId") Integer uId);

    @Select("select distinct year(start_time) from sys_project")
    List<String> getFinishScaleYears(@Param("uId") Integer uId);


    @Select("select distinct u_id from sys_result where p_id = #{pId} and step >= #{step} and (is_finish = 4 or is_finish = 1)")
    List<Integer> getUserByResult(Integer pId, Integer step);


    @Select("select sum(score) from sys_result where u_id = #{uId} and p_id = #{pId}")
    Integer findTotalScore(@Param("uId") Integer item, @Param("pId") Integer projectId);

    @Select("select is_finish from sys_result where u_id = #{uId} and p_id = #{pId} limit 1")
    Integer findResultStatus(@Param("uId") Integer item, @Param("pId") Integer projectId);

    @Select("select distinct p_id from sys_result where u_id = #{uId}")
    List<Integer> getProjectIds(@Param("uId") Integer userId);

    @Select("select evidence from sys_result where u_id = #{uId} and p_id = #{pId}")
    List<String> selectEvidenceList(@Param("pId") Integer projectId, @Param("uId") Integer userId);


    @Select("select distinct re.u_id from sys_result re,sys_user_role ur where re.p_id = #{pId} and re.step >= #{step} and (re.is_finish = 4 or re.is_finish = 1) and ur.role_id = #{role} " +
            "and re.u_id = ur.user_id")
    HashSet<Integer> getUserIdByRole(Integer role, Integer pId, Integer step);

    @Select("select distinct u_id from sys_result where p_id = #{pId} and step >= #{step} and (is_finish = 4 or is_finish = 1)")
    HashSet<Integer> selectUserIdByStep(@Param("pId") Integer pId, @Param("step") Integer step);

    @Select("select u_id from sys_result where p_id = #{pId} and u_id = #{uId} and step >= #{step} and (is_finish = 4 or is_finish = 1) limit 1")
    Integer checkResultByUId(@Param("pId") Integer pId, @Param("step") Integer step, @Param("uId") Integer val);

    List<Integer> checkResultScore(@Param("pId") Integer projectId, @Param("uIds") HashSet<Integer> uIds, @Param("category") String category);

    List<Integer> getPIdByRadar(@Param("pId") Integer projectId, @Param("year") String year, @Param("uId") Integer userId);

    List<Integer> getProjectResult(@Param("pId") Integer projectId, @Param("uIds") HashSet<Integer> uIds);

    List<SubTaskVo> getUsersSubTaskScore(@Param("title") String title, @Param("taskName") String taskName, @Param("taskCategory") String taskCategory);

    /**
     * 通过 ID 查询子任务，参数只需要 ID 和限制的分数
     *
     * @param dto 子任务查询参数
     * @return 子任务分数列表
     */
    List<SubTaskVo> getUsersSubTaskScoreById(SubTaskIdDto dto);

    void updateBySubTaskName(@Param("list") List<SubTaskIdDto> dto);
    List<Integer> selectScoreByDepartment(@Param("pId") Integer projectId, @Param("category") String category, @Param("department") String department);

    List<Integer> selectScoreByDepartmentAndCollege(@Param("pId") Integer projectId, @Param("category") String category);

    List<Integer> selectScoreByTeacher(@Param("pId") Integer projectId, @Param("teacher") String teacher);
    List<String> selectTaskByProjectAndTeacher(@Param("pId") Integer projectId,@Param("teacherId") String teacherId);

}
