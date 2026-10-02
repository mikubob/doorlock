package com.hnkjzyxy.ab.export;

import com.alibaba.excel.EasyExcel;
import com.hnkjzyxy.ab.vo.AssessVo;
import com.hnkjzyxy.ab.vo.CheckResultByTeacherDataVo;
import com.hnkjzyxy.ab.vo.CheckResultDataVo;
import com.hnkjzyxy.ab.vo.StudentInfoVo;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Excel 下载响应工具，将导出数据及对应列映射写入 HTTP 响应
 */
public final class ExcelResponseExporter {
    /**
     * 工具类构造方法，禁止直接实例化
     */
    private ExcelResponseExporter() { }

    /**
     * 将项目考核结果写入 Excel 下载响应
     *
     * @param assessVos 项目考核结果展示信息列表
     * @param response HTTP 响应对象
     */
    public static void exportAssess(List<AssessVo> assessVos, HttpServletResponse response) {
        try {
            //HttpServletResponse消息头参数设置
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
            String fileName = "考核结果导出" + ".xlsx";
            fileName = new String(fileName.getBytes(), "ISO-8859-1");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            EasyExcel.write(response.getOutputStream(), AssessVo.class)
                    .autoCloseStream(Boolean.FALSE)
                    .sheet("导出列表")
                    .doWrite(assessVos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 导出查询统计结果
     *
     * @param assessVos 班级维度统计结果
     * @param response  HTTP 响应
     */
    public static void exportSchedule(List<CheckResultDataVo> assessVos, HttpServletResponse response) {
        try {
            //HttpServletResponse消息头参数设置
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
            String fileName = "教学查询统计结果导出" + ".xlsx";
            fileName = new String(fileName.getBytes(), "ISO-8859-1");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            EasyExcel.write(response.getOutputStream(), CheckResultDataVo.class)
                    .autoCloseStream(Boolean.FALSE)
                    .sheet("导出列表")
                    .doWrite(assessVos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 导出教师版教学查询分析
     *
     * @param assessVos 教师维度统计结果
     * @param response  HTTP 响应
     */
    public static void exportScheduleByTeacher(List<CheckResultByTeacherDataVo> assessVos, HttpServletResponse response) {
        try {
            //HttpServletResponse消息头参数设置
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
            String fileName = "教学查询统计结果导出(教师版).xlsx";
            fileName = new String(fileName.getBytes(), "ISO-8859-1");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            EasyExcel.write(response.getOutputStream(), CheckResultByTeacherDataVo.class)
                    .autoCloseStream(Boolean.FALSE)
                    .sheet("导出列表")
                    .doWrite(assessVos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将学生录取结果写入 Excel 下载响应
     *
     * @param assessVos 学生录取展示信息列表
     * @param response HTTP 响应对象
     */
    public static void exportStudentInfo(List<StudentInfoVo> assessVos, HttpServletResponse response) {
        try {
            // HttpServletResponse消息头参数设置
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");

            String fileName = "学生录取结果导出.xlsx";
            fileName = URLEncoder.encode(fileName, "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            // 使用EasyExcel进行导出
            EasyExcel.write(response.getOutputStream(), StudentInfoVo.class)
                    .autoCloseStream(Boolean.FALSE)
                    .sheet("导出列表")
                    .doWrite(assessVos);

            // 刷新输出流
            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("导出Excel文件时发生异常: " + e.getMessage());
        } finally {
            try {
                response.getOutputStream().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
