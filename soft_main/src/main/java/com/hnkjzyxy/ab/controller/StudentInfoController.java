package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.export.ExcelResponseExporter;
import cn.hutool.core.util.ObjectUtil;
import com.hnkjzyxy.ab.dto.AdmissionDto;
import com.hnkjzyxy.ab.dto.BatchAdmitDto;
import com.hnkjzyxy.ab.model.StudentInfo;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.StudentInfoService;
import com.hnkjzyxy.ab.vo.StudentInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

/**
 * 学生信息管理
 * 提供学生信息的导入、查询、修改、删除与批量录取接口
 */
@RestController
@RequestMapping("/studentInfo")
public class StudentInfoController {
    @Autowired
    private StudentInfoService studentInfoService;

    /**
     * 上传学生信息
     *
     * @param file 学生信息 Excel 文件
     * @return 操作结果
     */
    @PostMapping("/upload")
    public ApiResult uploadStudentInfo(@RequestParam("file") MultipartFile file) {
        studentInfoService.uploadStudentInfo(file);
        return ApiResult.ok("上传成功！");
    }

    /**
     * 设置学生信息填报截止时间
     *
     * @param endTime 截止时间
     * @return 操作结果
     */
    @PutMapping("/updateEndTime")
    public ApiResult updateEndTime(@RequestParam Date endTime) {
        return studentInfoService.updateEndTime(endTime);
    }

    /**
     * 修改学生信息
     *
     * @param studentInfo 学生信息
     * @return 操作结果
     */
    @PutMapping
    public ApiResult updateStudentInfo(@RequestBody StudentInfo studentInfo) {
        return studentInfoService.updateStudentInfo(studentInfo);
    }

    /**
     * 删除学生信息
     *
     * @param id 学生信息ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult deleteStudentInfo(@PathVariable Long id) {
        return studentInfoService.deleteStudentInfoById(id);
    }

    /**
     * 获取当前学生用户的信息
     *
     * @return 当前登录学生的信息
     */
    @GetMapping("/ByStudentId")
    public ApiResult getStudentInfoByStudentId(Authentication authentication) {
        return studentInfoService.getStudentInfoByStudentId(authentication.getName());
    }

    /**
     * 条件查询学生信息
     *
     * @param dto 学生信息查询条件
     * @return 学生信息列表及分页数据
     */
    @GetMapping
    public ApiResult getStudentInfoList(AdmissionDto dto) {
        return studentInfoService.getStudentInfoList(dto);
    }

    /**
     * 获取所有班级名称
     *
     * @return 班级名称列表
     */
    @GetMapping("/getClassName")
    public ApiResult getClassName() {
        return studentInfoService.getClassName();
    }


    /**
     * 批量录取
     *
     * @param dto 批量录取信息
     * @return 录取结果
     */
    @PostMapping("/admit")
    public ApiResult batchAdmit(@RequestBody BatchAdmitDto dto) {
        return studentInfoService.batchAdmit(dto);
    }

    /**
     * 导出学生信息为 Excel
     *
     * @param resultVo 待导出的学生信息
     * @param response HTTP 响应流，直接输出 Excel 文件
     */
    @PostMapping("/export")
    public void exportAssess(@RequestBody List<StudentInfoVo> resultVo, HttpServletResponse response) {
        System.out.println(resultVo);
        if (ObjectUtil.isEmpty(resultVo)) {
            throw new RuntimeException("导出结果不能为空！");
        }
        ExcelResponseExporter.exportStudentInfo(resultVo, response);
    }

    /**
     * 同步学生账号
     *
     * @return 同步结果
     */
    @GetMapping("/synchronization")
    public ApiResult synchronization() {
        return studentInfoService.synchronization();
    }

    /**
     * 自动录取
     * @return
     */
//    @GetMapping("/autoAdmit")
//    public ApiResult admit(){
//        return studentInfoService.autoAdmit();
//    }
}
