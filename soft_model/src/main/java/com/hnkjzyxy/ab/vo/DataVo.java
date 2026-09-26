package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图表通用数据
 * 用于柱状图等统计图表的 X/Y 轴数据传递
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataVo {

    /**
     * X 轴数据
     */
    private List<String> x;

    /**
     * Y 轴数据
     */
    private List<String> y;

}
