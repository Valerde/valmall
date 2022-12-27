package com.sovava.deckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRedissonConfig {
    /**
     * 所有对redisson的使用都是通过使用RedissonClient
     *
     * @return
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() {
        Config config = new Config();
        //创建配置 Redis url should start with redis:// or rediss:// (for SSL connection
        config.useSingleServer().setAddress("redis://192.168.37.129:6379");
        //根据config创建出redissonClient实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
