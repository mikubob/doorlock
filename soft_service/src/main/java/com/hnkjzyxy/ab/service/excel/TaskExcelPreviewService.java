package com.hnkjzyxy.ab.service.excel;

import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.utils.ExcelWorkbookUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 旧任务模板预览服务，校验并读取工作簿，当前保留返回空列表的行为
 */
@Service
public class TaskExcelPreviewService {
    /**
     * 校验并打开旧任务预览模板
     * <p>
     * 保留旧模板解析未启用的行为，不创建任务或写入数据库。
     * </p>
     *
     * @param file 待预览的任务 Excel 文件
     * @return 当前实现返回空任务列表，实际任务导入由 TaskExcelImportService 执行
     * @throws RuntimeException 文件校验或工作簿解析失败时抛出
     */
    public List<Task> preview(MultipartFile file) {
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
        ArrayList<Task> result = new ArrayList<>();
        try (InputStream input = file.getInputStream();
             Workbook workbook = ExcelWorkbookUtils.openWorkbook(input)) {
            // 旧模板解析原本返回空列表，实际任务导入由导入服务承担。
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return result;
        }
    }
}
