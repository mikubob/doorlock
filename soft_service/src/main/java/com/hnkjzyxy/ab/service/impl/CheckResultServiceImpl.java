package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.config.CheckResultImportProperties;
import com.hnkjzyxy.ab.exception.ImportRejectedException;
import com.hnkjzyxy.ab.mapper.CheckResultMapper;
import com.hnkjzyxy.ab.mapper.CourseScheduleMapper;
import com.hnkjzyxy.ab.model.CheckResult;
import com.hnkjzyxy.ab.model.CourseSchedule;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.CheckResultService;
import com.hnkjzyxy.ab.service.CoursePeriodResolver;
import com.hnkjzyxy.ab.service.excel.CheckResultExcelImportService;
import com.hnkjzyxy.ab.vo.CheckResultByTeacherDataVo;
import com.hnkjzyxy.ab.vo.CheckResultDataVo;
import com.hnkjzyxy.ab.vo.CheckResultImportResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 教学巡查结果管理
 * 负责巡查记录 Excel 导入、结果查询与统计
 *
 * @version 1.0
 * @projectName: assessment
 * @author: Lucas
 * @date: 2024/4/23 20:25
 */
@Slf4j
@Service
public class CheckResultServiceImpl extends ServiceImpl<CheckResultMapper, CheckResult> implements CheckResultService {

    /**
     * 教学巡查结果数据访问接口
     */
    @Autowired
    private CheckResultMapper checkresultMapper;

