package com.sovava.product.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@Slf4j
public class MySessionConfig {


    /**
     * 解决子域共享问题
     * @return
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        log.debug("哈哈 ，输入法改造完成了");
        DefaultCookieSerializer defaultCookieSerializer = new DefaultCookieSerializer();
        defaultCookieSerializer.setCookieName("VALSESSION");
        defaultCookieSerializer.setDomainName("valmall.com");
        return defaultCookieSerializer;
    }

    /**
     * 解决序列化器
     *
     * @return
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
}
