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

/** 负责对应业务模板的 Excel 导入，保留原解析和事务规则。 */
@Service
public class CheckResultExcelImportService {
    @Autowired
    private CheckResultService checkResultService;
    @Autowired
    private SnowFlowUtils snowFlowUtils;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private CheckResultImportProperties checkResultImportProperties;

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
