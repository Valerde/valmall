package com.sovava.cart.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebConfig implements WebMvcConfigurer {
    /**
     * 视图映射
     *
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        /**
         * Request method 'POST' not supported
         * post请求->发到registercontroller-》转发/重定向-》视图映射->默认get方式
         */

        registry.addViewController("/cartListPage").setViewName("cartList");
        registry.addViewController("/cartListOldPage").setViewName("cartListOld");
        registry.addViewController("/successPage").setViewName("success");
        registry.addViewController("/successOldPage").setViewName("successOld");

    }
}