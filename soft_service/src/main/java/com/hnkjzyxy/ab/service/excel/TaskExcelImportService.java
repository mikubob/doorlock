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

/**
 * 任务模板 Excel 导入服务，校验项目可变更状态后导入任务
 */
@Service
public class TaskExcelImportService {
    /**
     * 项目任务生命周期及并发保护服务
     */
    @Autowired
    private ProjectTaskGuard projectTaskGuard;
    /**
     * TaskService业务服务
     */
    @Autowired
    private TaskService taskService;
    /**
     * 雪花ID生成工具
     */
    @Autowired
    private SnowFlowUtils snowFlowUtils;

    /**
     * 校验项目状态并导入任务 Excel 模板
     * <p>
     * 在 READ_COMMITTED 事务中取得项目锁并校验任务是否允许变更。
     * 通过任务监听器读取全部工作表，发生异常时回滚当前导入事务。
     * </p>
     *
     * @param file 任务 Excel 文件，扩展名为 xls 或 xlsx，大小不超过现有校验上限100MB
     * @param projectId 待导入任务所属项目ID
     * @throws Exception 项目状态不允许变更、文件校验失败或读取保存失败时抛出
     */
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
