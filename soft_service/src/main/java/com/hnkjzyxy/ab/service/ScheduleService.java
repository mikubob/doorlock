package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.model.ScheduleTask;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleService {
    public List<ScheduleTask> getAll();

    public List<ScheduleTask> getByTime(LocalDateTime startTime, LocalDateTime endTime);

    public int insert(ScheduleTask scheduleTask);

    public int update(ScheduleTask scheduleTask);

    public int delete(Integer id);

    public int updateStatus(ScheduleTask scheduleTask);

    public ScheduleTask getById(int id);

    public List<ScheduleTask> getByUserId(int userId);

    public int updateLoopCount(int loopCount, int taskId);
}
