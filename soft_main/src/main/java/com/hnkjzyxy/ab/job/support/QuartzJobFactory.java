package com.hnkjzyxy.ab.job.support;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.stereotype.Component;

/**
 * 支持 Spring 依赖注入的 Quartz Job 实例工厂
 */
@Component("quartzJobFactory")
public class QuartzJobFactory extends AdaptableJobFactory {

    /**
     * Spring 自动依赖注入工厂
     */
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    /**
     * 重写创建Job实例的方法（注意参数类型为org.quartz.spi.TriggerFiredBundle）
     *
     * @param bundle Quartz 触发时提供的任务及执行信息
     * @return 完成 Spring 依赖注入的 Job 实例
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        // 先通过父类创建Job实例
        Object jobInstance = super.createJobInstance(bundle);
        // 再将实例交由Spring容器进行依赖注入
        autowireCapableBeanFactory.autowireBean(jobInstance);
        return jobInstance;
    }
}