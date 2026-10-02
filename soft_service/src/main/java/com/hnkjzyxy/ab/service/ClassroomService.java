package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Classroom;

import java.util.List;

/**
 * 教室信息Service接口
 */
public interface ClassroomService extends IService<Classroom> {

    /**
     * 动态查询教室列表
     *
     * @param classroom 教室信息
     * @return 教室列表
     */
    List<Classroom> getClassroomList(Classroom  classroom);


}
