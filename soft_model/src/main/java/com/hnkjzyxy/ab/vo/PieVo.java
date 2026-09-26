package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 饼图数据
 * 用于首页统计图表的前端渲染
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PieVo implements Serializable {

    /**
     * 饼图数据项列表
     */
    List<PieDataVo> data;

}
