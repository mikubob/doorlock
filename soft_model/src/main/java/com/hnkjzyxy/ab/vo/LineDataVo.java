package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 折线图数据系列
 * 折线图 LineVo 中单个系列的数据
 *
 * @author 16702
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LineDataVo implements Serializable {

    /**
     * 系列名称
     */
    private String name;

    /**
     * 图表类型（默认 line）
     */
    private String type = "line";

    /**
     * 系列标题（默认 Total）
     */
    private String tiles = "Total";

    /**
     * 数据点列表
     */
    private List<String> data;

}
