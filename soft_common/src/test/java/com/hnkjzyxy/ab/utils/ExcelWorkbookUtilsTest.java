package com.hnkjzyxy.ab.utils;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayOutputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作簿读取测试，验证稀疏行及单元格转换
 */
class ExcelWorkbookUtilsTest {
    /**
     * 验证稀疏行读取以及原有单元格转换规则
     *
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Test
    void readsSparseRowsAndPreservesOriginalCellConversions() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row row = workbook.createSheet("学生").createRow(3);
            row.createCell(1).setCellValue(1001);
            row.createCell(2).setCellValue("张同学");
            row.createCell(3).setCellValue(true);
            row.createCell(4).setCellFormula("1+2");
            workbook.write(bytes);
        }
        MockMultipartFile file = new MockMultipartFile("file", "students.xlsx",
                "application/octet-stream", bytes.toByteArray());
        List<List<String>> rows = ExcelWorkbookUtils.readRows(file, 3);
        assertEquals(1, rows.size());
        assertNull(rows.get(0).get(0));
        assertEquals("1001", rows.get(0).get(1));
        assertEquals("张同学", rows.get(0).get(2));
        assertEquals("true", rows.get(0).get(3));
        assertEquals("1+2", rows.get(0).get(4));
        assertNull(ExcelWorkbookUtils.cellToString(null));
    }
}
