package com.hnkjzyxy.ab.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentFontStyle;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 巡查统计导出数据（按班级）
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/5/9 16:26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@HeadRowHeight(30)  //表头行高
@ContentRowHeight(15)  //内容行高
@ColumnWidth(18)  //列宽
@ContentFontStyle(fontHeightInPoints = (short) 12)
public class CheckResultDataVo implements Serializable {

    /**
     * 开始时间
     */
    @ExcelProperty(value = "开始时间", index = 0)
    private String startTime;

    /**
     * 结束时间
     */
    @ExcelProperty(value = "结束时间", index = 1)
    private String endTime;

    /**
     * 查询班级
     */
    @ExcelProperty(value = "查询班级", index = 2)
    private String classes;

    /**
     * 统计条数
     */
    @ExcelProperty(value = "统计条数", index = 3)
    private Integer count;

    /**
     * 平均到课率
     */
    @ExcelProperty(value = "平均到课率", index = 4)
    private String avgArrivalRate;

    /**
     * 平均带食物率
     */
    @ExcelProperty(value = "平均带食物率", index = 5)
    private String avgFoodBringRate;

    /**
     * 辅导员
     */
    @ExcelProperty(value = "辅导员", index = 6)
    private String counsellor;

    /**
     * 学院
     */
    @ExcelProperty(value = "学院", index = 7)
    private String college;

}
