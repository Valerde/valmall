package com.sovava.cart.config;

import com.sovava.cart.interceptor.CartInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
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
        registry.addViewController("/successPage").setViewName("success");

    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        CartInterceptor cartInterceptor = new CartInterceptor();
        registry.addInterceptor(cartInterceptor).addPathPatterns("/**");
    }
}
