package com.hnkjzyxy.ab.service.listener;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.service.TaskService;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-04 21:16
 */
public class TaskDataListener extends AnalysisEventListener<TaskModel> {

    private static final int BATCH_COUNT = 100;
    /**
     * 记录解析的数据总数
     */
    int count = 0;
    private TaskService taskService;
    private Integer projectId;
    private SnowFlowUtils snowFlowUtils;

    /**
     * 用于接收解析的所有数据
     */
    private List<TaskModel> data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    public TaskDataListener(TaskService taskService, Integer projectId, SnowFlowUtils snowFlowUtils) {
        this.taskService = taskService;
        this.projectId = projectId;
        this.snowFlowUtils = snowFlowUtils;
    }

    /**
     * 提供一个对外访问的方法
     *
     * @return 已解析的数据列表
     */
    public List<TaskModel> getData() {
        return data;
    }

    /**
     * 每解析一行调用一次
     *
     * @param objects         当前行解析结果
     * @param analysisContext 解析上下文
     */
    @Override
    public void invoke(TaskModel objects, AnalysisContext analysisContext) {
        //log.info("解析到一条数据:{}", JSON.toJSONString(data));
        data.add(objects);
        count++;
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (data.size() >= BATCH_COUNT) {
            saveData();
        }
    }

    /**
     * 所有数据解析完毕后执行的操作
     *
     * @param analysisContext 解析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        saveData();
        System.out.println("解析完毕，共" + (count - 1) + "条数据");
    }

    public void saveData() {
        List<Task> tasks = data.stream().map(item -> {
            Task task = new Task();
            task.setId(String.valueOf(snowFlowUtils.nextId()));
            task.setPId(projectId);
            if (ObjectUtil.isNull(item.getCategory())) {
                throw new RuntimeException("项目类别不能为空！");
            }
            task.setCategory(item.getCategory());
            if (ObjectUtil.isNull(item.getTaskName())) {
                throw new RuntimeException("项目子任务名称不能为空！");
            }
            task.setTaskName(item.getTaskName());
            if (ObjectUtil.isNull(item.getScore())) {
                throw new RuntimeException("项目评分满分不能为空！");
            }
            task.setScore(item.getScore());
            if (ObjectUtil.isNull(item.getStandard())) {
                throw new RuntimeException("项目评分标准不能为空！");
            }
            task.setStandard(item.getStandard());
            if (ObjectUtil.isNull(item.getIsFile())) {
                throw new RuntimeException("项目是否需要上传文件不能为空！");
            }
            task.setIsFile(item.getIsFile());
            if (ObjectUtil.isNull(item.getIsFile())) {
                throw new RuntimeException("项目是否需要扩展不能为空！");
            }
            task.setIsExtend(item.getIsExtend());
            task.setRemark(item.getRemark());
            return task;
        }).collect(Collectors.toList());
        if (!tasks.isEmpty() && !taskService.saveBatch(tasks)) {
            throw new IllegalStateException("Excel任务保存失败，项目任务导入已回滚");
        }
        // 存储完成清理 list
        data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    }
}
