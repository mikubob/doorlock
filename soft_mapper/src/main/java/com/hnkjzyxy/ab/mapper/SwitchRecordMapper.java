package com.hnkjzyxy.ab.mapper;


import com.hnkjzyxy.ab.model.SwitchRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能锁开关操作记录 Mapper
 * 对应数据库表 sys_operation_log，主键列名为 switch_id
 */

@Mapper
public interface SwitchRecordMapper {

    /**
     * 插入开关记录
     *
     * @param record 开关记录实体
     * @return 影响的行数
     */
    @Insert("INSERT INTO sys_operation_log(lock_id, user_id, operation_method, operation_time) " +
            "VALUES(#{lockId}, #{userId}, #{operationMethod}, #{operationTime})")
    @Options(useGeneratedKeys = true, keyProperty = "switchId", keyColumn = "switch_id")
    int insert(SwitchRecord record);

    /**
     * 根据ID删除开关记录
     *
     * @param switchId 开关记录ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM sys_operation_log WHERE switch_id = #{switchId}")
    int deleteById(Integer switchId);

    /**
     * 更新开关记录
     *
     * @param record 开关记录实体
     * @return 影响的行数
     */
    @Update("UPDATE sys_operation_log SET lock_id = #{lockId}, user_id = #{userId}, " +
            "operation_method = #{operationMethod}, operation_time = #{operationTime} " +
            "WHERE switch_id = #{switchId}")
    int update(SwitchRecord record);

    /**
     * 根据ID查询开关记录
     *
     * @param switchId 开关记录ID
     * @return 开关记录实体
     */
    @Select("SELECT * FROM sys_operation_log WHERE switch_id = #{switchId}")
    @Results({
            @Result(property = "switchId", column = "switch_id"),
            @Result(property = "lockId", column = "lock_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "operationMethod", column = "operation_method"),
            @Result(property = "operationTime", column = "operation_time")
    })
    SwitchRecord selectById(Integer switchId);

    /**
     * 查询所有开关记录
     *
     * @return 开关记录列表
     */
    @Select("SELECT * FROM sys_operation_log")
    @Results({
            @Result(property = "switchId", column = "switch_id"),
            @Result(property = "lockId", column = "lock_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "operationMethod", column = "operation_method"),
            @Result(property = "operationTime", column = "operation_time")
    })
    List<SwitchRecord> selectAll();

    /**
     * 根据锁ID查询开关记录
     *
     * @param lockId 锁ID
     * @return 开关记录列表
     */
    @Select("SELECT * FROM sys_operation_log WHERE lock_id = #{lockId}")
    @Results({
            @Result(property = "switchId", column = "switch_id"),
            @Result(property = "lockId", column = "lock_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "operationMethod", column = "operation_method"),
            @Result(property = "operationTime", column = "operation_time")
    })
    List<SwitchRecord> selectByLockId(Integer lockId);

    /**
     * 根据用户ID查询开关记录
     * <p>
     * 注意：sys_operation_log.user_id 在库中是 text 类型，显式按字符串比较。
     *
     * @param userId 用户ID
     * @return 开关记录列表
     */
    @Select("SELECT * FROM sys_operation_log WHERE user_id = CAST(#{userId} AS CHAR)")
    @Results({
            @Result(property = "switchId", column = "switch_id"),
            @Result(property = "lockId", column = "lock_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "operationMethod", column = "operation_method"),
            @Result(property = "operationTime", column = "operation_time")
    })
    List<SwitchRecord> selectByUserId(Integer userId);

    /**
     * 根据时间范围查询开关记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 开关记录列表
     */
    @Select("SELECT * FROM sys_operation_log WHERE operation_time BETWEEN #{startTime} AND #{endTime}")
    @Results({
            @Result(property = "switchId", column = "switch_id"),
            @Result(property = "lockId", column = "lock_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "operationMethod", column = "operation_method"),
            @Result(property = "operationTime", column = "operation_time")
    })
    List<SwitchRecord> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 统计某个锁的操作次数
     *
     * @param lockId 锁ID
     * @return 操作次数
     */
    @Select("SELECT COUNT(*) FROM sys_operation_log WHERE lock_id = #{lockId}")
    int countByLockId(Integer lockId);

    /**
     * 统计某个用户的操作次数
     * <p>
     * 注意：sys_operation_log.user_id 在库中是 text 类型，直接与整型参数比较会走隐式转换，
     * 这里显式按字符串比较，避免无法命中索引。
     *
     * @param userId 用户ID
     * @return 操作次数
     */
    @Select("SELECT COUNT(*) FROM sys_operation_log WHERE user_id = CAST(#{userId} AS CHAR)")
    int countByUserId(Integer userId);
}