package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.ClassroomMapper;
import com.hnkjzyxy.ab.model.Classroom;
import com.hnkjzyxy.ab.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教室信息Service实现类
 */
@Service
public class ClassroomServiceImpl extends ServiceImpl<ClassroomMapper, Classroom> implements ClassroomService {

    /**
     * 教室数据访问接口
     */
    @Autowired
    private ClassroomMapper classroomMapper;


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Classroom> getClassroomList(Classroom classroom) {
        return classroomMapper.getClassroomList(classroom);
    }
}
