package com.sovava.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 *  整合mybatis-plus
 * 1. 导入依赖
 *
 * 2. 配置
 *      配置数据源
 *          导入数据库驱动 ，在common中
 *          在application.yml 中配置数据源相关配置
 *      配置mybatis-plus
 *          使用@MapperScan("com.sovava.product.dao")
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
