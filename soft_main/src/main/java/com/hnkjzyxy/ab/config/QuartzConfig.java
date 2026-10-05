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

/**
 * 门禁 Quartz 调度配置，创建任务、触发器和调度器
 */
@Configuration
public class QuartzConfig {

    /**
     * 支持依赖注入的 Quartz Job 工厂
     */
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
                .withIdentity("smartLockJob_" + taskId, "smartLockGroup")
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
                        .inTimeZone(java.util.TimeZone.getTimeZone("Asia/Shanghai"))
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
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || daysOfWeek == null || daysOfWeek.length == 0 || daysOfWeek.length > 7) throw new IllegalArgumentException("星期或时间参数错误");
        java.util.Set<Integer> days = new java.util.TreeSet<>();
        for (int day : daysOfWeek) {
            if (day < 0 || day > 6 || !days.add(day == 6 ? 1 : day + 2)) throw new IllegalArgumentException("星期参数重复或超限");
        }
        String expression = days.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        return String.format("0 %d %d ? * %s", minute, hour, expression);
    }

    /**
     * 创建使用自定义 Job 工厂的 Quartz 调度器
     *
     * @return Quartz 调度器工厂
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setJobFactory(quartzJobFactory); // 设置自定义JobFactory
        return schedulerFactoryBean;
    }
}
