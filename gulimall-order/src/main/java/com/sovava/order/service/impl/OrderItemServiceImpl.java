package com.sovava.order.service.impl;

import com.rabbitmq.client.Channel;
import com.sovava.order.entity.OrderEntity;
import com.sovava.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.order.dao.OrderItemDao;
import com.sovava.order.entity.OrderItemEntity;
import com.sovava.order.service.OrderItemService;


@Service("orderItemService")
@Slf4j
@RabbitListener(queues = {"hello-java-queue"})
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * queues 声明需要监听的所有队列
     * 参数可以写以下类型：
     * 1. Message message 消息的详细信息：头+体  org.springframework.amqp.core.Message
     * 2. T<消息的原始类型>
     * 3. Channel channel 通道
     * <p>
     * Queue可以有很多人来监听，只要受到消息队列删除消息而且只能有一个接受消息
     * 只有当前消息处理完才会接受第二个消息
     *
     * @param message
     */
//    @RabbitListener(queues = {"hello-java-queue"})
    @RabbitHandler
    public void receiveMessage(Message message, OrderReturnReasonEntity orderReturnReason, Channel channel) throws InterruptedException {
//        log.debug("消息的内容是：{},类型是:{}", message, message.getClass());
        //消息头属性信息
        MessageProperties messageProperties = message.getMessageProperties();

//        log.debug("消息头属性:{}", messageProperties);
//        log.debug("消息体内容:{}", orderReturnReason.toString());

//        Thread.sleep(3000L);
//        log.debug("休眠后打印:{}",orderReturnReason.getName());

        // 通道内按顺序自增的
        long deliveryTag = messageProperties.getDeliveryTag();
        log.debug("deliveryTag:{}", deliveryTag);

        Long id = orderReturnReason.getId();
        //消息签收
        try {
            if (deliveryTag%2==1){
                log.debug("确认收货:{}",id);
                channel.basicAck(deliveryTag, false);
            }else{
                log.debug("拒绝:{}",id);
                channel.basicNack(deliveryTag,false,true);
            }

        } catch (IOException e) {
            log.debug("签收网络中断");
            e.printStackTrace();
        }
    }

    /**
     * @throws InterruptedException
     */
    @RabbitHandler
    public void receiveMessage2(OrderEntity order) throws InterruptedException {

//        log.debug("消息体内容:{}", order.toString());

    }

}