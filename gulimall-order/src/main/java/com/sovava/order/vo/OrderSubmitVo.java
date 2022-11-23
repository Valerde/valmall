package com.sovava.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装提交订单的内容
 */
@Data
public class OrderSubmitVo {
    /**
     * 收货地址的Id
     */
    private Long addrId;
    /**
     * 支付方式
     */
    private Integer payType;
    /**
     * 无需提交要购买的商品，直接去获取一遍
     */
    private String orderToken;

    /**
     * 应付价格 ,验价
     */
    private BigDecimal payPrice;

    /**
     * 用户相关信息，直接去session中取出
     */

    /**
     * 备注
     */
    private String node;
}
