package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.TaskMapper;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.service.TaskService;
import org.springframework.stereotype.Service;

/**
 * TaskServiceImplService实现类
 *
 * @author 16702
 */
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

}
