package com.sovava.order.listener;

import com.rabbitmq.client.Channel;
import com.sovava.common.to.SecKillOrderTo;
import com.sovava.order.entity.OrderEntity;
import com.sovava.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RabbitListener(queues = {"order.seckill.order.queue"})
public class OrdereSecKillListener {
    @Autowired
    private OrderService orderService;


    @RabbitHandler
    public void listener(SecKillOrderTo secKillOrder, Channel channel, Message message) throws IOException {

        log.debug("准备创建秒杀单的详细信息：{}", secKillOrder.toString());
        try {
            orderService.createSecKillOrder(secKillOrder);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }

    }
}
