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

/**
 * 项目任务树信息补充组件，查询项目的流程及任务统计信息
 */
@Component
public class ProjectTaskTreeSupport {
    /**
     * TaskMapper数据访问接口
     */
    @Resource
    private TaskMapper taskMapper;
    /**
     * 审批流程数据访问接口
     */
    @Resource
    private FlowMapper flowMapper;

    /**
     * 补充项目流程ID及任务数量
     * <p>
     * 分别查询项目任务和关联流程，任务数量沿用 TaskTreeUtils 当前的计数规则。
     * </p>
     *
     * @param projectList 待补充信息的项目列表，原对象会被更新
     */
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
