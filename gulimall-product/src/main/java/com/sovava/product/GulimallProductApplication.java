package com.sovava.product;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;

import javax.validation.constraints.NotNull;

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
 *          告诉mybatisplus sql文件的位置
 *
 * -----------
 * 逻辑删除
 * 1. 配置全局的逻辑删除规则
 * 2. 配置逻辑删除的组件Bean(mybatisplus3以上的不需要
 * 3. 给bean加上逻辑删除的注解@TableLogic
 *
 *
 * JSR303
 * 1. 给bean添加校验注解javax.validation.constraints ， 并添加消息提示
 * 2. 为方法请求参数添加注解@Valid
 *      效果： 校验错误后会有默认的响应
 * 3. 给校验的请求参数后面紧跟一个BindingResult， 就可以获得校验的结果
 *
 *
 *
 *
 * 统一的异常处理
 * \@ControllerAdvice
 *
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class GulimallProductApplication {

//    @NotNull
    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
