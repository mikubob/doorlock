package com.hnkjzyxy.ab.service.excel;

import com.alibaba.excel.EasyExcel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Locale;
import org.springframework.transaction.support.TransactionTemplate;
import com.hnkjzyxy.ab.dto.excel.CheckResultModel;
import com.hnkjzyxy.ab.service.listener.CheckResultDataListener;
import com.hnkjzyxy.ab.service.CheckResultService;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;
import com.hnkjzyxy.ab.config.CheckResultImportProperties;
import com.hnkjzyxy.ab.vo.CheckResultImportResult;

/**
 * 巡查结果 Excel 导入服务，校验文件并返回导入回执
 */
@Service
public class CheckResultExcelImportService {
    /**
     * 教学巡查结果业务服务
     */
    @Autowired
    private CheckResultService checkResultService;
    /**
     * 雪花ID生成工具
     */
    @Autowired
    private SnowFlowUtils snowFlowUtils;
    /**
     * 编程式事务模板
     */
    @Autowired
    private TransactionTemplate transactionTemplate;
    /**
     * 巡查 Excel 导入规则配置
     */
    @Autowired
    private CheckResultImportProperties checkResultImportProperties;

    /**
     * 校验巡查 Excel 文件并返回导入回执
     * <p>
     * 读取全部工作表，使用配置的巡查导入规则和事务模板处理数据。
     * </p>
     *
     * @param file 巡查 Excel 文件
     * @param sourceCollege 此次导入的数据来源学院
     * @return 包含导入数量和行级处理结果的回执
     * @throws Exception 文件校验、解析或事务保存失败时抛出
     */
    public CheckResultImportResult readScheduleExcel(MultipartFile file, String sourceCollege) throws Exception {
        String filename = file.getOriginalFilename();
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空！");
        }
        if (filename == null || (!filename.toLowerCase(Locale.ROOT).endsWith(".xls")
                && !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx"))) {
            throw new RuntimeException("上传文件的类型必须是xls或者xlsx!");
        }
        if (file.getSize() > 100L * 1024 * 1024) {
            throw new RuntimeException("上传的文件大小不能超过100MB!");
        }
        CheckResultDataListener listener = new CheckResultDataListener(checkResultService, snowFlowUtils,
                transactionTemplate, checkResultImportProperties, sourceCollege);
        EasyExcel.read(file.getInputStream(), CheckResultModel.class, listener)
                .headRowNumber(1)
                .doReadAll();
        return listener.finishAndSave();
    }
}
