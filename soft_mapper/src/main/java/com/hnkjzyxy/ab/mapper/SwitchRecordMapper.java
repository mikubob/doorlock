package com.hnkjzyxy.ab.mapper;

import com.hnkjzyxy.ab.model.SwitchRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
    int insert(SwitchRecord record);

    /**
     * 根据ID删除开关记录
     *
     * @param switchId 开关记录ID
     * @return 影响的行数
     */
    int deleteById(@Param("switchId") Integer switchId);

    /**
     * 更新开关记录
     *
     * @param record 开关记录实体
     * @return 影响的行数
     */
    int update(SwitchRecord record);

    /**
     * 根据ID查询开关记录
     *
     * @param switchId 开关记录ID
     * @return 开关记录实体
     */
    SwitchRecord selectById(@Param("switchId") Integer switchId);

    /**
     * 查询所有开关记录
     *
     * @return 开关记录列表
     */
    List<SwitchRecord> selectAll();

    /**
     * 根据锁ID查询开关记录
     *
     * @param lockId 锁ID
     * @return 开关记录列表
     */
    List<SwitchRecord> selectByLockId(@Param("lockId") Integer lockId);

    /**
     * 根据用户ID查询开关记录
     * <p>
     * 注意：sys_operation_log.user_id 在库中是 text 类型，显式按字符串比较。
     *
     * @param userId 用户ID
     * @return 开关记录列表
     */
    List<SwitchRecord> selectByUserId(@Param("userId") Integer userId);

    /**
     * 根据时间范围查询开关记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 开关记录列表
     */
    List<SwitchRecord> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 统计某个锁的操作次数
     *
     * @param lockId 锁ID
     * @return 操作次数
     */
    int countByLockId(@Param("lockId") Integer lockId);

    /**
     * 统计某个用户的操作次数
     * <p>
     * 注意：sys_operation_log.user_id 在库中是 text 类型，直接与整型参数比较会走隐式转换，
     * 这里显式按字符串比较，避免无法命中索引。
     *
     * @param userId 用户ID
     * @return 操作次数
     */
    int countByUserId(@Param("userId") Integer userId);
}