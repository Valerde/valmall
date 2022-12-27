package com.sovava.deckill.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.sovava.common.utils.R;
import com.sovava.deckill.feign.CouponFeignService;
import com.sovava.deckill.feign.ProductFeignService;
import com.sovava.deckill.service.SeckillService;
import com.sovava.deckill.to.SeckKillSkuRedisTo;
import com.sovava.deckill.to.SeckillSessionTo;
import com.sovava.deckill.to.SeckillSkuRelationTo;
import com.sovava.deckill.to.SkuInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;
    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private final String SESSIONS_SKU_CHACH_PREFIX = "seckill:sessionsSku:";
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";//+商品随机码


    @Override
    public void uploadSeckillSkuLatest3Days() {

        //去数据库扫描需要参与秒杀的活动
        R lasted3DaysSessionR = couponFeignService.getLasted3DaysSession();
        if (lasted3DaysSessionR.getCode() == 0) {
            List<SeckillSessionTo> data = lasted3DaysSessionR.getData(new TypeReference<List<SeckillSessionTo>>() {
            });
            log.debug(data.toString());

            //缓存活动的信息
            data.forEach(this::saveSessionInfos);
            //缓存活动的关联商品信息
        }
    }

    private void saveSessionInfos(SeckillSessionTo session) {
        long startTime = session.getStartTime().getTime();
        long endTime = session.getEndTime().getTime();
        String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
        List<String> skuIds = session.getRelationSkus().stream().map(item -> item.getSkuId().toString()).collect(Collectors.toList());

        redisTemplate.opsForList().leftPushAll(key, skuIds);
        log.debug("上架的session信息为：{}--{}", key, skuIds.toString());
        for (SeckillSkuRelationTo relationSkus : session.getRelationSkus()) {
            saveRelationSku(relationSkus, startTime, endTime);
        }
    }

    /**
     * 缓存商品
     *
     * @param sku
     */
    private void saveRelationSku(SeckillSkuRelationTo sku, long startTime, long endTime) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SESSIONS_SKU_CHACH_PREFIX);
        SeckKillSkuRedisTo skuRedis = new SeckKillSkuRedisTo();
        //sku的秒杀信息
        BeanUtils.copyProperties(sku, skuRedis);
        //商品的基本信息
        R info = productFeignService.getSkuInfo(sku.getSkuId());
        if (info.getCode() == 0) {
            SkuInfoTo skuInfo = info.getData("skuInfo", new TypeReference<SkuInfoTo>() {
            });
            skuRedis.setSkuInfo(skuInfo);
        }
        //商品的时间信息
        skuRedis.setStartTime(startTime);
        skuRedis.setEndTime(endTime);
        //商品的随机码 为了防止攻击或脚本
        String randomCode = UUID.randomUUID().toString().replace("-", "");
        skuRedis.setRandomCode(randomCode);

        //引入分布式的信号量 , 为了限流
        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
        semaphore.trySetPermits(sku.getSeckillCount().intValue());
        //准备hash操作
        String jsonString = JSON.toJSONString(skuRedis);
        hashOps.put(sku.getSkuId().toString(), jsonString);

        log.debug("sku信息为：{}", jsonString);
    }
}
