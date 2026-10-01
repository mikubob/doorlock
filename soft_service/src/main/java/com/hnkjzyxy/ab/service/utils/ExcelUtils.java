package com.hnkjzyxy.ab.service.utils;

import com.alibaba.excel.EasyExcel;
import com.hnkjzyxy.ab.config.CheckResultImportProperties;
import com.hnkjzyxy.ab.model.CheckResultImportResult;
import com.hnkjzyxy.ab.service.CheckResultService;
import com.hnkjzyxy.ab.service.CourseService;
import com.hnkjzyxy.ab.service.StudentInfoService;
import com.hnkjzyxy.ab.service.TaskService;
import com.hnkjzyxy.ab.service.UserRoleService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.service.listener.CheckResultDataListener;
import com.hnkjzyxy.ab.service.listener.CheckResultModel;
import com.hnkjzyxy.ab.service.listener.CourseDataListener;
import com.hnkjzyxy.ab.service.listener.CourseModel;
import com.hnkjzyxy.ab.service.listener.StudentInfoDataListener;
import com.hnkjzyxy.ab.service.listener.StudentInfoModel;
import com.hnkjzyxy.ab.service.listener.TaskDataListener;
import com.hnkjzyxy.ab.service.listener.TaskModel;
import com.hnkjzyxy.ab.service.listener.UserDataListener;
import com.hnkjzyxy.ab.service.listener.UserModel;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;
import com.hnkjzyxy.ab.vo.AssessVo;
import com.hnkjzyxy.ab.vo.CheckResultByTeacherDataVo;
import com.hnkjzyxy.ab.vo.CheckResultDataVo;
import com.hnkjzyxy.ab.vo.StudentInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-04 21:07
 */
@Component
public class ExcelUtils {

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private SnowFlowUtils snowFlowUtils;

    @Autowired
    private CheckResultService checkResultService;

    @Autowired
    private StudentInfoService studentInfoService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CheckResultImportProperties checkResultImportProperties;

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

    /**
     * 读取考核任务模板
     *
     * @param file      上传的 Excel 文件
     * @param projectId 项目ID
     * @throws Exception 解析异常
     */
    public void readTaskExcel(MultipartFile file, Integer projectId) throws Exception {
        String filename = file.getOriginalFilename();
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空！");
        }
        if (!filename.endsWith("xls") && !filename.endsWith("xlsx")) {
            throw new RuntimeException("上传文件的类型必须是xls或者xlsx!");
        }
        long size = file.getSize();
        double length = size / 1048576;
        if (length > 100) {
            throw new RuntimeException("上传的文件大小不能超过100MB!");
        }
        EasyExcel.read(file.getInputStream(), TaskModel.class, new TaskDataListener(taskService, projectId, snowFlowUtils)).doReadAll();
    }

    /**
     * 读取学期课程表模板
     *
     * @param file
     * @throws Exception
     */
    public void readCourseExcel(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空！");
        }
        if (!filename.endsWith("xls") && !filename.endsWith("xlsx")) {
            throw new RuntimeException("上传文件的类型必须是xls或者xlsx!");
        }
        long size = file.getSize();
        double length = size / 1048576;
        if (length > 100) {
            throw new RuntimeException("上传的文件大小不能超过100MB!");
        }
        EasyExcel.read(file.getInputStream(), CourseModel.class, new CourseDataListener(courseService, snowFlowUtils)).doReadAll();
    }

    /**
     * 读取巡查结果 Excel 并导入
     * <p>
     * 解析全部完成后在监听器内部以单事务落库；解析失败的行被跳过并登记行号与原因，
     * 不中断整次导入。
     * 表头固定为一行，读取全部工作表后再统一校验和提交。
     * </p>
     *
     * @param file          上传的 Excel 文件
     * @param sourceCollege 来源学院，策略为 FROM_UPLOADER 时取上传人所属学院
     * @return 导入回执，含总行数 / 成功数 / 失败数 / 错误清单
     * @throws Exception 文件校验或解析异常
     */
    public CheckResultImportResult readScheduleExcel(MultipartFile file, String sourceCollege) throws Exception {
        String filename = file.getOriginalFilename();
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空！");
        }
        if (filename == null || (!filename.toLowerCase(Locale.ROOT).endsWith(".xls")
                && !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx"))) {
            throw new RuntimeException("上传文件的类型必须是xls或者xlsx!");
        }
        if (file.getSize() > 100L * 1024 * 1024) {
            throw new RuntimeException("上传的文件大小不能超过100MB!");
        }
        CheckResultDataListener listener = new CheckResultDataListener(checkResultService, snowFlowUtils,
                transactionTemplate, checkResultImportProperties, sourceCollege);
        EasyExcel.read(file.getInputStream(), CheckResultModel.class, listener)
                .headRowNumber(1)
                .doReadAll();
        return listener.finishAndSave();
    }


    public void readUserExcel(String path) {
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    EasyExcel.read(path, UserModel.class, new UserDataListener(userService, bCryptPasswordEncoder, userRoleService)).doReadAll();
                }
            });
        } catch (Exception e) {
            // Transaction will be automatically rolled back due to exception
            throw e;
        }
    }

    public void readStudentInfoExcel(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空！");
        }
        if (!filename.endsWith("xls") && !filename.endsWith("xlsx")) {
            throw new RuntimeException("上传文件的类型必须是xls或者xlsx!");
        }
        long size = file.getSize();
        double length = size / 1048576;
        if (length > 100) {
            throw new RuntimeException("上传的文件大小不能超过100MB!");
        }
        //读取第二个sheet页
        EasyExcel.read(file.getInputStream(), StudentInfoModel.class, new StudentInfoDataListener(studentInfoService, snowFlowUtils)).doReadAll();

    }
}
