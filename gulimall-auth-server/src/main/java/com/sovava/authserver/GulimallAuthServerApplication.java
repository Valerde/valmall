package com.sovava.authserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 1）SpringSession核心原理  EnableRedisHttpSession 导入了RedisHttpSessionConfiguration
 *          1） 给容器添加了一个组件RedisIndexedSessionRepository  redis操作session Redis 操作session的增删改查操作类
 *          2） springSessionRepositoryFilter==》filter session存储的过滤器 作用：每个请求过来都必须经过filter
 *
 *
 */
@EnableRedisHttpSession //整合redis Session
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class GulimallAuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallAuthServerApplication.class, args);
    }

}
