package com.sovava.deckill.scheduled;

import com.sovava.deckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品的定时上架
 * 每天三点定时上架最近三天需要上架的商品
 * <br>
 * 当天， 00：00：00--23：59:59
 * 明天， 00：00：00--23：59:59
 * 后天， 00：00：00--23：59:59
 */
@Service
@Slf4j
public class SeckillSkuScheduled {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private RedissonClient redissonClient;
    /**
     * 分布式锁
     */
    private final String upload_lock = "seckill:upload:lock";

    /**
     * 定时上架：
     * TODO： 幂等性保证
     */
    @Scheduled(cron = "0 * * * * ?")
    public void uploadSeckillSkuLatest3Days() {

        //重复上架无需上架
        log.debug("上架秒杀的商品信息。。。");
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        } finally {
            lock.unlock();

        }


    }
}
