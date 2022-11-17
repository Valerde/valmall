package com.sovava.order.controller;

import com.sovava.order.entity.OrderEntity;
import com.sovava.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
@Slf4j
public class RabbitController {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/sendMq")
    public String sendMessage(@RequestParam(value = "num", defaultValue = "1") Integer num) {
        sendMsg(num);
        return "ok";
    }

    public void sendMsg(int num) {
        OrderReturnReasonEntity orderReturnReasonEntity = new OrderReturnReasonEntity();
        orderReturnReasonEntity.setId(1L);
        orderReturnReasonEntity.setName("testRabbit测试");
        orderReturnReasonEntity.setCreateTime(new Date());
        orderReturnReasonEntity.setStatus(123);

        //1. 发送消息
        for (int i = 0; i < 10; i++) {
            orderReturnReasonEntity.setId((long) i);
            rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", orderReturnReasonEntity, new CorrelationData(UUID.randomUUID().toString()));
        }


        OrderEntity order = new OrderEntity();
        order.setOrderSn(UUID.randomUUID().toString());
        for (int i = 0; i < 10; i++) {
            order.setId((long) num);
//            rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", order);
        }

        log.debug("消息发送成功，{}", orderReturnReasonEntity.toString());
    }
}
