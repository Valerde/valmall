package com.sovava.ware.listener;

import com.alibaba.fastjson2.TypeReference;
import com.rabbitmq.client.Channel;
import com.sovava.common.to.OrderTo;
import com.sovava.common.to.StockDetailTo;
import com.sovava.common.to.StockLockedTo;
import com.sovava.common.utils.R;
import com.sovava.ware.dao.WareSkuDao;
import com.sovava.ware.entity.WareOrderTaskDetailEntity;
import com.sovava.ware.entity.WareOrderTaskEntity;
import com.sovava.ware.feign.OrderFeignService;
import com.sovava.ware.service.WareOrderTaskDetailService;
import com.sovava.ware.service.WareOrderTaskService;
import com.sovava.ware.service.WareSkuService;
import com.sovava.ware.vo.OrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@Slf4j
@RabbitListener(queues = {"stock.release.stock.queue"})
public class StockReleaseListener {
    @Resource
    private WareSkuService wareSkuService;


    /**
     * 库存锁定成功 ， 但是订单出错误 ， 之前锁定的回滚<br>
     *
     * @param stockLockedTo
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo stockLockedTo, Message message, Channel channel) throws IOException {
        try {
            wareSkuService.unlockStock(stockLockedTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        }

    }

    @RabbitHandler
    public void handleStockLockedCancleRelease(String orderSn, Message message, Channel channel) throws IOException {
        log.debug("订单关闭准备解锁库存");

        try {
            wareSkuService.unlockStock(orderSn);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        }

    }

}
