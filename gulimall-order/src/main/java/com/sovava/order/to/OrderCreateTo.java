package com.sovava.order.to;

import com.sovava.order.entity.OrderEntity;
import com.sovava.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateTo {
    private OrderEntity order;
    private List<OrderItemEntity> orderItems;
    /**
     * 应付价格
     */
    private BigDecimal payPrice;
    /**
     * 运费
     */
    private BigDecimal fare;
}
