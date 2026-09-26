package com.hnkjzyxy.ab.service;


import com.hnkjzyxy.ab.model.SwitchRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface SwitchRecordService {

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

    int deleteById(Integer switchId);

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

    SwitchRecord selectById(Integer switchId);

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

    List<SwitchRecord> selectByLockId(Integer lockId);

    /**
     * 根据用户ID查询开关记录
     *
     * @param userId 用户ID
     * @return 开关记录列表
     */

    List<SwitchRecord> selectByUserId(Integer userId);

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

    int countByLockId(Integer lockId);

    /**
     * 统计某个用户的操作次数
     *
     * @param userId 用户ID
     * @return 操作次数
     */

    int countByUserId(Integer userId);
}