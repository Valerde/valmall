package com.sovava.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class MyRabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 使用json序列化机制 ， 进行消息转换
     * @return
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 发送端确认机制
     * 定制RabbitTemplate
     * 一 / confirm 服务器受到消息就回调 , producer 到 exchange 回调
     * 1. 设置publisher-confirm-type: correlated
     * 2. 设置确认回调
     *
     * 二 / 消息抵达队列的回调 exchange 到queue
     *     #开启exchange到queue的确认
     *     publisher-returns: true
     *     #只要抵达队列 ， 以异步发送优先回调return confirm
     *     template:
     *       mandatory: true
     *     设置确认回调setReturnCallback
     *
     *
     * 消费端确认机制 （保证每个消息都被正确消费 ， broker才会移除消息）
     *     #手动签收     listener:     simple:     acknowledge-mode: manual
     *      1. 默认是自动确认回复的，只要客户端受到消息，就会发送消息确认信息
     *          问题：
     *              受到很多消息，自动回复给服务器ack，而还没有处理完毕就宕机 , 消息就会宕机
     *          手动确认机制：
     *          只要没有确认ack,消息就一直是unacked模式,即使宕机 ， 消息也不会丢失
     *      2. 如何签收：
     *          签收 channel.basicAck(deliveryTag, false);
     *          拒签 channel.basicNack(deliveryTag,false,true);
     *
     *
     */
    @PostConstruct // 当前对象创建完成以后才执行这个方法
    public void initRabbitTemplate(){
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             *
             * @param correlationData correlation data for the callback. 当前消息的唯一关联数据（这个是消息的唯一id）
             * @param ack true for ack, false for nack  消息是否成功收到
             * @param cause An optional cause, for nack, when available, otherwise null. 失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
//                log.debug("confirm correlationData:{}",correlationData);
//                log.debug("消息是否成功收到：{}",ack);
//                log.debug("失败的原因：{}",cause);
            }
        });

        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 只要消息没有抵达队列 ， 就回调失败回调
             * @param message the returned message.  投递失败的消息
             * @param replyCode the reply code.    回复的状态码
             * @param replyText the reply text.     回复的文本内容
             * @param exchange the exchange.        将这个消息发送的交换机
             * @param routingKey the routing key.   失败消息的路由key
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                log.debug("失败的message：{}",message);
                log.debug("回复的状态码：{}",replyCode);
                log.debug("回复的文本内容：{}",replyText);
                log.debug("将这个消息发送的交换机：{}",exchange);
                log.debug("失败消息的路由key：{}",routingKey);
            }
        });
    }
}
