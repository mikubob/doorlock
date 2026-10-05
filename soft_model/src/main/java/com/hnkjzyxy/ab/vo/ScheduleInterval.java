package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 学校当地时间的有效占用区间。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleInterval {
    /**
     * 开始时刻。
     */
    private LocalDateTime start;
    /**
     * 结束时刻（当前占用不包含此时刻）。
     */
    private LocalDateTime end;
}
