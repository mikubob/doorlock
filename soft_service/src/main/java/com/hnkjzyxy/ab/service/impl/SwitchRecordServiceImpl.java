package com.hnkjzyxy.ab.service.impl;

import com.hnkjzyxy.ab.mapper.SwitchRecordMapper;
import com.hnkjzyxy.ab.model.SwitchRecord;
import com.hnkjzyxy.ab.service.SwitchRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SwitchRecordServiceImpl implements SwitchRecordService {
    @Autowired
    private SwitchRecordMapper switchRecordMapper;

    @Override
    public int insert(SwitchRecord record) {
        return switchRecordMapper.insert(record);
    }

    @Override
    public int deleteById(Integer switchId) {
        return switchRecordMapper.deleteById(switchId);
    }

    @Override
    public int update(SwitchRecord record) {
        return switchRecordMapper.update(record);
    }

    @Override
    public SwitchRecord selectById(Integer switchId) {
        return switchRecordMapper.selectById(switchId);
    }

    @Override
    public List<SwitchRecord> selectAll() {
        return switchRecordMapper.selectAll();
    }

    @Override
    public List<SwitchRecord> selectByLockId(Integer lockId) {
        return switchRecordMapper.selectByLockId(lockId);
    }

    @Override
    public List<SwitchRecord> selectByUserId(Integer userId) {
        return switchRecordMapper.selectByUserId(userId);
    }

    @Override
    public List<SwitchRecord> selectByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return switchRecordMapper.selectByTimeRange(startTime, endTime);
    }

    @Override
    public int countByLockId(Integer lockId) {
        return switchRecordMapper.countByLockId(lockId);
    }

    @Override
    public int countByUserId(Integer userId) {
        return switchRecordMapper.countByUserId(userId);
    }
}
