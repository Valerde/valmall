package com.sovava.deckill.service;

import com.sovava.deckill.to.SeckKillSkuRedisTo;

import java.util.List;

public interface SeckillService {
    void uploadSeckillSkuLatest3Days();

    List<SeckKillSkuRedisTo> getCurrentSeckillSkus();

    SeckKillSkuRedisTo getskuSeckillInfo(Long skuId);

    String kill(String killId, String key, Integer num);
}
