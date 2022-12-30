package com.sovava.product.feign;

import com.sovava.common.utils.R;
import com.sovava.product.fallback.SecKillServiceFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "gulimall-seckill",fallback = SecKillServiceFallBack.class)
public interface SeckillFeignService {
    @GetMapping("/sku/seckill/{skuId}")
    R getskuSeckillInfo(@PathVariable("skuId") Long skuId);
}
