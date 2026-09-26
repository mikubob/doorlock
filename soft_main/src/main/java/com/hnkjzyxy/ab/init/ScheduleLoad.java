package com.hnkjzyxy.ab.init;

import com.hnkjzyxy.ab.config.QuartzConfig;
import com.hnkjzyxy.ab.model.ScheduleTask;
import com.hnkjzyxy.ab.service.ScheduleService;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduleLoad implements CommandLineRunner {
    @Autowired
    ScheduleService scheduleService;
    @Autowired
    private QuartzConfig quartzConfig;
    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;
    @Override
    public void run(String... args) throws Exception {
        List<ScheduleTask> all = scheduleService.getAll();
        for(ScheduleTask scheduleTask:all){
            if(scheduleTask.getTaskStatus()==1){
                char[] charArray = scheduleTask.getCountDay().toCharArray();
                int [] Days=new int[charArray.length];
                for (int i = 0; i < charArray.length; i++){
                    if(charArray[i] >= '0' && charArray[i] <= '9') {
                        Days[i] = Integer.parseInt(String.valueOf(charArray[i]));
                    }
                }
                // 生成Cron表达式
                String cronExpression = quartzConfig.generateWeeklyCronExpression(
                        scheduleTask.getHour(),
                        scheduleTask.getMinute(),
                        Days
                );

                // 创建任务和触发器
                JobDetail jobDetail = quartzConfig.createJobDetail(scheduleTask.getLockId(), scheduleTask.getUserId(),scheduleTask.getTaskId(),scheduleTask.getRemarks());
                Trigger trigger;
                if (scheduleTask.getLoopCount()>0){
                    trigger = quartzConfig.createCronTrigger(jobDetail, cronExpression, scheduleTask.getLoopCount());
                }else{
                    trigger= quartzConfig.createCronTrigger(jobDetail, cronExpression);
                }
                // 获取调度器并安排任务
                Scheduler scheduler = schedulerFactoryBean.getScheduler();

                // 先删除可能存在的同名任务
                if (scheduler.checkExists(jobDetail.getKey())) {
                    scheduler.deleteJob(jobDetail.getKey());
                }

                scheduler.scheduleJob(jobDetail, trigger);
                System.out.println(jobDetail.getKey()+"任务已安排");
            }

        }
    }
}
