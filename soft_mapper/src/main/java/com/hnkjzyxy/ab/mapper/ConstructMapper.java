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
 * 建设项目数据访问接口
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/5/8 15:37
 */
//@CacheNamespace(implementation = MybatisRedisCache.class)
public interface ConstructMapper extends BaseMapper<Construct> {

    /**
     * 新增建设项目与参与人的关联记录
     *
     * @param conId 建设项目ID
     * @param uIds 用户ID集合
     */
    @Insert("insert into sys_construct_user(con_id, u_id) VALUES(#{conId},#{uIds})")
    void addConstructByUId(@Param("conId") Integer conId, @Param("uIds") String uIds);

    /**
     * 查询用户参与的建设项目ID
     *
     * @param userId 用户ID
     * @return 查询结果列表
     */
    @Select("select con_id from sys_construct_user where find_in_set(#{userId},u_id)")
    List<Integer> getConIds(@Param("userId") Integer userId);

    /**
     * 查询用户创建的建设项目年度
     *
     * @param userId 用户ID
     * @return 查询结果列表
     */
    @Select("select year(create_time) as year from sys_construct where u_id = #{userId} and p_id = 0 order by year desc")
    HashSet<String> getConstructYears(@Param("userId") Integer userId);

    /**
     * 查询用户参与的建设项目年度
     *
     * @param wrapper 数据库查询条件
     * @return 查询结果列表
     */
    @Select("select year(create_time) as year from sys_construct ${ew.customSqlSegment}")
    HashSet<String> getConstructYearsByUser(@Param("ew") QueryWrapper<Construct> wrapper);

    /**
     * 统计建设项目参与人关联记录数量
     *
     * @param id 建设项目ID
     * @return 建设项目参与人关联记录数量
     */
    @Select("select count(*) from sys_construct_user where con_id = #{id}")
    Integer getConstructById(Integer id);

    /**
     * 更新建设项目的参与人ID集合
     *
     * @param id 建设项目ID
     * @param replace 新的参与人ID字符串
     */
    @Update("update sys_construct_user set u_id = #{uIds} where con_id = #{conId}")
    void updateConstructByUId(@Param("conId") Integer id, @Param("uIds") String replace);

    /**
     * 查询建设项目的参与人ID字符串
     *
     * @param id 建设项目ID
     * @return 查询得到的文本信息
     */
    @Select("select u_id from sys_construct_user where con_id = #{conId}")
    String getConstructUIdsById(@Param("conId") Integer id);

}
