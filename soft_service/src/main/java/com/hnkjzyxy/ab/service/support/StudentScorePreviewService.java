package com.hnkjzyxy.ab.service.support;

import com.alibaba.fastjson.JSONArray;
import com.hnkjzyxy.ab.utils.ExcelWorkbookUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示用学生成绩预览服务，从工作簿读取学生并生成随机成绩
 */
@Service
public class StudentScorePreviewService {
    /**
     * 读取演示模板并生成学生成绩预览
     * <p>
     * 从每个工作表的第四行开始读取，使用随机成绩而非表格中的真实分数。
     * 文件流读取发生 I/O 异常时保留已收集的预览结果。
     * </p>
     *
     * @param file 学生成绩演示 Excel 文件
     * @return 包含姓名、学号、七项随机成绩和平均分的列表
     */
    public List<Map<String, String>> preview(MultipartFile file) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            for (List<String> row : ExcelWorkbookUtils.readRows(file, 3)) {
                Map<String, String> data = new HashMap<>();
                data.put("name", row.get(2));
                data.put("stuNo", row.get(1));
                double[] score = nextNum();
                data.put("score", JSONArray.toJSONString(score));
                data.put("avg", computeAvg(score) + "");
                result.add(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 生成七项演示成绩
     *
     * @return 七个保留两位小数的随机成绩，取值范围约为74至85
     */
    public double[] nextNum() {
        double[] a = new double[7];
        for (int i = 0; i < a.length; i++) {
            a[i] = Double.parseDouble(String.format("%.2f", 74 + (Math.random() * (85 - 74))));
        }
        return a;
    }

    /**
     * 计算演示成绩平均值
     *
     * @param score 待计算的非空成绩数组
     * @return 保留两位小数的算术平均值
     */
    public double computeAvg(double[] score) {
        double sum = 0.00;
        for (int i = 0; i < score.length; i++) {
            sum += score[i];
        }
        return Double.parseDouble(String.format("%.2f", sum / score.length));
    }
}