    /**
     * 巡查 Excel 导入规则配置
     */
    @Autowired
    private CheckResultImportProperties checkResultImportProperties;
    /**
     * 巡查结果 Excel 导入服务
     */
    @Autowired
    private CheckResultExcelImportService checkResultExcelImportService;
    /**
     * 课程身份访问。
     */
    @Autowired
    private CourseScheduleMapper courseScheduleMapper;
    /**
     * 学校时间规则。
     */
    @Autowired
    private CoursePeriodResolver periodResolver;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CheckResult> getList(CheckResult resultVo, User user) {
        LambdaQueryWrapper<CheckResult> wrapper = new LambdaQueryWrapper<>();

        //班级和时间

        if (resultVo != null) {
            wrapper.eq(resultVo.getClasses() != null, CheckResult::getClasses, resultVo.getClasses()).eq(user.getCollege() != null, CheckResult::getCollege, user.getCollege());

            wrapper.between(resultVo.getEndTime() != null && resultVo.getStartTime() != null
                    , CheckResult::getStartTime, resultVo.getStartTime(), resultVo.getEndTime());

        }

        List<CheckResult> checkResults = checkresultMapper.selectList(wrapper);
        return checkResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void addOrEdit(CheckResult resultVo) {
        if (resultVo.getId() == null) {
            if (resultVo.getCourseKey() != null && !resultVo.getCourseKey().trim().isEmpty()) {
                List<CourseSchedule> matches = courseScheduleMapper.selectList(
                        new LambdaQueryWrapper<CourseSchedule>()
                                .eq(CourseSchedule::getCourseKey, resultVo.getCourseKey()));
                if (matches.size() != 1) throw new IllegalArgumentException("课程身份存在歧义，请重新选择或填写补录理由");
                CourseSchedule course = matches.get(0);
                if (!Objects.equals(course.getDepartmentName(), resultVo.getCollege())) throw new IllegalArgumentException("巡查学院与课程来源不一致");
                LocalDate day = Instant.ofEpochMilli(resultVo.getDate().getTime()).atZone(CoursePeriodResolver.ZONE).toLocalDate();
                if (Integer.valueOf(0).equals(course.getEffective()) || "PENDING".equals(course.getParseStatus())
                        || !periodResolver.date(course.getClassDate()).equals(day)
                        || !Objects.equals(course.getClassName(), resultVo.getClasses())
                        || !Objects.equals(course.getClassPeriod(), resultVo.getSection())
                        || !Objects.equals(course.getClassroomNumber(), resultVo.getClassroom())) {
                    throw new IllegalArgumentException("巡查日期、节次、班级或教室与有效课程不一致，请重新核对");
                }
                resultVo.setScheduleSnapshot(course.toString() + "; intervals=" + periodResolver.resolve(course));
            } else {
                if (resultVo.getSupplementReason() == null || resultVo.getSupplementReason().trim().isEmpty()) throw new IllegalArgumentException("无可靠课表关联时必须填写人工补录理由");
                resultVo.setScheduleSnapshot("manual; date=" + resultVo.getDate() + "; section=" + resultVo.getSection()
                        + "; classroom=" + resultVo.getClassroom() + "; classes=" + resultVo.getClasses() + "; teacher=" + resultVo.getTeacher());
            }
            resultVo.setLeaveSource(resultVo.getPeopleLeave() == null ? "UNKNOWN" : "MANUAL_CONFIRMED");
            if (checkresultMapper.insert(resultVo) != 1) throw new IllegalStateException("巡查记录保存失败");
        } else {
            CheckResult previous = checkresultMapper.selectById(resultVo.getId());
            if (previous == null) throw new IllegalArgumentException("巡查记录不存在");
            if (previous.getCourseKey() != null && ((!Objects.equals(previous.getDate(), resultVo.getDate()) && resultVo.getDate() != null)
                    || (resultVo.getSection() != null && !Objects.equals(previous.getSection(), resultVo.getSection()))
                    || (resultVo.getClasses() != null && !Objects.equals(previous.getClasses(), resultVo.getClasses()))
                    || (resultVo.getClassroom() != null && !Objects.equals(previous.getClassroom(), resultVo.getClassroom())))) {
                throw new IllegalArgumentException("已关联的历史巡查日期、节次、班级及教室不能更改，请另行补录并保留历史");
            }
            resultVo.setCourseKey(previous.getCourseKey()); resultVo.setScheduleSnapshot(previous.getScheduleSnapshot());
            resultVo.setSupplementReason(previous.getSupplementReason()); resultVo.setLeaveSource(previous.getLeaveSource());
            if (resultVo.getPeopleLeave() == null) resultVo.setPeopleLeave(previous.getPeopleLeave());
            else resultVo.setLeaveSource("MANUAL_CONFIRMED");
            if (checkresultMapper.updateById(resultVo) != 1) throw new IllegalStateException("巡查历史更新失败");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CheckResultDataVo> dataStatistics(CheckResult resultVo) {


        String startTime = resultVo.getStartTime();
        String endTime = resultVo.getEndTime();
        List<CheckResultDataVo> resultDataVos = checkresultMapper.selectByTime(startTime, endTime);


        resultDataVos.forEach(dataVo -> {
            dataVo.setStartTime(startTime);
            dataVo.setEndTime(endTime);

        });
        return resultDataVos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CheckResult> dataStatisticsByClasses(CheckResult resultVo, User user) {

        List<CheckResult> checkResults = checkresultMapper.selectListByClasses(resultVo.getClasses(), resultVo.getStartTime(), resultVo.getEndTime(), user.getCollege());
        return checkResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CheckResultImportResult uploadCheckResult(MultipartFile file, User user) throws Exception {
        CheckResultImportProperties properties = checkResultImportProperties;
        // 总开关关闭时直接拒绝，不解析也不写库
        if (!properties.isEnabled()) {
            throw new ImportRejectedException("巡查结果导入功能已停用，请联系管理员");
        }
        // 学院来源策略：FROM_UPLOADER / MANUAL 均以「上传人所属学院」作为候选值，逐行落地时再校验
        String sourceCollege = user == null ? null : user.getCollege();
        // 整次导入的事务边界统一收在监听器内部（解析完成 → 单事务落库），此处不再叠加事务，避免嵌套误导
        try {
            return checkResultExcelImportService.readScheduleExcel(file, sourceCollege);
        } catch (ImportRejectedException e) {
            // 整次导入被拒绝属于正常的业务结果（数据量超限 / 有错即放弃 / 表头严格校验未通过），
            // 原样上抛由接口层呈现明确失败，避免被包装成「成功 0 条」的假成功
            log.warn("[巡查导入] 已放弃整次导入：{}", e.getReason());
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CheckResultByTeacherDataVo> teachCheckAnalysis(CheckResult resultVo) {
        String startTime = resultVo.getStartTime();
        String endTime = resultVo.getEndTime();
        List<CheckResultByTeacherDataVo> resultDataVos = checkresultMapper.selectByTimeAndTeacher(startTime, endTime);
        resultDataVos.forEach(dataVo -> {
            dataVo.setStartTime(startTime);
            dataVo.setEndTime(endTime);
        });

        return resultDataVos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CheckResult> dataStatisticsByTeacher(CheckResult resultVo) {

        //通过任课老师来进行查询

        return checkresultMapper.selectListByTeacher(resultVo.getTeacher(), resultVo.getStartTime(), resultVo.getEndTime());

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<CheckResult> findByDateRangeAndCollege(String startDate, String endDate, String college, User user) {
        if (user.getNickName()=="管理员"){
            return checkresultMapper.findByDateRangeAndCollege(startDate, endDate, college );
        }
        return checkresultMapper.findByDateRangeAndCollege(startDate, endDate, user.getCollege() );

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, Object>> getCollegeStatistics(String startDate, String endDate, List<String> colleges) {
        return checkresultMapper.getCollegeStatistics(startDate, endDate, colleges);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CheckResult> findByCollege(List<String> colleges) {
        return checkresultMapper.findByCollege(colleges);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, Object>> getMonthlyAbsenceDetailByCollege(Integer year, String college,User user) {
        if (user.getNickName()=="管理员"){
            return checkresultMapper.getMonthlyAbsenceDetailByCollege(year, college);
        }
        return checkresultMapper.getMonthlyAbsenceDetailByCollege(year, user.getCollege());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, Object>> getAbsenceClassesByCollegeAndDateRange(String startDate, String endDate, String college,User user) {

        if (user.getNickName()=="管理员"){
            return checkresultMapper.getAbsenceClassesByCollegeAndDateRange(startDate, endDate, college);
        }
        return checkresultMapper.getAbsenceClassesByCollegeAndDateRange(startDate, endDate, user.getCollege());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllColleges() {
        return checkresultMapper.getAllColleges();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CheckResult> sortedByTime(String order) {
        if ("ascending".equals(order)) {
            return checkresultMapper.sortedByTime("asc");
        } else{
             return checkresultMapper.sortedByTime("desc");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CheckResult> sortedByCommuteTime(String commuteTime, String timeOrder) {
        return checkresultMapper.sortedByCommuteTime(commuteTime,timeOrder);

    }

}
