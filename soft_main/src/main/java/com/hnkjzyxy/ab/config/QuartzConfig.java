package com.hnkjzyxy.ab.config;

import com.hnkjzyxy.ab.job.support.QuartzJobFactory;

import com.hnkjzyxy.ab.job.SmartLockJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {

    @Autowired
    private QuartzJobFactory quartzJobFactory; // 注入自定义JobFactory

    /**
     * 创建 JobDetail
     *
     * @param lockId  锁ID
     * @param userId  用户ID
     * @param taskId  任务ID
     * @param Channel 开锁通道
     * @return 任务明细
     */
    public JobDetail createJobDetail(int lockId, Integer userId, int taskId,String Channel) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("lockId", lockId);
        dataMap.put("userId", userId);
        dataMap.put("taskId", taskId);
        dataMap.put("Channel", Channel);
        return JobBuilder.newJob(SmartLockJob.class)
                .withIdentity("smartLockJob_" + lockId, "smartLockGroup")
                .setJobData(dataMap)
                .storeDurably()
                .build();
    }

    /**
     * 创建 Trigger，使用 SimpleTrigger（仅执行一次）
     *
     * @param jobDetail 任务明细
     * @return 触发器
     */
    public Trigger createTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName() + "_trigger", "smartLockTriggerGroup")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withRepeatCount(0)) // 只执行一次
                .build();
    }

    /**
     * 创建 CronTrigger（支持每周特定天数和时间，带重复次数控制）
     *
     * @param jobDetail      任务明细
     * @param cronExpression Cron 表达式
     * @param repeatCount    重复次数，为 null 或小于 0 时不限制
     * @return 触发器
     */
    public Trigger createCronTrigger(JobDetail jobDetail, String cronExpression, Integer repeatCount) {
        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        // 如果设置了重复次数（>=0），存储到JobDataMap中
        if (repeatCount != null && repeatCount >= 0) {
            jobDataMap.put("maxExecutionCount", repeatCount);
            jobDataMap.put("currentExecutionCount", 0);
        }

        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName() + "_cron_trigger", "smartLockTriggerGroup")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    /**
     * 创建 CronTrigger 的重载方法：不指定重复次数时默认无限重复
     *
     * @param jobDetail      任务明细
     * @param cronExpression Cron 表达式
     * @return 触发器
     */
    public Trigger createCronTrigger(JobDetail jobDetail, String cronExpression) {
        return createCronTrigger(jobDetail, cronExpression, null);
    }

    /**
     * 生成每周特定天数和时间的 Cron 表达式
     *
     * @param hour       小时（0-23）
     * @param minute     分钟（0-59）
     * @param daysOfWeek 星期数组（0-6 代表周一到周日）
     * @return Cron 表达式
     */
    public String generateWeeklyCronExpression(int hour, int minute, int[] daysOfWeek) {
        // 前端传递0-6代表周一到周日，转换为Quartz的2-7,1
        // 映射关系：
        // 前端0(周一) → Quartz 2
        // 前端1(周二) → Quartz 3
        // 前端2(周三) → Quartz 4
        // 前端3(周四) → Quartz 5
        // 前端4(周五) → Quartz 6
        // 前端5(周六) → Quartz 7
        // 前端6(周日) → Quartz 1
        StringBuilder days = new StringBuilder();
        for (int day : daysOfWeek) {
            int quartzDay;
            if (day == 6) {
                quartzDay = 1; // 周日对应Quartz的1
            } else {
                quartzDay = day + 2; // 周一到周六对应Quartz的2-7
            }
            days.append(quartzDay).append(",");
        }
        if (days.length() > 0) {
            days.deleteCharAt(days.length() - 1); // 移除最后一个逗号
        }

        // 确保时分在有效范围内
        hour = Math.max(0, Math.min(23, hour));
        minute = Math.max(0, Math.min(59, minute));

        return String.format("0 %d %d ? * %s", minute, hour, days.toString());
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setJobFactory(quartzJobFactory); // 设置自定义JobFactory
        return schedulerFactoryBean;
    }
}
