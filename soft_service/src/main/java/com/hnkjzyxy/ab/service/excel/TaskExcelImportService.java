package com.hnkjzyxy.ab.service.excel;

import com.alibaba.excel.EasyExcel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import com.hnkjzyxy.ab.dto.excel.TaskModel;
import com.hnkjzyxy.ab.service.listener.TaskDataListener;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.TaskService;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;

/** 负责对应业务模板的 Excel 导入，保留原解析和事务规则。 */
@Service
public class TaskExcelImportService {
    @Autowired
    private ProjectTaskGuard projectTaskGuard;
    @Autowired
    private TaskService taskService;
    @Autowired
    private SnowFlowUtils snowFlowUtils;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void readTaskExcel(MultipartFile file, Integer projectId) throws Exception {
        projectTaskGuard.mutable(projectTaskGuard.lock(projectId));
        String filename = file.getOriginalFilename();
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空！");
        }
        if (!filename.endsWith("xls") && !filename.endsWith("xlsx")) {
            throw new RuntimeException("上传文件的类型必须是xls或者xlsx!");
        }
        long size = file.getSize();
        double length = size / 1048576;
        if (length > 100) {
            throw new RuntimeException("上传的文件大小不能超过100MB!");
        }
        EasyExcel.read(file.getInputStream(), TaskModel.class, new TaskDataListener(taskService, projectId, snowFlowUtils)).doReadAll();
    }
}
