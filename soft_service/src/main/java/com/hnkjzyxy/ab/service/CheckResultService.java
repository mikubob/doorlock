package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.CheckResult;
import com.hnkjzyxy.ab.vo.CheckResultImportResult;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.vo.CheckResultByTeacherDataVo;
import com.hnkjzyxy.ab.vo.CheckResultDataVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 教学巡查结果Service接口
 */
public interface CheckResultService extends IService<CheckResult> {
    /**
     * 查询当前用户可见的教学巡查结果
     *
     * @param resultVo 教学巡查结果数据
     * @param user 当前用户
     * @return 教学巡查结果列表
     */
    List<CheckResult> getList(CheckResult resultVo, User user);

    /**
     * 新增或修改教学巡查结果
     *
     * @param resultVo 教学巡查结果数据
     */
    void addOrEdit(CheckResult resultVo);

    /**
     * 按班级汇总教学巡查数据
     *
     * @param resultVo 教学巡查结果数据
     * @return 查询结果列表
     */
    List<CheckResultDataVo> dataStatistics(CheckResult resultVo);

    /**
     * 查询班级维度的巡查明细
     *
     * @param resultVo 教学巡查结果数据
     * @param user 当前用户
     * @return 教学巡查结果列表
     */
    List<CheckResult> dataStatisticsByClasses(CheckResult resultVo, User user);

    /**
     * 上传巡查结果 Excel 并导入
     * <p>
     * 整次导入在单个事务内完成，解析失败的行被跳过并登记行号与原因。
     * </p>
     *
     * @param file 巡查结果 Excel 文件
     * @param user 当前上传人，用于按策略推导数据归属学院
     * @return 导入回执，含总行数 / 成功数 / 失败数 / 错误清单
     * @throws Exception 文件校验或解析异常
     */
    CheckResultImportResult uploadCheckResult(MultipartFile file, User user) throws Exception;

    /**
     * 按教师汇总教学巡查数据
     *
     * @param resultVo 教学巡查结果数据
     * @return 查询结果列表
     */
    List<CheckResultByTeacherDataVo> teachCheckAnalysis(CheckResult resultVo);

    /**
     * 查询教师维度的巡查明细
     *
     * @param resultVo 教学巡查结果数据
     * @return 教学巡查结果列表
     */
    List<CheckResult> dataStatisticsByTeacher(CheckResult resultVo);

    /**
     * 按时间范围和学院查询
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param college   学院
     * @param user 当前用户
     * @return 查询结果列表
     */

    List<CheckResult> findByDateRangeAndCollege(String startDate,
                                                String endDate,
                                                String college,
                                                User user);

    /**
     * 查询指定时间范围内各学院的巡查统计
     *
     * @param startDate 查询开始日期
     * @param endDate 查询结束日期
     * @param colleges 学院名称集合
     * @return 查询数据及相关统计信息列表
     */
    List<Map<String, Object>> getCollegeStatistics(
            String startDate,
            String endDate,
            List<String> colleges);

    /**
     * 按学院查询
     *
     * @param colleges 学院名称集合
     * @return 查询结果列表
     */

    List<CheckResult> findByCollege(List<String> colleges);

    /**
     * 查询指定年度和学院的月度缺勤明细
     *
     * @param year 统计年度
     * @param college 学院名称
     * @param user 当前用户
     * @return 查询数据及相关统计信息列表
     */
    List<Map<String, Object>> getMonthlyAbsenceDetailByCollege(
            Integer year,
            String college,
            User user);

    /**
     * 按月、学院分析所有老师授课班级缺勤率最高的10个班
     * 用于对比分析
     *
     * @param startDate 查询开始日期
     * @param endDate 查询结束日期
     * @param college 学院名称
     * @param user 当前用户
     * @return 查询数据及相关统计信息列表
     */

    List<Map<String, Object>> getAbsenceClassesByCollegeAndDateRange(
            String startDate,
            String endDate,
            String college,
            User user);

    /**
     * 查询巡查数据涉及的学院名称
     *
     * @return 查询结果列表
     */
    List<String> getAllColleges();

    /**
     * 按指定时间顺序查询巡查结果
     *
     * @param order 排序方向
     * @return 教学巡查结果列表
     */
    List<CheckResult> sortedByTime(String order);

    /**
     * 按上课节次和时间顺序查询巡查结果
     *
     * @param commuteTime 上课节次
     * @param timeOrder 时间排序方向
     * @return 教学巡查结果列表
     */
    List<CheckResult> sortedByCommuteTime(String commuteTime, String timeOrder);
}
