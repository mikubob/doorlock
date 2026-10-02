package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程数据访问接口
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 重置课程状态
     */
    default void updateAllState() {
        update(null, new LambdaUpdateWrapper<Course>().set(Course::getState, 0));
    }

    /**
     * 查询学院名称
     *
     * @return 去重后的学院名称列表
     */
    List<String> getColege();

    /**
     * 按学院查询班级名称
     *
     * @param college 学院名称
     * @return 指定学院去重后的班级名称列表
     */
    List<String> getClassByCollege(@Param("college") String college);
}
