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

/** 旧任务模板预览入口，实际导入由 TaskExcelImportService 承担。 */
@Service
public class TaskExcelPreviewService {
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
