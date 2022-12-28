package com.sovava.common.to;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SecKillOrderTo {
    /**
     * 订单号
     */
    private String orderSn;
    /**
     * 商品ID
     */
    private Long skuId;
    /**
     * 活动的场次ID
     */
    private Long promotionSessionId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀数量
     */
    private Integer num;
    /**
     * 会员Id
     */
    private Long memberId;


}
