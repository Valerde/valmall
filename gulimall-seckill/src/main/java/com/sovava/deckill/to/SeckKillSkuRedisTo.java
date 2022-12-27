package com.sovava.deckill.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SeckKillSkuRedisTo {


    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private BigDecimal seckillCount;
    /**
     * 每人限购数量
     */
    private BigDecimal seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;

    /**
     * 基本信息
     */
    private SkuInfoTo skuInfo;
    /**
     * 当前商品秒杀的开始时间
     */
    private Long startTime;
    /**
     * 当前商品保存的结束时间
     */
    private Long endTime;
    /**
     * 随机码
     */
    private String randomCode;
}
