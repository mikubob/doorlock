package com.hnkjzyxy.ab.service.impl;

import com.hnkjzyxy.ab.mapper.ScheduleMapper;
import com.hnkjzyxy.ab.model.ScheduleTask;
import com.hnkjzyxy.ab.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    @Autowired
    private ScheduleMapper scheduleMapper;

    @Override
    public List<ScheduleTask> getAll() {
        return scheduleMapper.getAll();
    }

    @Override
    public List<ScheduleTask> getByTime(LocalDateTime startTime, LocalDateTime endTime) {
        return scheduleMapper.getByTime(startTime, endTime);
    }


    @Override
    public int insert(ScheduleTask scheduleTask) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        Date now = new Date();
        String nowTime = dateFormat.format(now);
        String Details = nowTime + " " + scheduleTask.getHour() + "时:" + scheduleTask.getMinute() + "分" + "开始执行";
        if (scheduleTask.getIsLoop() == 1) {
            Details += " 循环执行";
        } else {
            char[] charArray = scheduleTask.getCountDay().toCharArray();
            Details += " 单次执行,执行周为";
            for (int i = 0; i < charArray.length; i++) {
                switch (charArray[i]) {
                    case '0':
                        Details += " 周一、";
                        break;
                    case '1':
                        Details += " 周二、";
                        break;
                    case '2':
                        Details += " 周三、";
                        break;
                    case '3':
                        Details += " 周四、";
                        break;
                    case '4':
                        Details += " 周五";
                        break;
                    case '5':
                        Details += " 周六";
                        break;
                    case '6':
                        Details += " 周七";
                        break;
                }
            }
        }
        if (scheduleTask.getTimedOperation() == 1) {
            Details += " 执行操作为：开锁";
        }
        scheduleTask.setTaskDetails(Details);
        if (scheduleMapper.insert(scheduleTask) > 0) {
            return 1;
        }
        return 0;
    }

    @Override
    public int update(ScheduleTask scheduleTask) {
        return scheduleMapper.update(scheduleTask);
    }


    @Override
    public int delete(Integer id) {
        if (scheduleMapper.delete(id) > 0) {
            return 1;
        }
        return 0;
    }

    @Override
    public int updateStatus(ScheduleTask scheduleTask) {
        if (scheduleMapper.updateStatus(scheduleTask.getTaskId(), scheduleTask.getTaskStatus(), scheduleTask.getLoopCount()) > 0) {
            return 1;
        }
        return 0;
    }

    @Override
    public ScheduleTask getById(int id) {
        return scheduleMapper.getById(id);
    }

    @Override
    public List<ScheduleTask> getByUserId(int userId) {
        return scheduleMapper.getByUserId(userId);
    }

    @Override
    public int updateLoopCount(int loopCount, int taskId) {
        if (scheduleMapper.updateLoopCount(loopCount, taskId) > 0) {
            return 1;
        }
        return 0;
    }
}
