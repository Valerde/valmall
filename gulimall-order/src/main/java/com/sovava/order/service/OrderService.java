package com.sovava.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.to.SecKillOrderTo;
import com.sovava.common.utils.PageUtils;
import com.sovava.order.entity.OrderEntity;
import com.sovava.order.vo.*;

import java.util.Map;

/**
 * 订单
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:47:29
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder();

    SubmitOrderRespVo submitOrder(OrderSubmitVo orderSubmitVo);

    OrderEntity getOrderByOrderSn(String orderSn);

    /**
     * 时间到达后关闭订单
     * @param order
     */
    void closeOrder(OrderEntity order);

    PayVo getpayVoByOrderSn(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handlePayResult(PayAsyncVo payAsyncVo);

    void createSecKillOrder(SecKillOrderTo secKillOrder);
}

