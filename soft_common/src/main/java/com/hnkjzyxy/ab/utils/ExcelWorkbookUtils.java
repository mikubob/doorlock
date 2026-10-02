package com.hnkjzyxy.ab.utils;

import cn.hutool.core.util.ObjectUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/** 通用工作簿读取和单元格转换，不包含业务模型或数据生成规则。 */
public final class ExcelWorkbookUtils {
    private ExcelWorkbookUtils() { }

    public static Workbook openWorkbook(InputStream inputStream) {
        Workbook sheets = null;
        try {
            sheets = WorkbookFactory.create(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Excel工作溥错误！");
        }
        return sheets;
    }

    public static String cellToString(Cell cell) {
        if (ObjectUtil.isNull(cell)) {
            return null;
        }
        String returnValue = null;
        switch (cell.getCellType()) {
            case NUMERIC:  //数字
                double value = cell.getNumericCellValue();
                DecimalFormat format = new DecimalFormat("0");
                returnValue = format.format(value);
                break;
            case STRING: //字符串
                returnValue = cell.getStringCellValue();
                break;
            case BOOLEAN: //布尔值
                Boolean b = cell.getBooleanCellValue();
                returnValue = b.toString();
                break;
            case BLANK: //空值
                break;
            case FORMULA: //公式
                returnValue = cell.getCellFormula();
                break;
            case ERROR: //故障，错误
                break;
            default:
                break;
        }
        return returnValue;
    }

    /** 按工作表顺序读取指定起始行之后的非空行。 */
    public static List<List<String>> readRows(MultipartFile file, int startRow) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (InputStream input = file.getInputStream(); Workbook workbook = openWorkbook(input)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;
                    List<String> values = new ArrayList<>();
                    for (int column = 0; column < Math.max(3, row.getLastCellNum()); column++) {
                        values.add(cellToString(row.getCell(column)));
                    }
                    rows.add(values);
                }
            }
        }
        return rows;
    }
}
