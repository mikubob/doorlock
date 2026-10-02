package com.hnkjzyxy.ab.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnkjzyxy.ab.mapper.FlowMapper;
import com.hnkjzyxy.ab.mapper.TaskMapper;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.service.utils.TaskTreeUtils;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;

/** 从数据库补充项目的流程信息和任务树统计。 */
@Component
public class ProjectTaskTreeSupport {
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private FlowMapper flowMapper;

    public void buildProject(List<Project> projectList) {
        projectList.forEach(item -> {
            LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Task::getPId, item.getId());
            List<Task> tasks = taskMapper.selectList(wrapper);//获得该项目下的所有子任务
            List<Task> list = TaskTreeUtils.taskTree(tasks, "0");//构建子任务树 但是一开始都是0
            Integer flowId = flowMapper.getFLowByProjectId(item.getId());//获得该项目对应的流程id
            item.setFlowId(flowId);
            item.setTaskNum(TaskTreeUtils.getTaskNum(list));
        });
    }
}
