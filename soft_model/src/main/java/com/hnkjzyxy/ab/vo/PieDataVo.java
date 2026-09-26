package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 饼图单项数据
 * 用于首页统计图表的前端渲染
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PieDataVo implements Serializable {

    /**
     * 数值
     */
    private Integer value;

    /**
     * 名称
     */
    private String name;

}
