package com.hnkjzyxy.ab.service.listener;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.hnkjzyxy.ab.model.StudentInfo;
import com.hnkjzyxy.ab.service.StudentInfoService;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

public class StudentInfoDataListener extends AnalysisEventListener<StudentInfoModel> {
    private static final int BATCH_COUNT = 100;
    //记录解析的数据总数
    int count = 0;
    private StudentInfoService studentInfoService;
    private SnowFlowUtils snowFlowUtils;
    private List<StudentInfoModel> data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    public StudentInfoDataListener(StudentInfoService studentInfoService, SnowFlowUtils snowFlowUtils) {
        this.studentInfoService = studentInfoService;
        this.snowFlowUtils = snowFlowUtils;
    }

    /**
     * 提供一个对外访问的方法
     *
     * @return 已解析的数据列表
     */
    public List<StudentInfoModel> getData() {
        return data;
    }

    /**
     * 每解析一行调用一次
     *
     * @param objects         当前行解析结果
     * @param analysisContext 解析上下文
     */
    @Override
    public void invoke(StudentInfoModel objects, AnalysisContext analysisContext) {
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
        List<StudentInfo> courses = data.stream().map(item -> {
            StudentInfo studentInfo = new StudentInfo();
            studentInfo.setId(Long.valueOf(snowFlowUtils.nextId()));

            if (ObjectUtil.isNull(item.getName())) {
                throw new RuntimeException("姓名不能为空！");
            }

            if (ObjectUtil.isNull(item.getStudentId())) {
                throw new RuntimeException("学号不能为空！");
            }

            if (ObjectUtil.isNull(item.getClassName())) {
                throw new RuntimeException("网页设计与制作成绩不能为空！");
            }

            if (ObjectUtil.isNull(item.getWebScore())) {
                throw new RuntimeException("网页设计与制作成绩不能为空！");
            }

            if (ObjectUtil.isNull(item.getJavaScore())) {
                throw new RuntimeException("面向对象程序设计（Java）成绩不能为空！");
            }

            if (ObjectUtil.isNull(item.getProgramScore())) {
                throw new RuntimeException("程序设计基础成绩不能为空！");
            }
            if (ObjectUtil.isNull(item.getDatabaseScore())) {
                throw new RuntimeException("数据库应用技术成绩不能为空！");
            }
            studentInfo.setStatus(0);
            studentInfo.setDelFlag(1);
            BeanUtils.copyProperties(item, studentInfo);
            return studentInfo;
        }).collect(Collectors.toList());
        studentInfoService.saveBatch(courses);
        // 存储完成清理 list
        data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    }
}
