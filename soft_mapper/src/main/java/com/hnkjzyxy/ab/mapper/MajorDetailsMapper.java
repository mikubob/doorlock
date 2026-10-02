package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.MajorDetails;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (MajorDetails)表数据库访问层
 *
 * @author Binc
 * @since 2025-04-14 13:31:28
 */
@Mapper
public interface MajorDetailsMapper extends BaseMapper<MajorDetails> {
    /**
     * 查询专业录取名额列表
     *
     * @return 专业录取名额列表
     */
    default List<MajorDetails> selectMajorDetails() {
        return selectList(new LambdaQueryWrapper<MajorDetails>());
    }

    /**
     * 按增量更新有效专业的已录取人数
     *
     * @param id 专业录取名额ID
     * @param updated 已录取人数的增量，负数表示减少
     * @return 受影响的记录数量
     */
    int updateAccepted(@Param("id") Long id, @Param("updated") int updated);
}
