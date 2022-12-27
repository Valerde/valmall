package com.sovava.deckill.scheduled;

import com.sovava.deckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    /**
     * 定时上架：
     * TODO： 幂等性保证
     */
    @Scheduled(cron = "0 * * * * ?")
    public void uploadSeckillSkuLatest3Days() {

        //重复上架无需上架
        log.debug("上架秒杀的商品信息。。。");
        seckillService.uploadSeckillSkuLatest3Days();
    }
}
