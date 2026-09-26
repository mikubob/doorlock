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


    void uploadStudentInfo(MultipartFile file);

    ApiResult updateStudentInfo(StudentInfo studentInfo);

    ApiResult deleteStudentInfoById(Long id);

    ApiResult getStudentInfoByStudentId(String studentId);

    ApiResult getStudentInfoList(AdmissionDto dto);

    ApiResult batchAdmit(BatchAdmitDto dto);

    ApiResult synchronization();

    ApiResult getClassName();

    ApiResult updateEndTime(Date endTime);
}

