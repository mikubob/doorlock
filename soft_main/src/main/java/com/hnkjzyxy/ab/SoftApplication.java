package com.hnkjzyxy.ab;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author 16702
 */

@SpringBootApplication
@MapperScan("com.hnkjzyxy.ab.mapper")
@ConfigurationPropertiesScan("com.hnkjzyxy.ab")
@EnableScheduling
//@EnableCaching
@EnableTransactionManagement
public class SoftApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoftApplication.class);
    }
}
