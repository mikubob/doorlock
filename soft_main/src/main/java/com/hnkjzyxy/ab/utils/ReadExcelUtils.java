package com.hnkjzyxy.ab.utils;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.hnkjzyxy.ab.model.Task;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

@Component
public class ReadExcelUtils {

    @Resource
    private SnowFlowUtils snowFlowUtils;

    /**
     * 根据文件类型获取对应的工作簿
     *
     * @param inputStream 文件输入流
     * @return 工作簿对象
     */
    private Workbook getWorkbook(InputStream inputStream) {
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
     * 将单元格内容转换为字符串
     *
     * @param cell 单元格
     * @return 单元格内容的字符串形式
     */
    private String convertToString(Cell cell) {
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
     * 处理Excel内容转换为List<Map<String,Object>>
     */
    private List<Task> handleData(Workbook workbook) {
        //结果返回集
        List<Task> result = new ArrayList<>();
        //解析sheet
        /*for (int num = 0; num < workbook.getNumberOfSheets(); num++) {
            Sheet sheetAt = workbook.getSheetAt(num);
            if (ObjectUtil.isNull(sheetAt)) {
                continue;
            }
            //获取结束行数
            int endRowNum = sheetAt.getLastRowNum();
            //获取表头行
            Row titleRow = sheetAt.getRow(0);
            //遍历行数
            for (int i = 1; i <= endRowNum; i++) {
                Row row = sheetAt.getRow(i);
                if (ObjectUtil.isNull(row)) {
                    continue;
                }
                try{
                    Task taskParent = new Task();
                    String uuid = snowFlowUtils.nextId() + "";
                    taskParent.setId(uuid);
                    taskParent.setParentId("0");
                    taskParent.setTitle(convertToString(row.getCell(1)));
                    taskParent.setIsUpload(Integer.valueOf(convertToString(row.getCell(2))));
                    taskParent.setRemark(convertToString(row.getCell(3)));
                    taskParent.setContent(convertToString(row.getCell(4)));
                    ArrayList<Task> taskList = new ArrayList<>();
                    for (int j = 4; j < row.getLastCellNum(); j++) {
                        String uuids = snowFlowUtils.nextId() + "";
                        String s = convertToString(row.getCell(j));
                        Task taskChildren = new Task();
                        taskChildren.setTitle(convertToString(titleRow.getCell(j)));
                        taskChildren.setIsUpload(Integer.valueOf(convertToString(row.getCell(2))));
                        taskChildren.setId(uuids);
                        taskChildren.setParentId(uuid);
                        taskChildren.setContent(s);
                        taskChildren.setParentId(uuid);
                        taskList.add(taskChildren);
                    }
                    taskParent.setChildren(taskList);
                    result.add(taskParent);
                }catch(Exception e){
                    e.printStackTrace();
                    throw new RuntimeException("Excel文件内容格式错误！");
                }
            }
        }*/
        return result;
    }

    public List<Task> readExcels(MultipartFile file) {
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
        ArrayList<Task> tasks = new ArrayList<>();
        return tasks;
    }

    /**
     * 读取 Excel 表格内容
     *
     * @param file 上传的 Excel 文件
     * @return 任务列表
     */
    public List<Task> readExcel(MultipartFile file) {
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
        //声明返回结果集
        ArrayList<Task> result = new ArrayList<>();
        //声明一个工作溥
        Workbook workbook = null;
        //声明一个文件输入流
        InputStream inputStream = null;
        //获取指定的excel工作溥
        try {
            inputStream = file.getInputStream();
            workbook = getWorkbook(inputStream);
            result = (ArrayList<Task>) handleData(workbook);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (workbook != null) {
                    workbook.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return result;
    }

    public List<Map<String, String>> generalExcel(MultipartFile file) {
        //声明返回结果集
        List<Map<String, String>> result = new ArrayList<>();
        //声明一个工作溥
        Workbook workbook = null;
        //声明一个文件输入流
        InputStream inputStream = null;
        //获取指定的excel工作溥
        try {
            inputStream = file.getInputStream();
            workbook = getWorkbook(inputStream);
            result = handlerJson(workbook);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (workbook != null) {
                    workbook.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return result;
    }

    public List<Map<String, String>> handlerJson(Workbook workbook) {
        List<Map<String, String>> result = new ArrayList<>();
        //解析sheet
        for (int num = 0; num < workbook.getNumberOfSheets(); num++) {
            Sheet sheetAt = workbook.getSheetAt(num);
            if (sheetAt == null) {
                continue;
            }
            //获取结束行数
            int endRowNum = sheetAt.getLastRowNum();
            //获取表头行
            Row titleRow = sheetAt.getRow(0);
            //遍历行数
            for (int i = 3; i <= endRowNum; i++) {
                Row row = sheetAt.getRow(i);
                if (row == null) {
                    continue;
                }
                HashMap<String, String> map = new HashMap<>();
                map.put("name", convertToString(row.getCell(2)));
                map.put("stuNo", convertToString(row.getCell(1)));
//                map.put("sex",convertToString(row.getCell(3)));
//                map.put("brith",convertToString(row.getCell(4)));
                double[] score = nextNum();
                map.put("score", JSONArray.toJSONString(score));
                map.put("avg", computeAvg(score) + "");
                result.add(map);
            }
        }
        return result;
    }

    public double[] nextNum() {
        double[] a = new double[7];
        for (int i = 0; i < a.length; i++) {
            a[i] = Double.parseDouble(String.format("%.2f", 74 + (Math.random() * (85 - 74))));
        }
        return a;
    }

    public double computeAvg(double[] score) {
        double sum = 0.00;
        for (int i = 0; i < score.length; i++) {
            sum += score[i];
        }
        return Double.parseDouble(String.format("%.2f", sum / score.length));
    }


}
