package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.mapper.ScheduleAuditMapper;
import com.hnkjzyxy.ab.vo.ExamRecordQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 只读查询考试操作历史；快照在考试删除后仍保留。
 */
@Service
public class ExamRecordService {
    /**
     * 共用审计表访问。
     */
    private final ScheduleAuditMapper mapper;

    /**
     * 创建考试记录服务。
     * @param mapper 审计访问
     */
    public ExamRecordService(ScheduleAuditMapper mapper) { this.mapper = mapper; }

    /**
     * 查询同一数据库快照中的分页和总数。
     * @param query 分页及筛选条件
     * @return 总数和记录
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> list(ExamRecordQuery query) {
        if (query.getPage() < 1 || query.getPage() > 1000000 || query.getSize() < 1 || query.getSize() > 100) {
            throw new IllegalArgumentException("考试记录分页参数不合法");
        }
        if (query.getFrom() != null && query.getTo() != null && query.getTo().isBefore(query.getFrom())) {
            throw new IllegalArgumentException("截止时间不能早于起始时间");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", mapper.countExamRecords(query));
        result.put("records", mapper.pageExamRecords(query, ((long) query.getPage() - 1) * query.getSize()));
        return result;
    }
}
