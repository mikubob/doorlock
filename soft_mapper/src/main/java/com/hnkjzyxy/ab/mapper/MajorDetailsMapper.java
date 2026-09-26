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
    @Select("select * from sys_major_details where del_flag = 1")
    List<MajorDetails> selectMajorDetails();

    @Update("UPDATE sys_major_details SET accepted = accepted + #{updated} WHERE id = #{id} AND del_flag = 1")
    int updateAccepted(Long id, int updated);
}
