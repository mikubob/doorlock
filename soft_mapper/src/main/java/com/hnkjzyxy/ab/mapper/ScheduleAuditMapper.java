package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.ScheduleAudit;
import com.hnkjzyxy.ab.vo.ExamRecordQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 排程审计数据访问接口。
 */
@Mapper
public interface ScheduleAuditMapper extends BaseMapper<ScheduleAudit> {
    /**
     * 重新核对排程前关闭上一版本问题，当前问题随后原子重新登记。
     * @return 关闭行数
     */
    int resolveOpen();

    /**
     * 查询考试操作记录总数，不混入课表冲突问题。
     * @param query 查询条件
     * @return 记录数
     */
    long countExamRecords(@Param("query") ExamRecordQuery query);

    /**
     * 按操作时间和主键倒序查询考试操作页。
     * @param query 查询条件
     * @param offset 分页偏移量
     * @return 操作记录
     */
    List<ScheduleAudit> pageExamRecords(@Param("query") ExamRecordQuery query,
            @Param("offset") long offset);
}
