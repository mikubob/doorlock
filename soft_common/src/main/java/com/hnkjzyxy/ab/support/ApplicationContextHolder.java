package com.hnkjzyxy.ab.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文持有者
 * 提供静态方式从 Spring 容器中获取 Bean
 *
 * @version 1.0
 * @author Spell a
 * @date 2023-10-01 16:35
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    /**
     * 根据 bean name 获取实例
     *
     * @param beanName bean 名称
     * @return Bean 实例，参数为空或上下文未初始化时返回 null
     */
    public static Object getBeanByName(String beanName) {
        if (beanName == null || applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(beanName);
    }

    /**
     * 根据类型获取实例
     * 只适合一个 class 只被定义一次的 bean（也就是说，根据 class 不能匹配出多个该 class 的实例）
     *
     * @param clazz Bean 类型
     * @return Bean 实例，参数为空或上下文未初始化时返回 null
     */
    public static Object getBeanByType(Class clazz) {
        if (clazz == null || applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(clazz);
    }

    /**
     * 获取容器中所有 Bean 的名称
     *
     * @return Bean 名称数组
     */
    public static String[] getBeanDefinitionNames() {
        return applicationContext.getBeanDefinitionNames();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.applicationContext = applicationContext;
    }

}