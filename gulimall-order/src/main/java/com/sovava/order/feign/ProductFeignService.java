package com.sovava.order.feign;

import com.sovava.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-product")
public interface ProductFeignService {
    @GetMapping("/product/spuinfo/{skuId}/info")
    R getSpuInfoBySkuId(@PathVariable("skuId") Long skuId);
}
