package com.sovava.product.fallback;

import com.sovava.common.utils.R;
import com.sovava.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecKillServiceFallBack implements SeckillFeignService {
    @Override
    public R getskuSeckillInfo(Long skuId) {
        log.error("熔断方法调用");
        return R.error(123,"请求过大");
    }
}
