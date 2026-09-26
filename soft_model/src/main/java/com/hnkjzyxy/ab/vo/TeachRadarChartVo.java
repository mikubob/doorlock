package com.hnkjzyxy.ab.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 教研室维度雷达图数据
 * 外层按教研室分组，内层存储各维度对应的考核数值
 */
@Data
public class TeachRadarChartVo {

    /**
     * 雷达图数值：教研室 -> （维度 -> 数值）
     */
    List<Map<String, Map<String, Integer>>> teachRadarValue;
}
