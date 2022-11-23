package com.sovava.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {

            @Override
            public void apply(RequestTemplate requestTemplate) {

                //利用RequestContextHolder上下文保持器获取 , ** 但是异步模式会丢失上下文环境
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (requestAttributes == null) return;
                HttpServletRequest request = requestAttributes.getRequest();//老请求
                //同步请求头数据cookie

                log.debug("feign远程调用执行之前加入请求头:{},{}", "Cookie", request.getHeader("Cookie"));
                requestTemplate.header("Cookie", request.getHeader("Cookie"));
            }
        };
    }
}
