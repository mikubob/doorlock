package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.CheckResult;
import com.hnkjzyxy.ab.vo.CheckResultByTeacherDataVo;
import com.hnkjzyxy.ab.vo.CheckResultDataVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 教学巡查结果数据访问接口
 */
public interface CheckResultMapper extends BaseMapper<CheckResult> {


    /**
     * 查询时间范围内的班级巡查统计
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 查询结果列表
     */
    @Select("select a.college, a.classes,a.counsellor,count(*) as count , avg(a.arrival_rate) as avgArrivalRate  ,avg(a.food_bring_rate) as avgFoodBringRate " +
            "from sys_check_result a where a.date between #{startTime} and #{endTime}  GROUP BY a.college,a.classes ,a.counsellor order by avgArrivalRate desc")
    List<CheckResultDataVo> selectByTime(@Param("startTime") String startTime, @Param("endTime") String endTime);


    /**
     * 按班级、学院和时间范围查询巡查明细
     *
     * @param classes 班级名称
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @param college 学院名称
     * @return 教学巡查结果列表
     */
    List<CheckResult> selectListByClasses(@Param("classes") String classes, @Param("startTime") String startTime, @Param("endTime") String endTime, @Param("college") String college);

    /**
     * 查询时间范围内的教师巡查统计
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 查询结果列表
     */
    @Select("select a.college,a.teacher,count(*) as count , avg(a.arrival_rate) as avgArrivalRate  ,avg(a.food_bring_rate) as avgFoodBringRate" +
            " from sys_check_result a  where a.date between #{startTime} and #{endTime} GROUP BY a.college,a.teacher order by avgArrivalRate desc")
    List<CheckResultByTeacherDataVo> selectByTimeAndTeacher(@Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 按教师和时间范围查询巡查明细
     *
     * @param teacher 教师名称
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 教学巡查结果列表
     */
    List<CheckResult> selectListByTeacher(@Param("teacher") String teacher, @Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 按时间范围和学院查询
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param college   学院
     * @return 查询结果列表
     */
    @Select("SELECT * FROM sys_check_result WHERE date BETWEEN #{startDate} AND #{endDate} AND college = #{college}")
    List<CheckResult> findByDateRangeAndCollege(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("college") String college);

    /**
     * 按学院查询（支持多个学院）
     *
     * @param colleges 学院列表
     * @return 查询结果列表
     */
    @Select("<script>" +
            "SELECT * FROM sys_check_result WHERE college IN " +
            "<foreach collection='colleges' item='college' open='(' separator=',' close=')'>" +
            "#{college}" +
            "</foreach>" +
            "</script>")
    List<CheckResult> findByCollege(@Param("colleges") List<String> colleges);

    /**
     * 查询某年某学院的每个班的每个月的详细缺勤统计
     * 返回更详细的信息
     *
     * @param year 统计年度
     * @param college 学院名称
     * @return 查询数据及相关统计信息列表
     */
    @Select("SELECT " +
            "    classes as className, " +
            "    MONTH(date) as month, " +
            "    SUM(should_arrival) as totalShouldArrival, " +
            "    SUM(arrival) as totalArrival, " +
            "    SUM(should_arrival - arrival) as totalAbsence, " +
            "    ROUND((SUM(should_arrival) - SUM(arrival)) * 100.0 / SUM(should_arrival), 2) as absenceRate " +
            "FROM sys_check_result " +
            "WHERE YEAR(date) = #{year} " +
            "    AND college = #{college} " +
            "    AND should_arrival > 0 " +
            "GROUP BY classes, MONTH(date) " +
            "ORDER BY classes, month")
    /**
     * {
     *   "absenceRate": 0.00,        // 缺勤率：0.00% 表示该月没有缺勤
     *   "month": 11,                // 月份：11月
     *   "totalArrival": 102,        // 总实到人数：该月累计实到102人
     *   "totalAbsence": 0,          // 总缺勤人数：该月累计缺勤0人
     *   "className": "软件游戏3232班", // 班级名称：软件游戏3232班
     *   "totalShouldArrival": 102   // 总应到人数：该月累计应到102人
     * }
     */
    List<Map<String, Object>> getMonthlyAbsenceDetailByCollege(
            @Param("year") Integer year,
            @Param("college") String college);


    /**
     * 按月、学院分析所有老师授课班级缺勤率最高的10个班
     * 用于对比分析
     */
    /**
     * 按时间范围、学院分析所有老师授课班级缺勤率最高的10个班
     * 用于对比分析
     *
     * @param startDate 查询开始日期
     * @param endDate 查询结束日期
     * @param college 学院名称
     * @return 查询数据及相关统计信息列表
     */
    @Select("SELECT " +
            "    classes as className, " +
            "    teacher as teacherName, " +
            "    SUM(should_arrival) as totalShouldArrival, " +
            "    SUM(arrival) as totalArrival, " +
            "    SUM(should_arrival - arrival) as totalAbsence, " +
            "    ROUND((SUM(should_arrival) - SUM(arrival)) * 100.0 / SUM(should_arrival), 2) as absenceRate " +
            "FROM sys_check_result " +
            "WHERE date BETWEEN #{startDate} AND #{endDate} " +
            "    AND college = #{college} " +
            "    AND should_arrival > 0 " +
            "GROUP BY classes, teacher " +
            "ORDER BY absenceRate DESC " +
            "LIMIT 10")
    List<Map<String, Object>> getAbsenceClassesByCollegeAndDateRange(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("college") String college);

    /**
     * 按学院统计缺勤率、请假率和带食物率
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @param colleges  学院列表
     * @return 统计结果
     */
    @Select("<script>" +
            "SELECT " +
            "    college, " +
            "    SUM(should_arrival) as totalShouldArrival, " +
            "    SUM(arrival) as totalArrival, " +
            "    SUM(people_leave) as totalLeave, " +
            "    SUM(food_bring_person) as totalFoodBring, " +
            "    ROUND((SUM(should_arrival) - SUM(arrival)) * 100.0 / SUM(should_arrival), 2) as absenceRate, " +
            "    ROUND(SUM(people_leave) * 100.0 / SUM(should_arrival), 2) as leaveRate, " +
            "    ROUND(SUM(food_bring_person) * 100.0 / SUM(should_arrival), 2) as foodBringRate " +
            "FROM sys_check_result " +
            "WHERE date BETWEEN #{startDate} AND #{endDate} " +
            "    AND should_arrival > 0 " +
            "    <if test='colleges != null and colleges.size() > 0'>" +
            "        AND college IN " +
            "        <foreach collection='colleges' item='college' open='(' separator=',' close=')'>" +
            "            #{college}" +
            "        </foreach>" +
            "    </if>" +
            "GROUP BY college " +
            "ORDER BY college" +
            "</script>")
    List<Map<String, Object>> getCollegeStatistics(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("colleges") List<String> colleges);

    /**
     * 查询所有不重复的学院列表
     *
     * @return 所有学院的列表，按字母顺序排序
     */
    @Select("SELECT DISTINCT college " +
            "FROM sys_check_result " +
            "WHERE college IS NOT NULL " +
            "AND college != '' " +
            "ORDER BY college")
    List<String> getAllColleges();
    /**
     * 按指定时间顺序查询巡查结果
     *
     * @param order 排序方向
     * @return 教学巡查结果列表
     */
    @Select("<script>" +
            "SELECT * FROM sys_check_result ORDER BY date " +
            "<choose>" +
            "<when test='order == \"asc\"'>ASC</when>" +
            "<otherwise>DESC</otherwise>" +
            "</choose>" +
            "</script>")
    List<CheckResult> sortedByTime(@Param("order") String order);
    /**
     * 按上课节次和时间顺序查询巡查结果
     *
     * @param commuteTime 上课节次
     * @param timeOrder 时间排序方向
     * @return 教学巡查结果列表
     */
    @Select("<script>" +
            "SELECT * FROM sys_check_result " +
            "ORDER BY " +
            "<choose>" +
            "<when test='timeOrder == \"asc\"'>date ASC</when>" +
            "<when test='timeOrder == \"desc\"'>date DESC</when>" +
            "<otherwise>date DESC</otherwise>" +
            "</choose>" +
            ", " +
            "<choose>" +
            "<when test='commuteTime == \"asc\"'>arrival_rate ASC</when>" +
            "<when test='commuteTime == \"desc\"'>arrival_rate DESC</when>" +
            "<otherwise>arrival_rate DESC</otherwise>" +
            "</choose>" +
            "</script>")
    List<CheckResult> sortedByCommuteTime(@Param("commuteTime") String commuteTime, @Param("timeOrder") String timeOrder);


}
