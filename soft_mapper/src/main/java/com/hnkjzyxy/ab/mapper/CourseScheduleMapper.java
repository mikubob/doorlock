package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 课程安排数据访问接口
 */
@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseSchedule> {

    /**
     * 动态条件查询课程列表
     *
     * @param courseSchedule 课表信息
     * @return 课表列表
     */
    List<CourseSchedule> selectListByCondition(CourseSchedule courseSchedule);

    /**
     * 清空课表
     * <p>
     * 使用 DELETE 而非 TRUNCATE TABLE：TRUNCATE 是 DDL，会隐式提交且无法回滚，
     * 一旦后续批量写入失败，课表将永久为空。DELETE 可参与外层事务，失败可整体回滚。
     * </p>
     *
     * @return 受影响行数
     */
    default int deleteAll() {
        return delete(new LambdaQueryWrapper<CourseSchedule>());
    }

    /**
     * 统计课表记录数
     *
     * @return 记录数
     */
    default int countAll() {
        return selectCount(new LambdaQueryWrapper<CourseSchedule>());
    }
}
