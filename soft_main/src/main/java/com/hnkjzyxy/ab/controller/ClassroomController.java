package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.model.Classroom;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教室信息管理
 * 提供教室信息的动态查询接口
 */
@RestController
@RequestMapping("/classroom")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    /**
     * 动态查询教室列表
     *
     * @param classroom 教室查询条件（可选参数：classroomNumber、classroomName、campusName、buildingName）
     * @return 教室列表
     */
    @PostMapping("/list")
    public ApiResult list(@RequestBody(required = false) Classroom classroom) {
        List<Classroom> list = classroomService.getClassroomList(classroom);
        return ApiResult.ok("data", list);
    }
}
