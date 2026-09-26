package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 雷达图数据
 * 用于首页个人/教研室考核结果的多维度展示
 *
 * @version 1.0
 * @author Spell a
 * @date 2024-01-17 11:02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RadarChartVo {

    /**
     * 维度名称列表
     */
    private List<String> metrics;

    /**
     * 各维度对应的数值列表
     */
    private List<Integer> value;

}
