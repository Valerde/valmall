package com.sovava.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RabbitMQ使用：
 * 1. 引入amqp场景启动器，RabbitAutoConfiguration就会自动生效
 * 2. 给容器中自动配置了RabbitTemplate / amqpAdmin / rabbitMessagingTemplate / RabbitConnectionFactoryCreator
 * 3. 给配置文件添加spring.rabbitmq 的相关配置
 * 4. @EnableRabbit
 * 5. 监听消息 使用@RabbitListener
 *      1) RabbitListener 可以标在类上和方法上， 可以标注监听那个队列的消息
 *      2） RabbitHandler 标注在方法上时， 和RabbitListener配合使用可以处理同一个队列的不同类（相当于函数重载）
 *
 */
@SpringBootApplication
@EnableRabbit
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
