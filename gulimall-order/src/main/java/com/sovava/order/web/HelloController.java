package com.sovava.order.web;

import com.sovava.order.entity.OrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.UUID;

@Controller
@Slf4j
public class HelloController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @ResponseBody
    @GetMapping("/test/createOrder")
    public String createOrderTest() {
        //订单下单成功
        OrderEntity order = new OrderEntity();
        order.setOrderSn(UUID.randomUUID().toString());
        order.setModifyTime(new Date());
        //给rabbitMQ发送消息
        rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order);
        log.debug("订单发送成功：{}",order.toString());
        return "ok";
    }

    @GetMapping("/{page}")
    public String listPage(@PathVariable("page") String page) {
        return page;
    }
}
