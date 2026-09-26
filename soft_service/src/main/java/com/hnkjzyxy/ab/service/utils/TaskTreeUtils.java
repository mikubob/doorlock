package com.hnkjzyxy.ab.service.utils;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnkjzyxy.ab.mapper.FlowMapper;
import com.hnkjzyxy.ab.mapper.TaskMapper;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.Task;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class TaskTreeUtils {
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private FlowMapper flowMapper;

    public static List<Task> taskTree(List<Task> taskList, String parentId) {
        /*ArrayList<Task> tasks = new ArrayList<>();
        taskList.forEach(item->{
            if(item.getParentId().equals(parentId)){
                item.setChildren(taskTree(taskList,item.getId()));
                tasks.add(item);
            }
        });
        return tasks.size() > 0 ? tasks : null;*/
        return null;
    }

    public void buildProject(List<Project> projectList) {
        projectList.forEach(item -> {
            LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Task::getPId, item.getId());
            List<Task> tasks = taskMapper.selectList(wrapper);//获得该项目下的所有子任务
            List<Task> list = TaskTreeUtils.taskTree(tasks, "0");//构建子任务树 但是一开始都是0
            Integer flowId = flowMapper.getFLowByProjectId(item.getId());//获得该项目对应的流程id
            item.setFlowId(flowId);
            item.setTaskNum(getTaskNum(list));
        });
    }

    public Integer getTaskNum(List<Task> list) {
        Integer taskNum = 0;
        if (ObjectUtil.isNotNull(list) && list.size() > 0) {
            for (Task task : list) {
                //List<Task> children = task.getChildren();
                /*if(Objects.nonNull(children) && children.size() > 0){
                    taskNum += children.size();
                }*/
            }
        }
        return taskNum;
    }

}
