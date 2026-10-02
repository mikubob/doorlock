package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.dto.AdmissionDto;
import com.hnkjzyxy.ab.dto.BatchAdmitDto;
import com.hnkjzyxy.ab.model.StudentInfo;
import com.hnkjzyxy.ab.result.ApiResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
 * (StudentInfo)表服务接口
 *
 * @author Binc
 * @since 2025-04-14 17:44:22
 */
public interface StudentInfoService extends IService<StudentInfo> {


    /**
     * 导入学生录取信息文件
     *
     * @param file 待处理文件
     */
    void uploadStudentInfo(MultipartFile file);

    /**
     * 更新学生录取信息
     *
     * @param studentInfo 学生录取信息信息
     * @return 统一接口响应
     */
    ApiResult updateStudentInfo(StudentInfo studentInfo);

    /**
     * 删除指定学生录取信息
     *
     * @param id 学生录取信息ID
     * @return 统一接口响应
     */
    ApiResult deleteStudentInfoById(Long id);

    /**
     * 按学号查询学生录取信息
     *
     * @param studentId 学生学号
     * @return 统一接口响应
     */
    ApiResult getStudentInfoByStudentId(String studentId);

    /**
     * 按录取查询条件查询学生信息
     *
     * @param dto 学生录取信息操作或查询参数
     * @return 统一接口响应
     */
    ApiResult getStudentInfoList(AdmissionDto dto);

    /**
     * 批量更新学生录取状态
     *
     * @param dto 学生录取信息操作或查询参数
     * @return 统一接口响应
     */
    ApiResult batchAdmit(BatchAdmitDto dto);

    /**
     * 同步学生录取信息到用户及角色数据
     *
     * @return 统一接口响应
     */
    ApiResult synchronization();

    /**
     * 查询学生班级名称
     *
     * @return 统一接口响应
     */
    ApiResult getClassName();

    /**
     * 更新录取截止时间
     *
     * @param endTime 查询结束时间
     * @return 统一接口响应
     */
    ApiResult updateEndTime(Date endTime);
}

