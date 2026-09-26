package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Exam;

import java.util.List;

/**
 * 考试信息Service接口
 */
public interface ExamService extends IService<Exam> {

    /**
     * 动态查询考试列表
     *
     * @param exam 考试查询条件
     * @return 考试列表
     */
    List<Exam> getExamList(Exam exam);

    boolean removeByBoardSn(Long boardSn);

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
