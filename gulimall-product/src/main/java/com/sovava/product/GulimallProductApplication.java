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
 * 4. 分组校验功能
 *        @NotBlank(message = "品牌名必须提交", groups = {AddGroup.class, UpdateGroup.class})
 *        给校验注解标注什么分组下进行校验
 *
 *        在请求参数中更换Valid 为Validated , 并指定分组
 *        默认不分组的校验规则 groups = {}， 只有在请求字段不进行分组校验（@Valid)的情况下在会生效
 * 5. 自定义的校验规则 ：
 *      编写一个自定义的校验注解
 *      编写一个自定义的校验器
 *      关联自定义的校验器和校验注解
 *           \@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})//可以在哪里使用
 *          \@Retention(RUNTIME)
 *           \@Documented
 *           \@Constraint(validatedBy = {ListValueConstraintValidator.class [可以指定多个校验器] })
 *          public @interface ListValue {
 *
 * 统一的异常处理
 * \@ControllerAdvice
 * 编写异常处理类 ， 使用ControllerAdvice注解
 * 使用ExceptionHandle标注可以处理的类型
 *
 *
 *
 *
 *
 * thymeleaf模板引擎
 * 配置cache为false
 * 静态资源都放在static下就能访问
 * 页面放在templates下 ， 直接访问 ， springboot在启动的时候默认寻找index.html
 * 页面更新不重启服务器 ，
 *  1. 引入devtools
 *  2. 修改完页面代码 ， ctrl+shift+F9 重新编译此页面，（如果是代码/配置 ， 还是重启服务）
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.sovava.product.feign")
public class GulimallProductApplication {

//    @NotNull
    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
