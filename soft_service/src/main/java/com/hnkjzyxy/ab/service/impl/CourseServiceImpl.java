package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.CourseMapper;
import com.hnkjzyxy.ab.model.Course;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.CourseService;
import com.hnkjzyxy.ab.service.utils.ExcelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @projectName: assessment
 * @author: Lucas
 * @description: TODO
 * @date: 2024/4/23 20:25
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Autowired
    private CourseMapper courseMapper;


    @Autowired
    private TransactionTemplate transactionTemplate;

    @Resource
    private ExcelUtils excelUtils;


    @Override
    public List<Course> getList(Course dto, User user) {

        //按照星期和上课的班级来进行查询
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();

        if (dto != null) {

            wrapper.eq(dto.getWeek() != null, Course::getWeek, dto.getWeek());
            wrapper.eq(dto.getClasses() != null, Course::getClasses, dto.getClasses());
            wrapper.eq(dto.getCollege() != null, Course::getCollege, user.getCollege());
        }
        List<Course> courses = courseMapper.selectList(wrapper);
        return courses;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadCourseFile(MultipartFile file, User user) {
        //
        transactionTemplate.execute(status -> {
            try {

                excelUtils.readCourseExcel(file);

            } catch (Exception e) {
//                throw new RuntimeException("导入学期课表失败");
                throw new RuntimeException(e.getMessage());

            }
            return null;
        });
    }

    @Override
    public List<String> getClassesList( User directUser) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>();
        wrapper.select(Course::getClasses).orderByAsc(Course::getClasses);
        List<Course> courses = courseMapper.selectList(wrapper);
        List<Course> coursesbak=new ArrayList<>();
        for(Course item:courses){
            if (Objects.equals(item.getCollege(), directUser.getCollege())){
                coursesbak.add(item);
            }
        }
        //List<String> classesList = courses.stream().map(Course::getClasses).distinct().collect(Collectors.toList());
        List<String> classesList = coursesbak.stream().map(Course::getClasses).distinct().collect(Collectors.toList());
        return classesList;
    }

    @Override
    public List<String> getclassroomList() {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>();
        wrapper.select(Course::getClassroom).orderByAsc(Course::getClassroom);
        List<Course> courses = courseMapper.selectList(wrapper);
        List<String> classesList = courses.stream().map(Course::getClassroom).distinct().collect(Collectors.toList());
        return classesList;
    }

    @Override
    public List<String> getTeacherList() {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>();
        wrapper.select(Course::getTeacher).orderByAsc(Course::getTeacher);
        List<Course> courses = courseMapper.selectList(wrapper);
        List<String> classesList = courses.stream().map(Course::getTeacher).distinct().collect(Collectors.toList());
        return classesList;
    }

    @Override
    public List<String> getCounsellorList() {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>();
        wrapper.select(Course::getCounsellor).orderByAsc(Course::getCounsellor);
        List<Course> courses = courseMapper.selectList(wrapper);
        List<String> classesList = courses.stream().map(Course::getCounsellor).distinct().collect(Collectors.toList());
        return classesList;
    }

    @Override
    public void updateAllState() {
        //先删除原有数据
        courseMapper.updateAllState();
    }

    @Override
    public List<String> getColege() {
        return courseMapper.getColege();
    }

    @Override
    public List<String> getClassByCollege(String college) {
        List<String> classes =courseMapper.getClassByCollege(college);
        return classes;
    }

}
