package com.hnkjzyxy.ab.service.utils;

import com.hnkjzyxy.ab.model.FlowTask;

import java.util.ArrayList;
import java.util.List;

public class FlowTaskUtils {

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
