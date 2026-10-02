package com.hnkjzyxy.ab.service.listener;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.hnkjzyxy.ab.dto.excel.CourseModel;
import com.hnkjzyxy.ab.model.Course;
import com.hnkjzyxy.ab.service.CourseService;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-04 21:16
 */
public class CourseDataListener extends AnalysisEventListener<CourseModel> {

    private static final int BATCH_COUNT = 100;
    /**
     * 记录解析的数据总数
     */
    int count = 0;
    private CourseService courseService;
    private SnowFlowUtils snowFlowUtils;

    /**
     * 用于接收解析的所有数据
     */
    private List<CourseModel> data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    public CourseDataListener(CourseService courseService, SnowFlowUtils snowFlowUtils) {
        this.courseService = courseService;
        this.snowFlowUtils = snowFlowUtils;
    }

    /**
     * 提供一个对外访问的方法
     *
     * @return 已解析的数据列表
     */
    public List<CourseModel> getData() {
        return data;
    }

    /**
     * 每解析一行调用一次
     *
     * @param objects         当前行解析结果
     * @param analysisContext 解析上下文
     */
    @Override
    public void invoke(CourseModel objects, AnalysisContext analysisContext) {
        //log.info("解析到一条数据:{}", JSON.toJSONString(data));
        data.add(objects);
        count++;
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (data.size() >= BATCH_COUNT) {
            saveData();
        }
    }

    /**
     * 所有数据解析完毕后执行的操作
     *
     * @param analysisContext 解析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        saveData();
        System.out.println("解析完毕，共" + (count - 1) + "条数据");
    }

    public void saveData() {
        List<Course> courses = data.stream().map(item -> {
            Course course = new Course();
            course.setId(String.valueOf(snowFlowUtils.nextId()));

            if (ObjectUtil.isNull(item.getWeek())) {
                throw new RuntimeException("星期不能为空！");
            }

            if (ObjectUtil.isNull(item.getWeeks())) {
                throw new RuntimeException("周次不能为空！");
            }

            if (ObjectUtil.isNull(item.getSection())) {
                throw new RuntimeException("节次不能为空！");
            }

            if (ObjectUtil.isNull(item.getClassroom())) {
                throw new RuntimeException("教室名不能为空！");
            }

            if (ObjectUtil.isNull(item.getClasses())) {
                throw new RuntimeException("上课班级不能为空！");
            }

            if (ObjectUtil.isNull(item.getShould_arrival())) {
                throw new RuntimeException("应到人数不能为空！");
            }
            if (ObjectUtil.isNull(item.getCourse())) {
                throw new RuntimeException("课程名称不能为空！");
            }
            if (ObjectUtil.isNull(item.getCounsellor())) {
                throw new RuntimeException("辅导员不能为空！");
            }
            if (ObjectUtil.isNull(item.getTeacher())) {
                throw new RuntimeException("任课老师不能为空！");
            }
            if (ObjectUtil.isNull(item.getCollege())) {
                throw new RuntimeException("学院不能为空！");
            }
            String arrival = item.getShould_arrival();
            BeanUtils.copyProperties(item, course);

            course.setShouldArrival(Integer.parseInt(arrival));

            course.setState(1);//表示这是新的课表
            return course;
        }).collect(Collectors.toList());
        courseService.saveBatch(courses);
        // 存储完成清理 list
        data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    }
}
