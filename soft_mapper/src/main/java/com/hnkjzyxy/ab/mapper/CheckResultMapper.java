package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.CheckResult;
import com.hnkjzyxy.ab.vo.CheckResultByTeacherDataVo;
import com.hnkjzyxy.ab.vo.CheckResultDataVo;
import org.apache.ibatis.annotations.Param;

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
     * @return 按学院、班级及辅导员汇总的巡查统计列表
     */
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
     * @return 按学院及教师汇总的巡查统计列表
     */
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
     * @return 指定学院在日期范围内的巡查明细列表
     */
    List<CheckResult> findByDateRangeAndCollege(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("college") String college);

    /**
     * 按学院查询（支持多个学院）
     *
     * @param colleges 学院列表
     * @return 指定学院集合的巡查明细列表
     */
    List<CheckResult> findByCollege(@Param("colleges") List<String> colleges);

    /**
     * 按年度和学院汇总各班级每月的缺勤情况
     * <p>
     * 结果包含班级名称、月份、累计应到人数、实到人数、缺勤人数及缺勤率；
     * 缺勤率以百分数表示，保留两位小数。
     * </p>
     *
     * @param year 统计年度
     * @param college 学院名称
     * @return 按班级名称、月份排序的缺勤统计列表
     */
    List<Map<String, Object>> getMonthlyAbsenceDetailByCollege(
            @Param("year") Integer year,
            @Param("college") String college);

    /**
     * 按时间范围、学院分析所有老师授课班级缺勤率最高的10个班
     * 用于对比分析
     *
     * @param startDate 查询开始日期
     * @param endDate 查询结束日期
     * @param college 学院名称
     * @return 按缺勤率降序排列的班级及教师统计，最多十条
     */
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
    List<Map<String, Object>> getCollegeStatistics(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("colleges") List<String> colleges);

    /**
     * 查询所有不重复的学院列表
     *
     * @return 所有学院的列表，按字母顺序排序
     */
    List<String> getAllColleges();

    /**
     * 按指定时间顺序查询巡查结果
     *
     * @param order 排序方向
     * @return 教学巡查结果列表
     */
    List<CheckResult> sortedByTime(@Param("order") String order);

    /**
     * 按巡查时间和到课率排序查询巡查结果
     *
     * @param commuteTime 到课率排序方向，asc 为升序，其他值为降序
     * @param timeOrder 时间排序方向，asc 为升序，其他值为降序
     * @return 教学巡查结果列表
     */
    List<CheckResult> sortedByCommuteTime(@Param("commuteTime") String commuteTime, @Param("timeOrder") String timeOrder);

}
