package com.sovava.order.config;

import com.rabbitmq.client.Channel;
import com.sovava.order.entity.OrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class MyMQConfig {


    /**
     * 容器中的组件自动创建
     * <br>
     * 一旦队列创建好，及时属性发生变化 ， 队列属性也不会更新 ， 必须删除后才能更新属性
     *
     * @return
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "order-event-exchange");
        arguments.put("x-dead-letter-routing-key", "order.release.order");
        arguments.put("x-message-ttl", 1 * 1000);
        Queue orderDelayQueue = new Queue("order.delay.queue", true, false, false, arguments);
        return orderDelayQueue;

    }

    @Bean
    public Queue orderReleaseOrderQueue() {
        Queue orderDelayQueue = new Queue("order.release.order.queue", true, false, false);
        return orderDelayQueue;
    }

    @Bean
    public Exchange orderEventExchange() {

        TopicExchange topicExchange = new TopicExchange("order-event-exchange", true, false);
        return topicExchange;
    }

    @Bean
    public Binding orderCreateBinding() {
        //String destination, DestinationType destinationType, String exchange, String routingKey,
        //			@Nullable Map<String, Object> arguments
        Binding binding = new Binding("order.delay.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.create.order", null);
        return binding;
    }

    @Bean
    public Binding orderReleaseBinding() {
        Binding binding = new Binding("order.release.order.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.release.order", null);
        return binding;
    }

    /**
     * 订单释放和库存释放直接进行绑定
     * @return
     */
    @Bean
    public Binding orderReleaseOtherBinding() {
        Binding binding = new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.release.other.#", null);
        return binding;
    }
}
