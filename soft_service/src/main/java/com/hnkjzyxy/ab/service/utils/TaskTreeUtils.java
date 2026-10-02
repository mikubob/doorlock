package com.hnkjzyxy.ab.service.utils;

import cn.hutool.core.util.ObjectUtil;
import com.hnkjzyxy.ab.model.Task;
import java.util.List;

/** 任务树的纯内存辅助方法，保留现有构建和计数行为。 */
public final class TaskTreeUtils {
    private TaskTreeUtils() { }

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

    public static Integer getTaskNum(List<Task> list) {
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
