package com.hnkjzyxy.ab.vo;


import lombok.Getter;
import lombok.Setter;

/**
 * 巡查统计响应结果
 * 返回指定范围内的缺勤率、请假人次与带食物率
 */
@Setter
@Getter
public class CheckResultStatisticsVo {

    /**
     * 缺勤率
     */
    String absenteeismRate;

    /**
     * 请假人次
     */
    String numberLeaveRequests;

    /**
     * 带食物率
     */
    String foodCarryingRate;

    public CheckResultStatisticsVo() {
    }

    public CheckResultStatisticsVo(String absenteeismRate, String numberLeaveRequests, String foodCarryingRate) {

        this.absenteeismRate = absenteeismRate;
        this.numberLeaveRequests = numberLeaveRequests;
        this.foodCarryingRate = foodCarryingRate;
    }

    @Override
    public String toString() {
        return "CheckResultStatisticsVo{" +
                ", absenteeismRate='" + absenteeismRate + '\'' +
                ", numberLeaveRequests='" + numberLeaveRequests + '\'' +
                ", foodCarryingRate='" + foodCarryingRate + '\'' +
                '}';
    }

}
