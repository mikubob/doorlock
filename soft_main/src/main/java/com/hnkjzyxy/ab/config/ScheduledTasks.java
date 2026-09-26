package com.hnkjzyxy.ab.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.model.CourseSchedule;
import com.hnkjzyxy.ab.service.CourseScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.hnkjzyxy.ab.utils.OaRequestAPIUtils.getClassBoardData;

/**
 * 定时任务配置类
 */
@Component
public class ScheduledTasks {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CourseScheduleService courseScheduleService;
    




    /**
     * 定时任务 - 每天凌晨3点执行
     * Cron表达式: 秒 分 时 日 月 周
     * 0 0 3 * * ? 表示每天03:00:00执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void executeDailyAtNoon() {
        System.out.println("========================================");
        System.out.println("定时任务执行 - 每天凌晨3点");
        System.out.println("执行时间: " + LocalDateTime.now().format(formatter));
        System.out.println("========================================");
            
        // TODO: 在这里添加您需要执行的业务逻辑
        // 例如：同步课程数据、发送通知等
            
        try {
            // 示例：执行业务逻辑
            performTask();
        } catch (Exception e) {
            System.err.println("定时任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
        
    /**
     * 项目启动时执行一次
     * 使用 @PostConstruct 注解，在 Bean 初始化完成后立即执行
     */
    @PostConstruct
    public void executeOnStartup() {
        System.out.println("========================================");
        System.out.println("项目启动 - 执行初始化任务");
        System.out.println("执行时间: " + LocalDateTime.now().format(formatter));
        System.out.println("========================================");
            
        try {
            // 延迟5秒执行，确保所有服务都已启动
            Thread.sleep(5000);
            performTask();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("启动任务被中断: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("启动任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 业务逻辑方法 - 将OA接口数据转换为CourseSchedule并保存
     */
    private void performTask() {
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
                return;
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
        }
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

    /**
     * 测试方法 - 每10秒执行一次（用于测试）
     * 如果需要测试，可以取消注释下面的代码
     */
    /*
    @Scheduled(fixedRate = 10000)
    public void testTask() {
        System.out.println("测试任务执行时间: " + LocalDateTime.now().format(formatter));
    }
    */
}
