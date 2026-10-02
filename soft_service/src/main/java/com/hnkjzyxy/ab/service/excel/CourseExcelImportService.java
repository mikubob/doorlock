package com.hnkjzyxy.ab.service.excel;

import com.alibaba.excel.EasyExcel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.hnkjzyxy.ab.dto.excel.CourseModel;
import com.hnkjzyxy.ab.service.listener.CourseDataListener;
import com.hnkjzyxy.ab.service.CourseService;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;

/**
 * 课程模板 Excel 导入服务，读取工作表并交由课程监听器保存
 */
@Service
public class CourseExcelImportService {
    /**
     * 课程业务服务
     */
    @Autowired
    private CourseService courseService;
    /**
     * 雪花ID生成工具
     */
    @Autowired
    private SnowFlowUtils snowFlowUtils;

    /**
     * 读取课程 Excel 模板并导入课程
     *
     * @param file 课程 Excel 文件
     * @throws Exception 文件校验、工作簿读取或课程保存失败时抛出
     */
    public void readCourseExcel(MultipartFile file) throws Exception {
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
        EasyExcel.read(file.getInputStream(), CourseModel.class, new CourseDataListener(courseService, snowFlowUtils)).doReadAll();
    }
}
