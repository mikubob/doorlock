package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Exam;

import java.util.List;

/**
 * 考试信息Service接口
 */
public interface ExamService extends IService<Exam> {

    /**
     * 按学校时间刷新普通考试状态，保留终态并记录系统操作。
     * @return 更新记录数
     */
    int refreshStatuses();

    /**
     * 动态查询考试列表
     *
     * @param exam 考试查询条件
     * @return 考试列表
     */
    List<Exam> getExamList(Exam exam);

    /**
     * 按电子班牌SN删除考试安排
     *
     * @param boardSn 电子班牌SN
     * @return 操作或条件校验结果
     */
    boolean removeByBoardSn(String boardSn);

    /**
     * 批量添加考试
     * 功能说明：
     * 1. 验证每条考试数据的完整性（教室、考试号、时间等）
     * 2. 验证教室是否存在
     * 3. 检查与数据库中已有考试的时间冲突
     * 4. 检查当前批次中考试之间的时间冲突
     * 5. 设置默认状态值
     * 6. 执行批量插入操作
     *
     * @param exams 考试列表
     * @return 操作结果，成功返回null，失败返回错误信息
     */
    String batchAdd(List<Exam> exams);

    /**
     * 共同事务内整批删除考试。
     * @param ids 考试主键
     * @return 是否成功
     */
    boolean deleteExams(List<Long> ids);

    /**
     * 只验证本批最终排程，不保存或创建课程调整。
     * @param exams 最终考试批次
     * @param update 是否为更新批次
     * @return 校验错误，成功返回空
     */
    String preview(List<Exam> exams, boolean update);

    /**
     * 批量更新考试
     * 功能说明：
     * 1. 验证每条考试数据的ID是否存在
     * 2. 验证考试是否存在
     * 3. 验证教室是否存在（如果修改了教室）
     * 4. 合并原始数据和更新数据（支持部分字段更新）
     * 5. 检查与数据库中已有考试的时间冲突
     * 6. 检查当前批次中考试之间的时间冲突
     * 7. 执行批量更新操作
     *
     * @param exams 考试列表（必须包含id）
     * @return 操作结果，成功返回null，失败返回错误信息
     */
    String batchUpdate(List<Exam> exams);
}
