package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Course;
import com.hnkjzyxy.ab.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseService extends IService<Course> {
    List<Course> getList(Course resultVo, User user);

    void uploadCourseFile(MultipartFile file, User user);

    List<String> getClassesList( User directUser);

    List<String> getclassroomList();

    List<String> getTeacherList();

    List<String> getCounsellorList();

    /**
     * 逻辑删除 数据库所有内容
     */
    void updateAllState();

    List<String> getColege();

    List<String> getClassByCollege(String college);
}
