package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.model.Exam;
import java.time.LocalDateTime;

/**
 * 考试生命周期规则；历史终态不按时间重新启用。
 */
public final class ExamLifecycle {
    /**
     * 工具类不允许实例化。
     */
    private ExamLifecycle() { }

    /**
     * 判断是否为终态。
     * @param status 状态码
     * @return 已结束、提前结束或取消
     */
    public static boolean terminal(Integer status) {
        return status != null && status >= 2 && status <= 4;
    }

    /**
     * 从学校时间推导普通考试状态，保留已存在的终态。
     * @param exam 考试安排
     * @param now 当前学校时刻
     * @return 生命周期状态码
     */
    public static int statusAt(Exam exam, LocalDateTime now) {
        if (terminal(exam.getStatus())) return exam.getStatus();
        if (exam.getStartTime() == null || exam.getEndTime() == null) {
            throw new IllegalArgumentException("考试时间无法确认");
        }
        return now.isBefore(exam.getStartTime()) ? 0 : now.isBefore(exam.getEndTime()) ? 1 : 2;
    }
}
