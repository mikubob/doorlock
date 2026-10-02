package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.MajorDetails;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
    @Select("select * from sys_major_details where del_flag = 1")
    List<MajorDetails> selectMajorDetails();

    /**
     * 更新专业已录取人数
     *
     * @param id 专业录取名额ID
     * @param updated 更新后的已录取人数
     * @return 受影响的记录数量
     */
    @Update("UPDATE sys_major_details SET accepted = accepted + #{updated} WHERE id = #{id} AND del_flag = 1")
    int updateAccepted(Long id, int updated);
}
