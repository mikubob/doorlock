package com.hnkjzyxy.ab.service.impl;

import com.hnkjzyxy.ab.service.excel.StudentInfoExcelImportService;
import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.dto.AdmissionDto;
import com.hnkjzyxy.ab.dto.BatchAdmitDto;
import com.hnkjzyxy.ab.mapper.MajorDetailsMapper;
import com.hnkjzyxy.ab.mapper.StudentInfoMapper;
import com.hnkjzyxy.ab.model.MajorDetails;
import com.hnkjzyxy.ab.model.StudentInfo;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.model.UserRole;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.StudentInfoService;
import com.hnkjzyxy.ab.service.UserRoleService;
import com.hnkjzyxy.ab.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * (StudentInfo)表服务实现类
 *
 * @author Binc
 * @since 2025-04-14 17:44:23
 */
@Service
public class StudentInfoServiceImpl extends ServiceImpl<StudentInfoMapper, StudentInfo> implements StudentInfoService {
    /**
     * 编程式事务模板
     */
    @Autowired
    private TransactionTemplate transactionTemplate;
    /**
     * 学生录取信息数据访问接口
     */
    @Autowired
    private StudentInfoMapper studentInfoMapper;
    /**
     * 密码编码器
     */
    @Autowired
    private PasswordEncoder passwordEncoder;
    /**
     * 用户角色关联业务服务
     */
    @Autowired
    private UserRoleService userRoleService;
    /**
     * 专业录取名额数据访问接口
     */
    @Autowired
    private MajorDetailsMapper majorDetailsMapper;
    /**
     * 用户业务服务
     */
    @Autowired
    private UserService userService;
    /**
     * 学生信息 Excel 导入服务
     */
    @Autowired
    private StudentInfoExcelImportService studentInfoExcelImportService;

