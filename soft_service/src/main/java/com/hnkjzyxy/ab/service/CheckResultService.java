package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.CheckResult;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.vo.CheckResultByTeacherDataVo;
import com.hnkjzyxy.ab.vo.CheckResultDataVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface CheckResultService extends IService<CheckResult> {
    List<CheckResult> getList(CheckResult resultVo, User user);

    void addOrEdit(CheckResult resultVo);

    List<CheckResultDataVo> dataStatistics(CheckResult resultVo);

    List<CheckResult> dataStatisticsByClasses(CheckResult resultVo, User user);

    void uploadCheckResult(MultipartFile file) throws Exception;

    List<CheckResultByTeacherDataVo> teachCheckAnalysis(CheckResult resultVo);

    List<CheckResult> dataStatisticsByTeacher(CheckResult resultVo);

    /**
     * 按时间范围和学院查询
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param college   学院
     * @return 查询结果列表
     */

    List<CheckResult> findByDateRangeAndCollege(String startDate,
                                                String endDate,
                                                String college,
                                                User user);

    List<Map<String, Object>> getCollegeStatistics(
            String startDate,
            String endDate,
            List<String> colleges);

    /**
     * 按学院查询
     *
     * @return 查询结果列表
     */

    List<CheckResult> findByCollege(List<String> colleges);

    List<Map<String, Object>> getMonthlyAbsenceDetailByCollege(
            Integer year,
            String college,
            User user);

    /**
     * 按月、学院分析所有老师授课班级缺勤率最高的10个班
     * 用于对比分析
     */

    List<Map<String, Object>> getAbsenceClassesByCollegeAndDateRange(
            String startDate,
            String endDate,
            String college,
            User user);

    List<String> getAllColleges();

    List<CheckResult> sortedByTime(String order);

    List<CheckResult> sortedByCommuteTime(String commuteTime, String timeOrder);
}
