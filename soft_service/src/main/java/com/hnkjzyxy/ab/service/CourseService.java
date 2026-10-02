package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Course;
import com.hnkjzyxy.ab.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程Service接口
 */
public interface CourseService extends IService<Course> {
    /**
     * 查询课程信息
     *
     * @param resultVo 课程数据
     * @param user 当前用户
     * @return 课程列表
     */
    List<Course> getList(Course resultVo, User user);

    /**
     * 导入课程 Excel 文件
     *
     * @param file 待处理文件
     * @param user 当前用户
     */
    void uploadCourseFile(MultipartFile file, User user);

    /**
     * 查询课程涉及的班级名称
     *
     * @param directUser 用户信息
     * @return 查询结果列表
     */
    List<String> getClassesList( User directUser);

    /**
     * 查询课程涉及的教室名称
     *
     * @return 查询结果列表
     */
    List<String> getclassroomList();

    /**
     * 查询课程教师名称
     *
     * @return 查询结果列表
     */
    List<String> getTeacherList();

    /**
     * 查询辅导员名称
     *
     * @return 查询结果列表
     */
    List<String> getCounsellorList();

    /**
     * 逻辑删除 数据库所有内容
     */
    void updateAllState();

    /**
     * 查询学院名称
     *
     * @return 查询结果列表
     */
    List<String> getColege();

    /**
     * 按学院查询班级名称
     *
     * @param college 学院名称
     * @return 查询结果列表
     */
    List<String> getClassByCollege(String college);
}
