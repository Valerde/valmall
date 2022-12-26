package com.sovava.order;

import com.sovava.order.config.AlipayTemplate;
import com.sovava.order.entity.OrderEntity;
import com.sovava.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@SpringBootTest
@Slf4j
class GulimallOrderApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private AmqpAdmin amqpAdmin;

    /**
     * 1. 如何创建exchange queue binding
     * 1) 使用AmqpAdmin
     * 2. 如何收发消息
     */
    @Test
    public void testCreateExchange() {
        //String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false, null);
        amqpAdmin.declareExchange(directExchange);
        log.debug("exchange创建成功：{}", "hello-java-exchange");

    }

    /**
     * 创建队列
     */
    @Test
    public void testCreateQueue() {
        //String name, boolean durable, boolean exclusive, boolean autoDelete,
        //			@Nullable Map<String, Object> arguments
        Queue hello_java_queue = new Queue("hello-java-queue", true, false, false, null);

        amqpAdmin.declareQueue(hello_java_queue);
        log.debug("队列创建成功{}", hello_java_queue.getName());
    }

    /**
     * 创建绑定
     */
    @Test
    public void testBinding() {
        //String destination, DestinationType destinationType, String exchange, String routingKey,
        //			@Nullable Map<String, Object> arguments
        //将exchange指定的交换机和目的地进行绑定，使用routing key作为指定的类型匹配
        Binding binding = new Binding("hello-java-queue", Binding.DestinationType.QUEUE, "hello-java-exchange", "hello.java", null);
        amqpAdmin.declareBinding(binding);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void sendMessage() {
        OrderReturnReasonEntity orderReturnReasonEntity = new OrderReturnReasonEntity();
        orderReturnReasonEntity.setId(1L);
        orderReturnReasonEntity.setName("testRabbit测试");
        orderReturnReasonEntity.setCreateTime(new Date());
        orderReturnReasonEntity.setStatus(123);

        //1. 发送消息
        for (int i = 0; i < 10; i++) {
            orderReturnReasonEntity.setId((long) i);
            rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", orderReturnReasonEntity);
        }


        OrderEntity order = new OrderEntity();
        order.setOrderSn(UUID.randomUUID().toString());
        for (int i = 0; i < 10; i++) {
            order.setId((long) i);
            rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", order);
        }

        log.debug("消息发送成功，{}", orderReturnReasonEntity.toString());
    }


    @Autowired
    private AlipayTemplate alipayTemplate;
    @Test
    public void testAliPay(){
        System.out.println(alipayTemplate.getNotify_url());
    }

}
