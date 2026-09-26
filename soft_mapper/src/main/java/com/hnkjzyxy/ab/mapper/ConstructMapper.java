package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Construct;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.HashSet;
import java.util.List;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/5/8 15:37
 */
//@CacheNamespace(implementation = RedisCacheConfig.class)
public interface ConstructMapper extends BaseMapper<Construct> {

    @Insert("insert into sys_construct_user(con_id, u_id) VALUES(#{conId},#{uIds})")
    void addConstructByUId(@Param("conId") Integer conId, @Param("uIds") String uIds);

    @Select("select con_id from sys_construct_user where find_in_set(#{userId},u_id)")
    List<Integer> getConIds(@Param("userId") Integer userId);

    @Select("select year(create_time) as year from sys_construct where u_id = #{userId} and p_id = 0 order by year desc")
    HashSet<String> getConstructYears(@Param("userId") Integer userId);

    @Select("select year(create_time) as year from sys_construct ${ew.customSqlSegment}")
    HashSet<String> getConstructYearsByUser(@Param("ew") QueryWrapper<Construct> wrapper);

    @Select("select count(*) from sys_construct_user where con_id = #{id}")
    Integer getConstructById(Integer id);

    @Update("update sys_construct_user set u_id = #{uIds} where con_id = #{conId}")
    void updateConstructByUId(@Param("conId") Integer id, @Param("uIds") String replace);

    @Select("select u_id from sys_construct_user where con_id = #{conId}")
    String getConstructUIdsById(@Param("conId") Integer id);

}
