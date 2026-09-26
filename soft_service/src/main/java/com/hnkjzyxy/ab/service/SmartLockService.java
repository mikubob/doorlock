package com.hnkjzyxy.ab.service;


import com.hnkjzyxy.ab.model.LockInfo;

import java.util.List;

/**
 * 智能锁Service接口
 */
public interface SmartLockService {
    /**
     * 根据ID查询智能锁信息
     *
     * @param lockId 智能锁唯一标识符
     * @return 智能锁信息
     */
    LockInfo getById(Integer lockId);

    /**
     * 查询所有智能锁信息
     *
     * @return 智能锁列表
     */
    List<LockInfo> getAll();



    /**
     * 根据教室编号查询智能锁
     *
     * @param classroomNumber 教室编号
     * @return 智能锁信息
     */
    LockInfo getByClassroom(String classroomNumber);

    /**
     * 新增智能锁信息
     *
     * @param lockInfo 智能锁对象
     * @return 是否成功
     */
    boolean add(LockInfo lockInfo);

    /**
     * 更新智能锁信息
     *
     * @param lockInfo 智能锁对象
     * @return 是否成功
     */
    boolean update(LockInfo lockInfo);

    /**
     * 更新智能锁开关状态
     *
     * @param lockId       智能锁ID
     * @param switchStatus 开关状态
     * @return 是否成功
     */
    boolean updateSwitchStatus(Integer lockId, Integer switchStatus);

    /**
     * 根据ID删除智能锁信息
     *
     * @param lockId 智能锁唯一标识符
     * @return 是否成功
     */
    boolean deleteById(Integer lockId);

    int countNum();

    boolean addLockInfoAll(LockInfo lockInfo);


    LockInfo getByBoardSn(String boardSn);
}
