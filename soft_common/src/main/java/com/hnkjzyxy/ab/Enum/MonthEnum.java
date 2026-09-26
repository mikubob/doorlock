package com.hnkjzyxy.ab.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum MonthEnum {


    January(1, "一月"),
    February(2, "二月"),
    March(3, "三月"),
    April(4, "四月"),
    May(5, "五月"),
    June(6, "六月"),
    July(7, "七月"),
    August(8, "八月"),
    September(9, "九月"),
    October(10, "十月"),
    November(11, "十一月"),
    December(12, "十二月");

    private Integer code;
    private String month;

}
