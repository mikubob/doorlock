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

/**
 * 通用 Excel 工作簿读取和单元格转换工具
 */
public final class ExcelWorkbookUtils {
    /**
     * 工具类构造方法，禁止直接实例化
     */
    private ExcelWorkbookUtils() { }

    /**
     * 从输入流创建 Excel 工作簿
     *
     * @param inputStream 包含 Excel 文件内容的输入流
     * @return 读取的工作簿，调用方使用后负责关闭
     * @throws RuntimeException 工作簿读取发生 I/O 异常时抛出
     */
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

    /**
     * 按单元格类型转换字符串
     * <p>
     * 数值按整数格式转换，布尔值转为文本，公式返回公式表达式而非计算结果。
     * </p>
     *
     * @param cell 待转换单元格，可为 null
     * @return 转换后的文本；单元格不存在、为空或错误类型时返回 null
     */
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

    /**
     * 按工作表顺序读取起始行及其后的非空行
     * <p>
     * 跳过不存在的行，保留单元格位置；单元格缺失或为空时对应值为 null。
     * 本方法负责关闭输入流及工作簿。
     * </p>
     *
     * @param file 待读取的 Excel 文件
     * @param startRow 起始行索引，从零开始，包含该行
     * @return 按工作表及行顺序排列的单元格字符串列表，每行至少包含三列
     * @throws IOException 读取上传文件流失败时抛出
     */
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
