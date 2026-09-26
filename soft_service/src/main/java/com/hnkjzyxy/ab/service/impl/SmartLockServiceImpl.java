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

    @Autowired
    private SmartLockMapper smartLockMapper;


    @Override
    public LockInfo getByBoardSn(String boardSn) {
        return smartLockMapper.selectByBoardSn(boardSn);
    }

    @Override
    public LockInfo getById(Integer lockId) {
        return smartLockMapper.selectById(lockId);
    }

    @Override
    public List<LockInfo> getAll() {
        return smartLockMapper.selectAll();
    }

    @Override
    public LockInfo getByClassroom(String classroomNumber) {
        return smartLockMapper.selectByClassroom(classroomNumber);
    }

    @Override
    public boolean add(LockInfo lockInfo) {
        return smartLockMapper.insert(lockInfo) > 0;
    }

    @Override
    public boolean update(LockInfo lockInfo) {
        return smartLockMapper.update(lockInfo) > 0;
    }

    @Override
    public boolean updateSwitchStatus(Integer lockId, Integer switchStatus) {
        return smartLockMapper.updateSwitchStatus(lockId, switchStatus) > 0;
    }

    @Override
    public boolean deleteById(Integer lockId) {
        return smartLockMapper.deleteById(lockId) > 0;
    }

    @Override
    public int countNum() {
        return smartLockMapper.countNum();
    }

    @Override
    public boolean addLockInfoAll(LockInfo lockInfo) {
        return smartLockMapper.addLockInfoAll(lockInfo)>0;
    }




}
