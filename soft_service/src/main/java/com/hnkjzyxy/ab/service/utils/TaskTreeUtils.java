package com.hnkjzyxy.ab.service.utils;

import cn.hutool.core.util.ObjectUtil;
import com.hnkjzyxy.ab.model.Task;
import java.util.List;

/**
 * 任务树辅助工具，保留当前未启用的构建及计数行为
 */
public final class TaskTreeUtils {
    /**
     * 工具类构造方法，禁止直接实例化
     */
    private TaskTreeUtils() { }

    /**
     * 任务树构建入口，保留当前未启用的实现
     * <p>
     * 原递归构建逻辑处于注释中，本方法当前不构建或修改任务树。
     * </p>
     *
     * @param taskList 原始任务列表
     * @param parentId 根父任务ID
     * @return 当前实现固定返回 null
     */
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

    /**
     * 任务树计数入口，保留当前未启用的子任务统计
     *
     * @param list 任务树列表，可为 null
     * @return 当前实现固定返回零
     */
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
