package com.hnkjzyxy.ab.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 月份枚举，保存月份编号及中文名称
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum MonthEnum {


    /**
     * 一月
     */
    January(1, "一月"),
    /**
     * 二月
     */
    February(2, "二月"),
    /**
     * 三月
     */
    March(3, "三月"),
    /**
     * 四月
     */
    April(4, "四月"),
    /**
     * 五月
     */
    May(5, "五月"),
    /**
     * 六月
     */
    June(6, "六月"),
    /**
     * 七月
     */
    July(7, "七月"),
    /**
     * 八月
     */
    August(8, "八月"),
    /**
     * 九月
     */
    September(9, "九月"),
    /**
     * 十月
     */
    October(10, "十月"),
    /**
     * 十一月
     */
    November(11, "十一月"),
    /**
     * 十二月
     */
    December(12, "十二月");

    /**
     * 月份编号，一至十二
     */
    private Integer code;
    /**
     * 月份中文名称
     */
    private String month;

}