    /**
     * {@inheritDoc}
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void uploadStudentInfo(MultipartFile file) {
        // 执行事务操作，确保数据一致性
        transactionTemplate.execute(status -> {
            try {
                // 读取课程Excel文件，导入学生数据
                studentInfoExcelImportService.readStudentInfoExcel(file);
            } catch (Exception e) {
                // 异常处理，抛出运行时异常，终止事务
                throw new RuntimeException(e.getMessage());
            }
            // 事务执行完毕，返回null
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResult updateStudentInfo(StudentInfo studentInfo) {
        studentInfoMapper.updateById(studentInfo);
        return ApiResult.ok("修改成功！");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResult deleteStudentInfoById(Long id) {
        studentInfoMapper.deleteById(id);
        return ApiResult.ok("删除成功！");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResult getStudentInfoByStudentId(String studentId) {
        StudentInfo studentInfo = studentInfoMapper.getStudentInfoByStudentId(studentId);
        return ApiResult.ok("data", studentInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResult getStudentInfoList(AdmissionDto dto) {
        // 创建 LambdaQueryWrapper 对象
        LambdaQueryWrapper<StudentInfo> queryWrapper = new LambdaQueryWrapper<>();

        // 条件：del_flag = 1
        queryWrapper.eq(StudentInfo::getDelFlag, 1);

        // 根据专业方向查询，志愿一跟志愿二
        if (dto.getMajorFilter() != null && !dto.getMajorFilter().isEmpty()) {
            queryWrapper.eq(StudentInfo::getApplicationOne, dto.getMajorFilter());
        }
        // 根据录取状态
        if (dto.getStatus() != null) {
            queryWrapper.eq(StudentInfo::getStatus, dto.getStatus());
        }

        // 根据班级名
        if (dto.getClassName() != null && !dto.getClassName().isEmpty()) {
            queryWrapper.eq(StudentInfo::getClassName, dto.getClassName());
        }

        // 根据姓名
        if (dto.getName() != null && !dto.getName().isEmpty()) {
            queryWrapper.eq(StudentInfo::getName, dto.getName());
        }

        // 根据录取结果
        if (dto.getResult() != null && !dto.getResult().isEmpty()) {
            queryWrapper.eq(StudentInfo::getResult, dto.getResult());
        }

        // 根据专业分数排序
        if (dto.getScore() != null && !dto.getScore().isEmpty()) {
            switch (dto.getScore()) {
                case "web_score":
                    queryWrapper.orderByDesc(StudentInfo::getWebScore);
                    break;
                case "java_score":
                    queryWrapper.orderByDesc(StudentInfo::getJavaScore);
                    break;
                case "program_score":
                    queryWrapper.orderByDesc(StudentInfo::getProgramScore);
                    break;
                case "database_score":
                    queryWrapper.orderByDesc(StudentInfo::getDatabaseScore);
                    break;
                default:
                    // 如果传入的值无效，可以选择不排序或抛出异常
                    break;
            }
        }
        // 根据成绩汇总排序
        if (dto.getTotal() != null && !dto.getTotal().isEmpty()) {
            switch (dto.getTotal()) {
                case "web_total":
                    queryWrapper.orderByDesc(StudentInfo::getWebTotal);
                    break;
                case "java_total":
                    queryWrapper.orderByDesc(StudentInfo::getJavaTotal);
                    break;
                case "program_total":
                    queryWrapper.orderByDesc(StudentInfo::getProgramTotal);
                    break;
                case "database_total":
                    queryWrapper.orderByDesc(StudentInfo::getProgramTotal);
                    break;
            }
        }

        // 执行查询
        List<StudentInfo> studentInfoList = studentInfoMapper.selectList(queryWrapper);
//        List<StudentInfo> studentInfoList = studentInfoMapper.selectStudentInfoList(dto);
        return ApiResult.ok("data", studentInfoList);
    }

    /**
     * 批量更新学生录取状态
     *
     * @param dto 学生录取信息操作或查询参数
     * @return 统一接口响应
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult batchAdmit(BatchAdmitDto dto) {
        // 参数校验
        if (dto == null || dto.getStudentIds() == null || dto.getStudentIds().isEmpty() || StringUtils.isBlank(dto.getAdmittedMajor())) {
            return ApiResult.error("请求参数不能为空");
        }

        // 获取专业信息（带行锁）
        MajorDetails major = majorDetailsMapper.selectOne(new LambdaQueryWrapper<MajorDetails>()
                .eq(MajorDetails::getMajorName, dto.getAdmittedMajor())
                .last("FOR UPDATE")); // 悲观锁

        if (major == null) {
            return ApiResult.error("专业 " + dto.getAdmittedMajor() + " 不存在");
        }

        // 计算剩余名额
        int remaining = major.getNumber() - major.getAccepted();
        if (remaining <= 0) {
            return ApiResult.error("专业已录满");
        }

        // 校验录取数量
        int toAdmit = Math.min(dto.getStudentIds().size(), remaining);
        if (toAdmit <= 0) {
            log.error("无可录取名额");
            return ApiResult.error("无可录取名额");
        }

        // 分页获取有效学生（避免全表扫描）
        List<StudentInfo> validStudents = studentInfoMapper.selectList(
                new LambdaQueryWrapper<StudentInfo>()
                        .in(StudentInfo::getStudentId, dto.getStudentIds())
                        .eq(StudentInfo::getStatus, 0)
                        .last("LIMIT " + toAdmit)
        );

        if (CollectionUtils.isEmpty(validStudents)) {
            log.error("没有符合录取条件的学生");
            return ApiResult.error("没有符合录取条件的学生");
        }

        // 获取实际可录取ID
        List<Long> validIds = validStudents.stream()
                .map(StudentInfo::getId)
                .filter(Objects::nonNull) // 过滤掉 null 的 ID
                .collect(Collectors.toList());

        if (validIds.isEmpty()) {
            log.error("没有符合录取条件的学生");
            return ApiResult.error("没有符合录取条件的学生");
        }

        // 原子更新学生状态
        int updated = studentInfoMapper.update(null, new LambdaUpdateWrapper<StudentInfo>()
                .in(StudentInfo::getId, validIds)
                .eq(StudentInfo::getStatus, 0) // 二次校验
                .set(StudentInfo::getResult, major.getMajorName())
                .set(StudentInfo::getStatus, 1)
                .set(StudentInfo::getUpdateTime, LocalDateTime.now()));

        // 匹配更新数量
        if (updated != validIds.size()) {
            throw new RuntimeException("学生状态变更不一致，触发回滚");
        }

        // 原子更新专业人数
        int affect = majorDetailsMapper.updateAccepted(major.getId(), updated);
        if (affect == 0) {
            throw new RuntimeException("专业名额更新失败，触发回滚");
        }

        return ApiResult.ok("成功录取 " + updated + " 人");

    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResult synchronization() {
        // 查询所有学生信息
        LambdaQueryWrapper<StudentInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StudentInfo::getDelFlag, 1);
        List<StudentInfo> studentInfos = studentInfoMapper.selectList(queryWrapper);
        if (studentInfos.isEmpty()) {
            return ApiResult.error("没有需要同步的学生信息");
        }

        // 创建用户列表
        List<User> userList = studentInfos.stream().map(studentInfo -> {
            User user = new User();
            user.setNickName(studentInfo.getName()); // name -> nickName
            user.setUserName(studentInfo.getStudentId().toString()); // studentId -> userName
            user.setPassword(passwordEncoder.encode(studentInfo.getStudentId().toString())); // studentId -> password
            user.setMajor(studentInfo.getClassName()); // className -> major
            user.setEntryTime(new Date());
            user.setStatus(1); // 默认状态为1
            return user;
        }).collect(Collectors.toList());

        // 批量插入用户
        boolean isUserSaved = userService.saveBatch(userList, 1000); // 每批次插入1000条
        if (!isUserSaved) {
            throw new RuntimeException("用户信息批量插入失败");
        }

        // 创建用户角色关联列表
        List<UserRole> userRoleList = userList.stream().map(user -> {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getUserId()); // 获取生成的 userId
            userRole.setRoleId(77); // 固定角色ID
            return userRole;
        }).collect(Collectors.toList());

        // 批量插入用户角色关联
        boolean isUserRoleSaved = userRoleService.saveBatch(userRoleList, 100); // 每批次插入100条
        if (!isUserRoleSaved) {
            throw new RuntimeException("用户角色关联批量插入失败");
        }

        return ApiResult.ok("同步成功！");
    }
    /*@Scheduled(cron = "0 0/1 * * * ?") // 每分钟检查一次
    public void checkEndTimeAndAdmit() {
        // 获取当前时间
        Date now = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        // 查出结束时间
        LambdaQueryWrapper<StudentInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StudentInfo::getDelFlag, 1)
                .last("LIMIT 1");
        StudentInfo studentInfo = studentInfoMapper.selectOne(queryWrapper);
        if (studentInfo != null && studentInfo.getEndTime() != null) {
            // 比较当前时间与结束时间
            if (now.after(studentInfo.getEndTime())) {
                autoAdmit(); // 调用自动录取方法
            }
        }
    }*/
//    @Override
//    public ApiResult autoAdmit() {
//        // 1. 获取所有专业的信息
//        List<MajorDetails> majorDetails = majorDetailsMapper.selectMajorDetails(); // 查询所有专业的详细信息
//
//        // 遍历每个专业，逐一处理自动录取逻辑
//        for (MajorDetails majorDetail : majorDetails) {
//            // 设置该专业的录取人数（总人数减去20）
//            int admitNumber = majorDetail.getNumber() - 20; // 计算需要录取的学生数量
//
//            // 如果录取人数小于等于0，则跳过该专业的录取逻辑
//            if (admitNumber <= 0) {
//                continue; // 跳过当前循环，处理下一个专业
//            }
//
//            // 2. 查询该专业下的学生信息（根据志愿和当前状态）
//            LambdaQueryWrapper<StudentInfo> queryWrapper = new LambdaQueryWrapper<>(); // 创建查询条件构造器
//            queryWrapper.eq(StudentInfo::getStatus, 0) // 筛选待录取状态的学生（假设0表示待录取）
//                    .and(wrapper -> wrapper.eq(StudentInfo::getApplicationOne, majorDetail.getMajorName()) // 第一志愿匹配当前专业
//                            .or().eq(StudentInfo::getApplicationTwo, majorDetail.getMajorName())); // 或者第二志愿匹配当前专业
//
//            // 执行查询，获取符合条件的学生列表
//            List<StudentInfo> studentInfos = studentInfoMapper.selectList(queryWrapper); // 查询符合录取条件的学生
//
//            // 如果没有符合条件的学生，则跳过该专业的录取逻辑
//            if (studentInfos == null || studentInfos.isEmpty()) {
//                continue; // 跳过当前循环，处理下一个专业
//            }
//
//            // 3. 根据单科成绩对学生进行排序并录取
//            List<StudentInfo> admittedStudents = new ArrayList<>(); // 存储最终录取的学生列表
//
//            // 按网页设计与制作成绩排序，取前 admitNumber 名
//            List<StudentInfo> webScoreSorted = studentInfos.stream()
//                    .sorted(Comparator.comparingInt(StudentInfo::getWebScore).reversed()) // 按成绩降序排序
//                    .limit(admitNumber) // 取前 admitNumber 名
//                    .collect(Collectors.toList()); // 收集结果为列表
//            admittedStudents.addAll(webScoreSorted); // 将排序结果加入录取列表
//
//            // 按面向对象程序设计（Java）成绩排序，取前 admitNumber 名
//            List<StudentInfo> javaScoreSorted = studentInfos.stream()
//                    .sorted(Comparator.comparingInt(StudentInfo::getJavaScore).reversed()) // 按成绩降序排序
//                    .limit(admitNumber) // 取前 admitNumber 名
//                    .collect(Collectors.toList()); // 收集结果为列表
//            admittedStudents.addAll(javaScoreSorted); // 将排序结果加入录取列表
//
//            // 按程序设计基础成绩排序，取前 admitNumber 名
//            List<StudentInfo> programScoreSorted = studentInfos.stream()
//                    .sorted(Comparator.comparingInt(StudentInfo::getProgramScore).reversed()) // 按成绩降序排序
//                    .limit(admitNumber) // 取前 admitNumber 名
//                    .collect(Collectors.toList()); // 收集结果为列表
//            admittedStudents.addAll(programScoreSorted); // 将排序结果加入录取列表
//
//            // 按数据库应用技术成绩排序，取前 admitNumber 名
//            List<StudentInfo> databaseScoreSorted = studentInfos.stream()
//                    .sorted(Comparator.comparingInt(StudentInfo::getDatabaseScore).reversed()) // 按成绩降序排序
//                    .limit(admitNumber) // 取前 admitNumber 名
//                    .collect(Collectors.toList()); // 收集结果为列表
//            admittedStudents.addAll(databaseScoreSorted); // 将排序结果加入录取列表
//
//            // 去重（如果一个学生在多门课程中都被选中）
//            Set<Long> uniqueStudentIds = admittedStudents.stream()
//                    .map(StudentInfo::getId) // 提取学生的唯一标识ID
//                    .collect(Collectors.toSet()); // 使用Set去重
//
//            List<StudentInfo> finalAdmittedStudents = admittedStudents.stream()
//                    .filter(studentInfo -> uniqueStudentIds.remove(studentInfo.getId())) // 过滤掉重复的学生
//                    .collect(Collectors.toList()); // 收集结果为最终录取列表
//            // 如果最终录取的学生数量超过剩余名额，则截取前 admitNumber 名
//            if (finalAdmittedStudents.size() > admitNumber) {
//                finalAdmittedStudents = finalAdmittedStudents.subList(0, admitNumber); // 截取前 admitNumber 名
//            }
//            // 4. 自动录取这些学生
//            for (StudentInfo studentInfo : finalAdmittedStudents) {
//                // 更新学生信息状态为已录取
//                boolean isAdmit = studentInfoMapper.update(null, new LambdaUpdateWrapper<StudentInfo>()
//                        .eq(StudentInfo::getId, studentInfo.getId()) // 匹配学生ID
//                        .set(StudentInfo::getResult, majorDetail.getMajorName()) // 设置录取专业名称
//                        .set(StudentInfo::getStatus, 1) // 设置状态为已录取（1表示已录取）
//                        .set(StudentInfo::getUpdateTime, LocalDateTime.now())) > 0; // 更新时间戳
//
//                // 如果更新失败，抛出异常，触发事务回滚
//                if (!isAdmit) {
//                    throw new RuntimeException("学生录取状态更新失败，触发回滚");
//                }
//            }
//
//            // 5. 更新专业已录取人数
//            int updated = majorDetailsMapper.updateAccepted(majorDetail.getId(), finalAdmittedStudents.size()); // 更新专业已录取人数
//            if (updated == 0) {
//                throw new RuntimeException("专业录取人数更新失败，触发回滚");
//            }
//        }
//        // 返回成功结果
//        return ApiResult.ok("自动录取成功！");
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResult getClassName() {
        List<String> classNameList = studentInfoMapper.selectClassNameList();
        return ApiResult.ok("data", classNameList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResult updateEndTime(Date endTime) {
        List<StudentInfo> studentInfoList = studentInfoMapper.selectList(null);
        // 将所有 studentInfoList 中的 endTime 更新为 endTime
        studentInfoList.forEach(studentInfo -> {
            studentInfo.setEndTime(endTime);
            studentInfoMapper.updateById(studentInfo);
        });
        // 返回成功的 ApiResult
        return ApiResult.ok("截止时间更新成功！");
    }
}

