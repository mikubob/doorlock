package com.hnkjzyxy.ab.service.utils;

import com.hnkjzyxy.ab.model.FlowTask;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程节点集合的递归筛选辅助工具
 */
public class FlowTaskUtils {

    /**
     * 按节点ID递归筛选流程节点，无匹配节点时返回 null
     *
     * @param pId 考核项目ID
     * @param list 待处理数据列表
     * @return 流程节点列表
     */
    public static List<FlowTask> flowTaskTree(Integer pId, List<FlowTask> list) {
        ArrayList<FlowTask> flowTasks = new ArrayList<>();
        list.forEach(item -> {
            if (item.getId().equals(pId)) {
                flowTaskTree(item.getId(), list);
                flowTasks.add(item);
            }
        });
        return flowTasks.size() > 0 ? flowTasks : null;
    }

}
