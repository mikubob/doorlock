package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.mapper.CourseScheduleMapper;
import com.hnkjzyxy.ab.model.CourseSchedule;
import com.hnkjzyxy.ab.service.CourseScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.hnkjzyxy.ab.utils.OaRequestAPIUtils.getClassBoardData;


/**
 * 课程安排Service实现类
 */
@Service
public class CourseScheduleServiceImpl extends ServiceImpl<CourseScheduleMapper, CourseSchedule> implements CourseScheduleService {

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;
    @Autowired
    private CourseScheduleService courseScheduleService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<CourseSchedule> getList(CourseSchedule courseSchedule) {
        // 或者更清晰的写法
        // 使用动态SQL查询
        return courseScheduleMapper.selectListByCondition(courseSchedule);
}

    @Override
    public void truncateTable() {
        courseScheduleMapper.truncateTable();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCourseSchedule(CourseSchedule courseSchedule) {
        return courseScheduleMapper.insert(courseSchedule) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCourseSchedule(CourseSchedule courseSchedule) {
        return courseScheduleMapper.updateById(courseSchedule) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Integer id) {
        return courseScheduleMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return courseScheduleMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public boolean refresh() {
        System.out.println("开始执行定时任务业务逻辑...");
        try {
            // 1. 使用 DDL 语句清空表（TRUNCATE TABLE）
            System.out.println("正在清空 course_schedule 表...");
            courseScheduleService.truncateTable();
            System.out.println("✓ 表数据已清空（使用 TRUNCATE TABLE）");

            // 2. 调用 OA 接口获取课程数据
            String dataJson = getClassBoardData("", "");

            if (dataJson == null || dataJson.isEmpty()) {
                System.out.println("未获取到课程数据");
                return false;
            }

            // 3. 解析 JSON 数组
            JsonNode jsonArray = objectMapper.readTree(dataJson);
            int count = 0;
            int successCount = 0;

            List<CourseSchedule> courseSchedules = new ArrayList<>();
            // 4. 遍历每一条课程数据
            for (JsonNode node : jsonArray) {
                try {
                    // 5. 转换为 CourseSchedule 对象
                    CourseSchedule courseSchedule = convertToCourseSchedule(node);

                    // 6. 添加到列表
                    if (courseSchedule != null) {
                        courseSchedules.add(courseSchedule);
                        count++;
                    }
                } catch (Exception e) {
                    System.err.println("✗ 转换失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 7. 批量保存，每100条提交一次
            int batchSize = 100;
            int totalBatches = (courseSchedules.size() + batchSize - 1) / batchSize;

            for (int i = 0; i < courseSchedules.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, courseSchedules.size());
                List<CourseSchedule> batch = courseSchedules.subList(i, endIndex);

                try {
                    boolean result = courseScheduleService.saveBatch(batch);
                    if (result) {
                        successCount += batch.size();
                        System.out.println("✓ 批次 " + (i / batchSize + 1) + "/" + totalBatches +
                                " 保存成功: " + batch.size() + " 条数据");
                    } else {
                        System.err.println("✗ 批次 " + (i / batchSize + 1) + " 保存失败");
                    }
                } catch (Exception e) {
                    System.err.println("✗ 批次 " + (i / batchSize + 1) + " 保存失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }


            System.out.println("========================================");
            System.out.println("定时任务执行完成！");
            System.out.println("总共处理: " + count + " 条数据");
            System.out.println("成功保存: " + successCount + " 条数据");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("定时任务执行失败: " + e.getMessage());
            e.printStackTrace();
            return false;// 返回失败
        }

        return true;
    }

    /**
     * 将 JsonNode 转换为 CourseSchedule 对象
     */
    private CourseSchedule convertToCourseSchedule(JsonNode node) {
        CourseSchedule course = new CourseSchedule();

        // 映射字段
        course.setCourseName(node.path("KCMC").asText(null));           // 课程名称
        course.setAcademicYear(node.path("KKXND").asText(null));        // 学年
        course.setSemester(node.path("KKXQM").asText(null));            // 学期
        course.setWeek(node.path("ZC").asText(null));                   // 周次
        course.setDayOfWeek(node.path("XQJ").asText(null));             // 星期几
        course.setClassPeriod(node.path("SKJC").asText(null));          // 上课节次
        course.setClassroomNumber(node.path("JSH").asText(null));       // 教室号
        course.setTeachingLocation(node.path("SKDD").asText(null));     // 上课地点
        course.setCampus(node.path("XQ").asText(null));                 // 校区
        course.setBuildingName(node.path("JZWMC").asText(null));        // 建筑物名称
        course.setTeacherId(node.path("JGH").asText(null));             // 教师工号
        course.setTeacherName(node.path("JSXM").asText(null));          // 教师姓名
        course.setDepartmentName(node.path("SZDWMC").asText(null));     // 所在单位
        course.setClassName(node.path("BJMC").asText(null));            // 班级名称
        course.setCounselorName(node.path("FDYXM").asText(null));       // 辅导员姓名


        // 数值类型字段
        if (node.has("JXBRS") && !node.path("JXBRS").isNull()) {
            course.setClassSize(node.path("JXBRS").asInt(0));
        }
        if (node.has("QJRS") && !node.path("QJRS").isNull()) {
            course.setLeaveCount(node.path("QJRS").asInt(0));
        }

        course.setHasLeave(node.path("SFYQJRS").asText("0"));           // 是否有请假
        course.setClassDate(node.path("SKRQ").asText(null));            // 上课日期

        // 验证必要字段
        if (course.getCourseName() == null || course.getClassName() == null ||
                course.getClassDate() == null) {
            System.err.println("跳过无效数据: 缺少必要字段");
            return null;
        }

        return course;
    }

}

