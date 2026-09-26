package com.hnkjzyxy.ab.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentFontStyle;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考核成绩导出数据
 * 项目考核结果的 Excel 导出对象
 *
 * @version 1.0
 * @author Spell a
 * @date 2024-01-17 17:04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@HeadRowHeight(30)  //表头行高
@ContentRowHeight(15)  //内容行高
@ColumnWidth(18)  //列宽
@ContentFontStyle(fontHeightInPoints = (short) 12)
public class AssessVo {

    /**
     * 项目名称
     */
    @ExcelProperty("项目名称")
    private String projectName;

    /**
     * 开始时间
     */
    @ExcelProperty("开始时间")
    private String startTime;

    /**
     * 结束时间
     */
    @ExcelProperty("结束时间")
    private String endTime;

    /**
     * 用户姓名
     */
    @ExcelProperty("用户姓名")
    private String nickName;

    /**
     * 用户工号
     */
    @ExcelProperty("用户工号")
    private String userName;

    /**
     * 教研室
     */
    @ExcelProperty("教研室")
    private String major;

    /**
     * 成绩
     */
    @ExcelProperty("成绩")
    private String score;

    /**
     * 排名
     */
    @ExcelProperty("排名")
    private String rank;

    /**
     * 状态（已完成/未完成）
     */
    @ExcelProperty("状态")
    private String status;

}
