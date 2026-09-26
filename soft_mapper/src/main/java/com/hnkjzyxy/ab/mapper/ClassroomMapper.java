package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Classroom;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 教室信息表数据库访问层
 */
@Mapper
public interface ClassroomMapper extends BaseMapper<Classroom> {

    /**
     * 动态查询教室列表
     *
     * @param classroom 教室查询条件
     * @return 教室列表
     */
    List<Classroom> getClassroomList(Classroom classroom);
}
