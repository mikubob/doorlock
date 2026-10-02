package com.hnkjzyxy.ab.service.excel;

import com.alibaba.excel.EasyExcel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import com.hnkjzyxy.ab.dto.excel.StudentInfoModel;
import com.hnkjzyxy.ab.service.listener.StudentInfoDataListener;
import com.hnkjzyxy.ab.service.StudentInfoService;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;

/**
 * 学生录取信息 Excel 导入服务
 */
@Service
public class StudentInfoExcelImportService {
    /**
     * 学生录取信息业务服务
     */
    @Autowired
    private StudentInfoService studentInfoService;
    /**
     * 雪花ID生成工具
     */
    @Autowired
    private SnowFlowUtils snowFlowUtils;

    /**
     * 读取学生录取信息 Excel 文件并导入学生
     *
     * @param file 学生录取信息 Excel 文件
     * @throws IOException 读取上传文件输入流失败时抛出
     */
    public void readStudentInfoExcel(MultipartFile file) throws IOException {
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
        //读取第二个sheet页
        EasyExcel.read(file.getInputStream(), StudentInfoModel.class, new StudentInfoDataListener(studentInfoService, snowFlowUtils)).doReadAll();

    }
}
