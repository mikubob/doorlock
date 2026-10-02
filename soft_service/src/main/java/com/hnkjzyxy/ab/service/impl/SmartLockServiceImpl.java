package com.hnkjzyxy.ab.service.impl;


import com.hnkjzyxy.ab.mapper.SmartLockMapper;
import com.hnkjzyxy.ab.model.LockInfo;
import com.hnkjzyxy.ab.service.SmartLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能锁Service实现类
 */
@Service
public class SmartLockServiceImpl implements SmartLockService {

    /**
     * 智能门锁数据访问接口
     */
    @Autowired
    private SmartLockMapper smartLockMapper;


    /**
     * {@inheritDoc}
     */
    @Override
    public LockInfo getByBoardSn(String boardSn) {
        return smartLockMapper.selectByBoardSn(boardSn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LockInfo getById(Integer lockId) {
        return smartLockMapper.selectById(lockId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LockInfo> getAll() {
        return smartLockMapper.selectAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LockInfo getByClassroom(String classroomNumber) {
        return smartLockMapper.selectByClassroom(classroomNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(LockInfo lockInfo) {
        return smartLockMapper.insert(lockInfo) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(LockInfo lockInfo) {
        return smartLockMapper.update(lockInfo) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateSwitchStatus(Integer lockId, Integer switchStatus) {
        return smartLockMapper.updateSwitchStatus(lockId, switchStatus) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteById(Integer lockId) {
        return smartLockMapper.deleteById(lockId) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countNum() {
        return smartLockMapper.countNum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addLockInfoAll(LockInfo lockInfo) {
        return smartLockMapper.addLockInfoAll(lockInfo)>0;
    }




}
