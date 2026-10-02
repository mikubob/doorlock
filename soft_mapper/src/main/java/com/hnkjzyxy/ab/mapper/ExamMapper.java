package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Exam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考试信息表数据库访问层
 */
@Mapper
public interface ExamMapper extends BaseMapper<Exam> {

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
     * @return 至少删除一条考试安排时返回 true
     */
    boolean removeByBoardSn(@Param("boardSn") Long boardSn);
}
