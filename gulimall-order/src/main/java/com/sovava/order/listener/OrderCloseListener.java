package com.sovava.order.listener;

import com.rabbitmq.client.Channel;
import com.sovava.order.entity.OrderEntity;
import com.sovava.order.feign.WareFeignService;
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
@RabbitListener(queues = {"order.release.order.queue"})
public class OrderCloseListener {
    @Autowired
    private OrderService orderService;

    @Autowired
    private WareFeignService wareFeignService;

    @RabbitHandler
    public void listener(OrderEntity order, Channel channel, Message message) throws IOException {
        log.debug("收到关闭的订单消息 ，准备关闭订单：{}", order.toString());
        try {
            orderService.closeOrder(order);
            //解锁库存

            //手动调用支付宝的收单功能

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);


        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }

    }

}
