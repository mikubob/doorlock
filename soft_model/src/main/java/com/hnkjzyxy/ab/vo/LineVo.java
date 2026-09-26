package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 折线图数据
 * 用于首页统计图表的前端渲染
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LineVo implements Serializable {

    /**
     * 图例
     */
    private List<String> legend;

    /**
     * X 轴刻度
     */
    private List<String> xAxis;

    /**
     * 数据系列
     */
    private List<LineDataVo> series;

}
