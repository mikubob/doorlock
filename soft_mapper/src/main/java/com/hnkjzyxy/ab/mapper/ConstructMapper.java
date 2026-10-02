package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Construct;
import org.apache.ibatis.annotations.Param;

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
public interface ConstructMapper extends BaseMapper<Construct> {

    /**
     * 新增建设项目与参与人的关联记录
     *
     * @param conId 建设项目ID
     * @param uIds 以逗号分隔的参与人ID字符串
     */
    void addConstructByUId(@Param("conId") Integer conId, @Param("uIds") String uIds);

    /**
     * 查询用户参与的建设项目ID
     *
     * @param userId 用户ID
     * @return 用户参与的建设项目ID列表
     */
    List<Integer> getConIds(@Param("userId") Integer userId);

    /**
     * 查询用户创建的建设项目年度
     *
     * @param userId 用户ID
     * @return 用户创建的顶层建设项目年度集合
     */
    HashSet<String> getConstructYears(@Param("userId") Integer userId);

    /**
     * 查询用户参与的建设项目年度
     *
     * @param wrapper 数据库查询条件
     * @return 满足查询条件的建设项目年度集合
     */
    HashSet<String> getConstructYearsByUser(@Param("ew") QueryWrapper<Construct> wrapper);

    /**
     * 统计建设项目参与人关联记录数量
     *
     * @param id 建设项目ID
     * @return 建设项目参与人关联记录数量
     */
    Integer getConstructById(@Param("id") Integer id);

    /**
     * 更新建设项目的参与人ID集合
     *
     * @param id 建设项目ID
     * @param replace 新的参与人ID字符串
     */
    void updateConstructByUId(@Param("conId") Integer id, @Param("uIds") String replace);

    /**
     * 查询建设项目的参与人ID字符串
     *
     * @param id 建设项目ID
     * @return 以逗号分隔的参与人ID字符串，不存在时返回 null
     */
    String getConstructUIdsById(@Param("conId") Integer id);

}
