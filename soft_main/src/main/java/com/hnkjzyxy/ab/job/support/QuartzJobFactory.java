package com.hnkjzyxy.ab.job.support;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.stereotype.Component;

@Component("quartzJobFactory")
public class QuartzJobFactory extends AdaptableJobFactory {

    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    /**
     * 重写创建Job实例的方法（注意参数类型为org.quartz.spi.TriggerFiredBundle）
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