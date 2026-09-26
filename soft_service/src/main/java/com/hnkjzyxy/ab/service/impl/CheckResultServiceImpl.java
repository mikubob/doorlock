package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.CheckResultMapper;
import com.hnkjzyxy.ab.model.CheckResult;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.CheckResultService;
import com.hnkjzyxy.ab.service.utils.ExcelUtils;
import com.hnkjzyxy.ab.vo.CheckResultByTeacherDataVo;
import com.hnkjzyxy.ab.vo.CheckResultDataVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @projectName: assessment
 * @author: Lucas
 * @description: TODO
 * @date: 2024/4/23 20:25
 */
@Service
public class CheckResultServiceImpl extends ServiceImpl<CheckResultMapper, CheckResult> implements CheckResultService {

    @Autowired
    private CheckResultMapper checkresultMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Resource
    private ExcelUtils excelUtils;

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

    @Override
    @Transactional
    public void addOrEdit(CheckResult resultVo) {
        if (resultVo.getId() == null) {
            checkresultMapper.insert(resultVo);
        } else {
            checkresultMapper.updateById(resultVo);
        }
    }

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

    @Override
    public List<CheckResult> dataStatisticsByClasses(CheckResult resultVo, User user) {

        List<CheckResult> checkResults = checkresultMapper.selectListByClasses(resultVo.getClasses(), resultVo.getStartTime(), resultVo.getEndTime(), user.getCollege());
        return checkResults;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadCheckResult(MultipartFile file) {

        transactionTemplate.execute(status -> {
            try {
                //先删除原有数据
                excelUtils.readScheduleExcel(file);
            } catch (Exception e) {
//                throw new RuntimeException("导入学期课表失败");
                throw new RuntimeException(e.getMessage());

            }
            return null;
        });
    }

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

    @Override
    public List<CheckResult> dataStatisticsByTeacher(CheckResult resultVo) {

        //通过任课老师来进行查询

        return checkresultMapper.selectListByTeacher(resultVo.getTeacher(), resultVo.getStartTime(), resultVo.getEndTime());

    }


    @Override
    public List<CheckResult> findByDateRangeAndCollege(String startDate, String endDate, String college, User user) {
        if (user.getNickName()=="管理员"){
            return checkresultMapper.findByDateRangeAndCollege(startDate, endDate, college );
        }
        return checkresultMapper.findByDateRangeAndCollege(startDate, endDate, user.getCollege() );

    }

    @Override
    public List<Map<String, Object>> getCollegeStatistics(String startDate, String endDate, List<String> colleges) {
        return checkresultMapper.getCollegeStatistics(startDate, endDate, colleges);

    }

    @Override
    public List<CheckResult> findByCollege(List<String> colleges) {
        return checkresultMapper.findByCollege(colleges);
    }

    @Override
    public List<Map<String, Object>> getMonthlyAbsenceDetailByCollege(Integer year, String college,User user) {
        if (user.getNickName()=="管理员"){
            return checkresultMapper.getMonthlyAbsenceDetailByCollege(year, college);
        }
        return checkresultMapper.getMonthlyAbsenceDetailByCollege(year, user.getCollege());
    }

    @Override
    public List<Map<String, Object>> getAbsenceClassesByCollegeAndDateRange(String startDate, String endDate, String college,User user) {

        if (user.getNickName()=="管理员"){
            return checkresultMapper.getAbsenceClassesByCollegeAndDateRange(startDate, endDate, college);
        }
        return checkresultMapper.getAbsenceClassesByCollegeAndDateRange(startDate, endDate, user.getCollege());
    }


    @Override
    public List<String> getAllColleges() {
        return checkresultMapper.getAllColleges();
    }

    @Override
    public List<CheckResult> sortedByTime(String order) {
        if ("ascending".equals(order)) {
            return checkresultMapper.sortedByTime("asc");
        } else{
             return checkresultMapper.sortedByTime("desc");
        }
    }

    @Override
    public List<CheckResult> sortedByCommuteTime(String commuteTime, String timeOrder) {
        return checkresultMapper.sortedByCommuteTime(commuteTime,timeOrder);

    }


}
