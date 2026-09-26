package com.hnkjzyxy.ab.params;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 项目雷达图数据
 * 按时间分组存储项目的多维度考核数值
 */
@Data
public class ProjectRadarChartVo {

    /**
     * 时间（年份）
     */
    String time;

    /**
     * 雷达图数值：外层 key 为维度/项目标识，内层为各维度对应的数值
     */
    HashMap<String, Map<String, Integer>> radarvalue;
}
