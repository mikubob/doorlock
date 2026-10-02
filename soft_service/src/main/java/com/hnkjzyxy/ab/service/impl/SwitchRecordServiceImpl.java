package com.hnkjzyxy.ab.service.impl;

import com.hnkjzyxy.ab.mapper.SwitchRecordMapper;
import com.hnkjzyxy.ab.model.SwitchRecord;
import com.hnkjzyxy.ab.service.SwitchRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开关锁记录Service实现类
 */
@Service
public class SwitchRecordServiceImpl implements SwitchRecordService {
    /**
     * 开关锁记录数据访问接口
     */
    @Autowired
    private SwitchRecordMapper switchRecordMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public int insert(SwitchRecord record) {
        return switchRecordMapper.insert(record);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int deleteById(Integer switchId) {
        return switchRecordMapper.deleteById(switchId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(SwitchRecord record) {
        return switchRecordMapper.update(record);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SwitchRecord selectById(Integer switchId) {
        return switchRecordMapper.selectById(switchId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SwitchRecord> selectAll() {
        return switchRecordMapper.selectAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SwitchRecord> selectByLockId(Integer lockId) {
        return switchRecordMapper.selectByLockId(lockId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SwitchRecord> selectByUserId(Integer userId) {
        return switchRecordMapper.selectByUserId(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SwitchRecord> selectByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return switchRecordMapper.selectByTimeRange(startTime, endTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countByLockId(Integer lockId) {
        return switchRecordMapper.countByLockId(lockId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countByUserId(Integer userId) {
        return switchRecordMapper.countByUserId(userId);
    }
}
