package com.sovava.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.utils.PageUtils;
import com.sovava.order.entity.OrderEntity;

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
}

