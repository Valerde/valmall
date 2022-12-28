package com.sovava.deckill.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.sovava.common.to.SecKillOrderTo;
import com.sovava.common.utils.R;
import com.sovava.common.vo.MemberRespVo;
import com.sovava.deckill.feign.CouponFeignService;
import com.sovava.deckill.feign.ProductFeignService;
import com.sovava.deckill.interceptor.LoginUserInterceptor;
import com.sovava.deckill.service.SeckillService;
import com.sovava.deckill.to.SeckKillSkuRedisTo;
import com.sovava.deckill.to.SeckillSessionTo;
import com.sovava.deckill.to.SeckillSkuRelationTo;
import com.sovava.deckill.to.SkuInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

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

    @Override
    public List<SeckKillSkuRedisTo> getCurrentSeckillSkus() {
        //确定当前时间有哪个秒杀场次
        long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        log.debug(keys.toString());
        for (String key : keys) {
            //seckill:sessions:1672358400000_1672448400000
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            Long startTime = Long.parseLong(s[0]);
            Long endTime = Long.parseLong(s[1]);
            log.debug("时间区间为：{}-{}-{}", startTime, time, endTime);
            if (time >= startTime && time <= endTime) {
//              获取当前场次的所有商品信息
                List<String> sessionAndSkuId = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, Object> hashOps = redisTemplate.boundHashOps(SESSIONS_SKU_CHACH_PREFIX);
                List<Object> list = hashOps.multiGet(sessionAndSkuId);
                log.debug(list.toString());
                if (list != null && list.size() > 0) {
                    List<SeckKillSkuRedisTo> skuRedisTos = list.stream().map(item -> {
                        SeckKillSkuRedisTo seckKillSkuRedisTo = JSON.parseObject(item.toString(), SeckKillSkuRedisTo.class);

                        //当前秒杀已经开始了 ， 把随机码可以传过去

                        return seckKillSkuRedisTo;
                    }).collect(Collectors.toList());
                    return skuRedisTos;
                }


                break;
            }
        }
        return null;
    }

    @Override
    public SeckKillSkuRedisTo getskuSeckillInfo(Long skuId) {
        //找到所有需要参与秒杀的商品key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SESSIONS_SKU_CHACH_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                //1_9
                if (Pattern.matches(regx, key)) {
                    String jsonString = hashOps.get(key);
                    SeckKillSkuRedisTo seckKillSkuRedisTo = JSON.parseObject(jsonString, SeckKillSkuRedisTo.class);
                    //处理随机码
                    Long startTime = seckKillSkuRedisTo.getStartTime();
                    long time = new Date().getTime();
                    if (time > seckKillSkuRedisTo.getEndTime() || time < seckKillSkuRedisTo.getStartTime()) {
                        seckKillSkuRedisTo.setRandomCode(null);
                    }
                    return seckKillSkuRedisTo;
                }
            }

        }
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) {
        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();

        //获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SESSIONS_SKU_CHACH_PREFIX);
        String s = hashOps.get(killId);
        if (!StringUtils.isEmpty(s)) {
            SeckKillSkuRedisTo seckKillSkuRedisTo = JSON.parseObject(s, SeckKillSkuRedisTo.class);
            //校验合法性
            Long startTime = seckKillSkuRedisTo.getStartTime();
            Long endTime = seckKillSkuRedisTo.getEndTime();
            long time = new Date().getTime();
            long ttl = endTime - time;
            if (time >= startTime && time <= endTime) {
                //在秒杀时间内
                //校验随机码和商品ID是否正确
                String randomCode = seckKillSkuRedisTo.getRandomCode();
                String skuId = seckKillSkuRedisTo.getPromotionSessionId().toString() + "_" + seckKillSkuRedisTo.getSkuId().toString();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    //携带的数据是合法的
                    if (num <= seckKillSkuRedisTo.getSeckillLimit().intValue()) {
                        //数量是合法的

                        //验证这个人是否已经购买过
                        String redisKey = memberRespVo.getId().toString() + "_" + skuId;
                        //自动过期
                        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (ifAbsent) {
                            //占位成功 ， 说明没有买过这是第一次购买
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                            try {
                                boolean tryAcquire = semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS);
                                if (tryAcquire) {
                                    //拿到信号量
                                    //秒杀成功，快速下单 ， 发送消息到MQ TODO: 发送消息到MQ
                                    String timeId = IdWorker.getTimeId();
                                    SecKillOrderTo secKillOrderTo = new SecKillOrderTo();
                                    secKillOrderTo.setOrderSn(timeId);
                                    secKillOrderTo.setNum(num);
                                    secKillOrderTo.setSeckillPrice(seckKillSkuRedisTo.getSeckillPrice());
                                    secKillOrderTo.setMemberId(memberRespVo.getId());
                                    secKillOrderTo.setSkuId(seckKillSkuRedisTo.getSkuId());
                                    secKillOrderTo.setPromotionSessionId(seckKillSkuRedisTo.getPromotionSessionId());

                                    rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", secKillOrderTo);
                                    return timeId;
                                } else {
                                    return null;
                                }
                            } catch (InterruptedException e) {
                                log.error("发生错误：{}", e.toString());
                                e.printStackTrace();
                            }
                        } else {
                            //占位失败说明这个人已经买过不能再次购买了
                            return null;
                        }

                    }
                }

            } else {
                return null;
            }
        } else {
            return null;
        }
        return null;
    }

    private void saveSessionInfos(SeckillSessionTo session) {
        long startTime = session.getStartTime().getTime();
        long endTime = session.getEndTime().getTime();
        String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;

        List<String> skuIds = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId().toString() + "_" + item.getSkuId().toString()).collect(Collectors.toList());
        Boolean hasKey = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(hasKey)) {
            redisTemplate.opsForList().leftPushAll(key, skuIds);
        }

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
        String randomCode = UUID.randomUUID().toString().replace("-", "");
        Boolean hasKey1 = hashOps.hasKey(sku.getPromotionSessionId().toString() + "_" + sku.getSkuId().toString());
        //幂等性, 防止重复上架
        if (Boolean.FALSE.equals(hasKey1)) {

            SeckKillSkuRedisTo skuRedis = new SeckKillSkuRedisTo();
            //sku的秒杀信息
            BeanUtils.copyProperties(sku, skuRedis);
            //商品的基本信息
            R info = productFeignService.getSkuInfo(sku.getSkuId());
            log.debug("从远端获得的商品基本信息为：{}", info.toString());
            if (info.getCode() == 0) {
                SkuInfoTo skuInfo = info.getData("skuInfo", new TypeReference<SkuInfoTo>() {
                });
                skuRedis.setSkuInfo(skuInfo);
            }
            //商品的时间信息
            skuRedis.setStartTime(startTime);
            skuRedis.setEndTime(endTime);
            //商品的随机码 为了防止攻击或脚本

            skuRedis.setRandomCode(randomCode);


            //准备hash操作
            String jsonString = JSON.toJSONString(skuRedis);
            hashOps.put(sku.getPromotionSessionId().toString() + "_" + sku.getSkuId().toString(), jsonString);

            log.debug("sku信息为：{}", jsonString);

            //引入分布式的信号量 , 为了限流
            //如果当前场次 商品的库存信息 ， 已经上架就不需要上架了
            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
            semaphore.trySetPermits(sku.getSeckillCount().intValue());
        }

    }
}
