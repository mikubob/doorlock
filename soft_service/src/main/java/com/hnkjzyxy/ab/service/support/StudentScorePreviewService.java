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

/** 旧演示接口的学生成绩预览，随机成绩规则集中在这里。 */
@Service
public class StudentScorePreviewService {
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
